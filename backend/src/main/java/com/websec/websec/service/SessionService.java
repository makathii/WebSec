package com.websec.websec.service;

import com.websec.websec.dto.LoginRequest;
import com.websec.websec.exception.ApiException;
import com.websec.websec.model.RefreshToken;
import com.websec.websec.model.User;
import com.websec.websec.repository.RefreshTokenRepository;
import com.websec.websec.repository.UserRepository;
import com.websec.websec.validation.Patterns;
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

	public record Session(User user, String accessToken, String refreshToken) {
	}

	private final UserRepository userRepository;

	private final RefreshTokenRepository refreshTokenRepository;

	private final TokenService tokenService;

	private final PasswordEncoder passwordEncoder;

	private final RateLimitService rateLimitService;

	private final String dummyHash;

	public SessionService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
			TokenService tokenService, PasswordEncoder passwordEncoder, RateLimitService rateLimitService) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.tokenService = tokenService;
		this.passwordEncoder = passwordEncoder;
		this.rateLimitService = rateLimitService;
		this.dummyHash = passwordEncoder.encode("not-a-real-password");
	}

	@Transactional
	public Session login(LoginRequest request) {
		Optional<User> user = request.isEmail()
				? userRepository.findByEmail(request.login())
				: userRepository.findByUsername(request.login());

		String failKey = user.map(u -> "user:" + u.getId()).orElse("login:" + request.login());
		if (rateLimitService.tooManyFailedLogins(failKey)) {
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed logins, try again later");
		}

		// always run BCrypt, even for unknown users, so the response time doesn't reveal if a user exists
		String hash = user.map(User::getPasswordHash).orElse(dummyHash);
		boolean passwordOk = passwordEncoder.matches(request.password(), hash);
		if (user.isEmpty() || !passwordOk) {
			rateLimitService.recordFailedLogin(failKey);
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid login or password");
		}

		rateLimitService.resetFailedLogins(failKey);
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
