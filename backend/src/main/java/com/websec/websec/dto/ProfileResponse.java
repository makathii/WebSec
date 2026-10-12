package com.websec.websec.dto;

import java.time.Instant;

public record ProfileResponse(String username, Instant createdAt, long postCount) {
}
