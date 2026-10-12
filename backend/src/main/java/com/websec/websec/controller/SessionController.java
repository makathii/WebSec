package com.websec.websec.controller;

import com.websec.websec.config.AppProperties;
import com.websec.websec.dto.LoginRequest;
import com.websec.websec.dto.SessionResponse;
import com.websec.websec.dto.UserResponse;
import com.websec.websec.exception.ApiException;
import com.websec.websec.security.RefreshCookie;
import com.websec.websec.service.SessionService;
import com.websec.websec.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
public class SessionController {

	private final SessionService sessionService;

	private final AppProperties properties;

	private final RefreshCookie refreshCookie;

	public SessionController(SessionService sessionService, AppProperties properties, RefreshCookie refreshCookie) {
		this.sessionService = sessionService;
		this.properties = properties;
		this.refreshCookie = refreshCookie;
	}

	@PostMapping
	public ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request,
			@RequestHeader(name = HttpHeaders.ORIGIN, required = false) String origin) {
		checkOrigin(origin);
		return created(sessionService.login(request));
	}

	@PostMapping("/tokens")
	public ResponseEntity<SessionResponse> refresh(@CookieValue(name = RefreshCookie.NAME, required = false) String token,
			@RequestHeader(name = HttpHeaders.ORIGIN, required = false) String origin) {
		checkOrigin(origin);
		if (token == null) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Not logged in");
		}
		return created(sessionService.refresh(token));
	}

	@DeleteMapping
	public ResponseEntity<Void> logout(@CookieValue(name = RefreshCookie.NAME, required = false) String token,
			@RequestHeader(name = HttpHeaders.ORIGIN, required = false) String origin) {
		checkOrigin(origin);
		if (token != null) {
			sessionService.logout(token);
		}
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, refreshCookie.clear()).build();
	}

	// The cookie is SameSite=Strict already, this is a second check against CSRF on the cookie endpoints.
	// Browsers always send Origin on cross-origin and on POST/DELETE requests.
	private void checkOrigin(String origin) {
		if (origin == null || !origin.equals(properties.corsOrigin())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "Invalid origin");
		}
	}

	private ResponseEntity<SessionResponse> created(SessionService.Session session) {
		SessionResponse body = new SessionResponse(session.accessToken(),
				TokenService.ACCESS_TOKEN_TTL.toSeconds(), UserResponse.from(session.user()));
		return ResponseEntity.status(HttpStatus.CREATED)
			.header(HttpHeaders.LOCATION, "/api/session")
			.header(HttpHeaders.SET_COOKIE, refreshCookie.create(session.refreshToken()))
			.body(body);
	}
}
