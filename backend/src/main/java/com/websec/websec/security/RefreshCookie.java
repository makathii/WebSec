package com.websec.websec.security;

import com.websec.websec.config.AppProperties;
import com.websec.websec.service.SessionService;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshCookie {

	public static final String NAME = "refresh_token";

	private final AppProperties properties;

	public RefreshCookie(AppProperties properties) {
		this.properties = properties;
	}

	public String create(String token) {
		return build(token, SessionService.REFRESH_TOKEN_TTL);
	}

	public String clear() {
		return build("", Duration.ZERO);
	}

	private String build(String value, Duration maxAge) {
		return ResponseCookie.from(NAME, value)
			.httpOnly(true)
			.secure(properties.cookieSecure())
			.sameSite("Strict")
			.path("/api/session")
			.maxAge(maxAge)
			.build()
			.toString();
	}
}
