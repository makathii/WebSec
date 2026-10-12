package com.websec.websec.security;

import com.websec.websec.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {

	private CurrentUser() {
	}

	public static Long idOrNull(Jwt jwt) {
		return jwt == null ? null : Long.valueOf(jwt.getSubject());
	}

	public static long id(Jwt jwt) {
		if (jwt == null) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Not logged in");
		}
		return Long.parseLong(jwt.getSubject());
	}
}
