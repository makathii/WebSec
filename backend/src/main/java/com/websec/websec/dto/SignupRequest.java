package com.websec.websec.dto;

import com.websec.websec.validation.Patterns;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record SignupRequest(
		@NotNull @Size(max = 254) @Pattern(regexp = Patterns.EMAIL) String email,
		@NotNull @Pattern(regexp = Patterns.USERNAME) String username,
		@NotNull @Size(max = 128) String password) {

	public SignupRequest {
		email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
		username = username == null ? null : username.strip().toLowerCase(Locale.ROOT);
		password = Patterns.normalizePassword(password);
	}
}
