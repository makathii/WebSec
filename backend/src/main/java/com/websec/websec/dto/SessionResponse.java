package com.websec.websec.dto;

public record SessionResponse(String accessToken, long expiresIn, UserResponse user) {
}
