package com.websec.websec.dto;

import com.websec.websec.validation.Patterns;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DeleteAccountRequest(@NotNull @Pattern(regexp = Patterns.ANY_PASSWORD) String password) {

	public DeleteAccountRequest {
		password = Patterns.normalizePassword(password);
	}
}
