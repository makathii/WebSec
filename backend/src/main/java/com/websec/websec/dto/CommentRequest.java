package com.websec.websec.dto;

import com.websec.websec.validation.Patterns;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CommentRequest(@NotNull @Size(min = 1, max = 500) @Pattern(regexp = Patterns.TEXT) String text) {

	public CommentRequest {
		text = Patterns.normalizeText(text);
	}
}
