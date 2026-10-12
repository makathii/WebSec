package com.websec.websec.dto;

import com.websec.websec.model.Comment;

import java.time.Instant;

public record CommentResponse(long id, String username, String text, Instant createdAt) {

	public static CommentResponse from(Comment comment) {
		return new CommentResponse(comment.getId(), comment.getUser().getUsername(), comment.getText(),
				comment.getCreatedAt());
	}
}
