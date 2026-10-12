package com.websec.websec.dto;

import com.websec.websec.validation.Patterns;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record CreatePostRequest(
		@NotNull MultipartFile image,
		@Size(max = 300) @Pattern(regexp = Patterns.TEXT) String caption) {

	public CreatePostRequest {
		caption = Patterns.normalizeText(caption);
		if (caption != null && caption.isEmpty()) {
			caption = null;
		}
	}
}
