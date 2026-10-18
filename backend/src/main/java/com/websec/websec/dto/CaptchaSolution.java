package com.websec.websec.dto;

import com.websec.websec.service.CaptchaService;
import com.websec.websec.validation.Patterns;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CaptchaSolution(
		@NotNull @Pattern(regexp = Patterns.SHA256_HEX) String challenge,
		@NotNull @Pattern(regexp = Patterns.CAPTCHA_SALT) String salt,
		@Min(0) @Max(CaptchaService.MAX_NUMBER) long number,
		@NotNull @Pattern(regexp = Patterns.SHA256_HEX) String signature) {
}
