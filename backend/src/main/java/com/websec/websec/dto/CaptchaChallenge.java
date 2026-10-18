package com.websec.websec.dto;

public record CaptchaChallenge(String algorithm, String challenge, long maxNumber, String salt, String signature) {
}
