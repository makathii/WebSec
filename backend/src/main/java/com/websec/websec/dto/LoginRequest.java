package com.websec.websec.dto;

import com.websec.websec.validation.Patterns;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record LoginRequest(
		@NotNull @Size(max = 254) @Pattern(regexp = Patterns.LOGIN) String login,
		@NotNull @Pattern(regexp = Patterns.ANY_PASSWORD) String password,
		@Pattern(regexp = Patterns.CAPTCHA_TOKEN) String captcha) {

	public LoginRequest {
		login = login == null ? null : login.strip().toLowerCase(Locale.ROOT);
		password = Patterns.normalizePassword(password);
	}

	public boolean isEmail() {
		return login.contains("@");
	}
}
