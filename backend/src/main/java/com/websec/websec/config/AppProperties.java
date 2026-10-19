package com.websec.websec.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Base64;

@ConfigurationProperties("app")
public record AppProperties(
		String jwtSecret,
		String passwordPepper,
		String corsOrigin,
		boolean cookieSecure,
		String uploadDir,
		String turnstileSiteKey,
		String turnstileSecretKey) {

	public AppProperties {
		requireKey("app.jwt-secret", jwtSecret);
		requireKey("app.password-pepper", passwordPepper);
		if (turnstileSiteKey == null || turnstileSiteKey.isBlank() || turnstileSecretKey == null
				|| turnstileSecretKey.isBlank()) {
			throw new IllegalStateException("app.turnstile-site-key and app.turnstile-secret-key must be set");
		}
	}

	public byte[] jwtKey() {
		return Base64.getDecoder().decode(jwtSecret);
	}

	public byte[] pepperKey() {
		return Base64.getDecoder().decode(passwordPepper);
	}

	// refuse to start with a missing or weak secret
	private static void requireKey(String name, String value) {
		byte[] bytes;
		try {
			bytes = value == null ? new byte[0] : Base64.getDecoder().decode(value);
		}
		catch (IllegalArgumentException e) {
			throw new IllegalStateException(name + " must be base64");
		}
		if (bytes.length < 32) {
			throw new IllegalStateException(name + " must be at least 32 random bytes (base64)");
		}
	}
}
