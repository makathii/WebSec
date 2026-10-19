package com.websec.websec.service;

import com.websec.websec.dto.LoginRequest;
import com.websec.websec.exception.ApiException;
import com.websec.websec.model.RefreshToken;
import com.websec.websec.model.User;
import com.websec.websec.repository.RefreshTokenRepository;
import com.websec.websec.repository.UserRepository;
import com.websec.websec.validation.Patterns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class SessionService {

	public static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

	private static final Logger log = LoggerFactory.getLogger(SessionService.class);

	public record Session(User user, String accessToken, String refreshToken) {
	}

	private final UserRepository userRepository;

	private final RefreshTokenRepository refreshTokenRepository;

	private final TokenService tokenService;

	private final PasswordEncoder passwordEncoder;

	private final RateLimitService rateLimitService;

	private final CaptchaService captchaService;

	private final String dummyHash;

	public SessionService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
			TokenService tokenService, PasswordEncoder passwordEncoder, RateLimitService rateLimitService,
			CaptchaService captchaService) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.tokenService = tokenService;
		this.passwordEncoder = passwordEncoder;
		this.rateLimitService = rateLimitService;
		this.captchaService = captchaService;
		this.dummyHash = passwordEncoder.encode("not-a-real-password");
	}

	@Transactional
	public Session login(LoginRequest request, String ip) {
		Optional<User> user = request.isEmail()
				? userRepository.findByEmail(request.login())
				: userRepository.findByUsername(request.login());
		Long userId = user.map(User::getId).orElse(null);

		String failKey = user.map(u -> "user:" + u.getId()).orElse("login:" + request.login());
		if (rateLimitService.tooManyFailedLogins(failKey, ip)) {
			log.warn("Login blocked after too many failed attempts: user id {}, IP {}", userId, ip);
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed logins, try again later");
		}
		if (rateLimitService.captchaRequired(failKey, ip) && !captchaService.verify(request.captcha(), ip)) {
			log.warn("Login without valid captcha: user id {}, IP {}", userId, ip);
			throw new ApiException(HttpStatus.PRECONDITION_REQUIRED, "Captcha required");
		}

		// always run BCrypt, even for unknown users, so the response time doesn't reveal if a user exists
		String hash = user.map(User::getPasswordHash).orElse(dummyHash);
		boolean passwordOk = passwordEncoder.matches(request.password(), hash);
		if (user.isEmpty() || !passwordOk) {
			rateLimitService.recordFailedLogin(failKey, ip);
			log.warn("Failed login: user id {}, IP {}", userId, ip);
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid login or password");
		}

		rateLimitService.resetFailedLogins(failKey, ip);
		return startSession(user.get());
	}

	// noRollbackFor: when a reused token revokes all sessions, that revoke has to be committed
	@Transactional(noRollbackFor = ApiException.class)
	public Session refresh(String rawToken) {
		RefreshToken token = findToken(rawToken)
			.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session expired"));

		if (token.isRevoked()) {
			// a token that was already used shows up again, so it was probably stolen
			refreshTokenRepository.revokeAllForUser(token.getUser().getId());
			log.warn("Reused refresh token, all sessions revoked: user id {}", token.getUser().getId());
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Session expired");
		}
		if (token.isExpired()) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Session expired");
		}

		token.revoke();
		return startSession(token.getUser());
	}

	@Transactional
	public void logout(String rawToken) {
		findToken(rawToken).ifPresent(RefreshToken::revoke);
	}

	@Scheduled(cron = "0 0 3 * * *")
	@Transactional
	public void deleteExpiredTokens() {
		refreshTokenRepository.deleteExpired(Instant.now());
	}

	private Optional<RefreshToken> findToken(String rawToken) {
		if (rawToken == null || !rawToken.matches(Patterns.REFRESH_TOKEN)) {
			return Optional.empty();
		}
		return refreshTokenRepository.findByTokenHash(tokenService.hash(rawToken));
	}

	private Session startSession(User user) {
		String refreshToken = tokenService.createRefreshToken();
		refreshTokenRepository.save(
				new RefreshToken(user, tokenService.hash(refreshToken), Instant.now().plus(REFRESH_TOKEN_TTL)));
		return new Session(user, tokenService.createAccessToken(user), refreshToken);
	}
}
