package com.websec.websec.dto;

import com.websec.websec.model.User;

import java.time.Instant;

public record UserResponse(String username, Instant createdAt) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getUsername(), user.getCreatedAt());
	}
}
