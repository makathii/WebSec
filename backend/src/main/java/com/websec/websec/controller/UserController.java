package com.websec.websec.controller;

import com.websec.websec.dto.ChangePasswordRequest;
import com.websec.websec.dto.DeleteAccountRequest;
import com.websec.websec.dto.PageResponse;
import com.websec.websec.dto.PostResponse;
import com.websec.websec.dto.ProfileResponse;
import com.websec.websec.dto.SignupRequest;
import com.websec.websec.dto.UserResponse;
import com.websec.websec.security.CurrentUser;
import com.websec.websec.security.RefreshCookie;
import com.websec.websec.service.PostService;
import com.websec.websec.service.UserService;
import com.websec.websec.validation.Patterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	private final PostService postService;

	private final RefreshCookie refreshCookie;

	public UserController(UserService userService, PostService postService, RefreshCookie refreshCookie) {
		this.userService = userService;
		this.postService = postService;
		this.refreshCookie = refreshCookie;
	}

	@PostMapping
	public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
		UserResponse user = userService.signup(request);
		return ResponseEntity.created(URI.create("/api/users/" + user.username())).body(user);
	}

	@GetMapping("/{username}")
	public ProfileResponse get(@PathVariable @Pattern(regexp = Patterns.USERNAME) String username) {
		return userService.getProfile(username);
	}

	@GetMapping("/{username}/posts")
	public PageResponse<PostResponse> posts(@PathVariable @Pattern(regexp = Patterns.USERNAME) String username,
			@RequestParam(defaultValue = "0") @Min(0) @Max(1000) int page, @AuthenticationPrincipal Jwt jwt) {
		return postService.byUser(username, page, CurrentUser.idOrNull(jwt));
	}

	// "me" can't clash with a real user, usernames have at least 3 characters
	@PutMapping("/me/password")
	public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		userService.changePassword(CurrentUser.id(jwt), request);
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, refreshCookie.clear()).build();
	}

	@DeleteMapping("/me")
	public ResponseEntity<Void> deleteAccount(@Valid @RequestBody DeleteAccountRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		userService.deleteAccount(CurrentUser.id(jwt), request.password());
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, refreshCookie.clear()).build();
	}
}
