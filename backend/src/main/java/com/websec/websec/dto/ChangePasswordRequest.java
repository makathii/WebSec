package com.websec.websec.dto;

import com.websec.websec.validation.Patterns;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
		@NotNull @Pattern(regexp = Patterns.ANY_PASSWORD) String currentPassword,
		@NotNull @Pattern(regexp = Patterns.ANY_PASSWORD) String newPassword) {
}
