package com.websec.websec.service;

import com.websec.websec.dto.ChangePasswordRequest;
import com.websec.websec.dto.ProfileResponse;
import com.websec.websec.dto.SignupRequest;
import com.websec.websec.dto.UserResponse;
import com.websec.websec.exception.ApiException;
import com.websec.websec.model.User;
import com.websec.websec.repository.PostRepository;
import com.websec.websec.repository.RefreshTokenRepository;
import com.websec.websec.repository.UserRepository;
import com.websec.websec.validation.PasswordPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

	private final UserRepository userRepository;

	private final PostRepository postRepository;

	private final RefreshTokenRepository refreshTokenRepository;

	private final PasswordEncoder passwordEncoder;

	private final PasswordPolicy passwordPolicy;

	private final RateLimitService rateLimitService;

	private final ImageStorageService imageStorage;

	public UserService(UserRepository userRepository, PostRepository postRepository,
			RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder,
			PasswordPolicy passwordPolicy, RateLimitService rateLimitService, ImageStorageService imageStorage) {
		this.userRepository = userRepository;
		this.postRepository = postRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.passwordPolicy = passwordPolicy;
		this.rateLimitService = rateLimitService;
		this.imageStorage = imageStorage;
	}

	@Transactional
	public UserResponse signup(SignupRequest request) {
		requireGoodPassword(request.password(), request.username(), request.email());
		if (userRepository.existsByUsername(request.username()) || userRepository.existsByEmail(request.email())) {
			throw new ApiException(HttpStatus.CONFLICT, "Username or email is already taken");
		}
		User user = new User(request.email(), request.username(), passwordEncoder.encode(request.password()));
		return UserResponse.from(userRepository.save(user));
	}

	@Transactional(readOnly = true)
	public ProfileResponse getProfile(String username) {
		User user = findByUsername(username);
		return new ProfileResponse(user.getUsername(), user.getCreatedAt(), postRepository.countByUser_Id(user.getId()));
	}

	@Transactional(readOnly = true)
	public User findByUsername(String username) {
		return userRepository.findByUsername(username)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
	}

	@Transactional
	public void changePassword(long userId, ChangePasswordRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
		checkPassword(user, request.currentPassword());
		if (request.newPassword().equals(request.currentPassword())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "The new password must be different");
		}
		requireGoodPassword(request.newPassword(), user.getUsername(), user.getEmail());

		user.changePassword(passwordEncoder.encode(request.newPassword()));
		// log out everywhere, someone else might know the old password
		refreshTokenRepository.revokeAllForUser(user.getId());
	}

	@Transactional
	public void deleteAccount(long userId, String password) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
		checkPassword(user, password);

		List<String> images = postRepository.findImagePathsByUserId(user.getId());
		// posts, comments, likes and refresh tokens are removed by ON DELETE CASCADE
		userRepository.delete(user);
		userRepository.flush();
		images.forEach(imageStorage::delete);
	}

	private void checkPassword(User user, String password) {
		String failKey = "user:" + user.getId();
		if (rateLimitService.tooManyFailedLogins(failKey)) {
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many wrong passwords, try again later");
		}
		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			rateLimitService.recordFailedLogin(failKey);
			// 403 and not 401, because the access token itself is fine
			throw new ApiException(HttpStatus.FORBIDDEN, "Wrong password");
		}
		rateLimitService.resetFailedLogins(failKey);
	}

	private void requireGoodPassword(String password, String username, String email) {
		String problem = passwordPolicy.check(password, username, email);
		if (problem != null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, problem);
		}
	}
}
