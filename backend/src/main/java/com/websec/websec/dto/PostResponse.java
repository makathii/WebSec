package com.websec.websec.dto;

import java.time.Instant;

public record PostResponse(
		long id,
		String username,
		String imageUrl,
		String caption,
		long likeCount,
		long commentCount,
		boolean likedByMe,
		Instant createdAt) {
}
