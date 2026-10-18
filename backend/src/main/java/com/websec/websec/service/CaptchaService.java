package com.websec.websec.service;

import com.websec.websec.dto.CaptchaChallenge;
import com.websec.websec.dto.CaptchaSolution;
import com.websec.websec.validation.Patterns;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaptchaService {

	public static final int MAX_NUMBER = 50_000;

	private static final Duration TTL = Duration.ofMinutes(5);

	private final SecureRandom random = new SecureRandom();

	private final SecretKeySpec key;

	private final Map<String, Long> used = new ConcurrentHashMap<>();

	public CaptchaService() {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		this.key = new SecretKeySpec(bytes, "HmacSHA256");
	}

	public CaptchaChallenge createChallenge() {
		byte[] saltBytes = new byte[12];
		random.nextBytes(saltBytes);
		String salt = HexFormat.of().formatHex(saltBytes) + "." + Instant.now().plus(TTL).getEpochSecond();
		String challenge = sha256(salt + random.nextInt(MAX_NUMBER + 1));
		return new CaptchaChallenge("SHA-256", challenge, MAX_NUMBER, salt, hmac(challenge));
	}

	public boolean verify(CaptchaSolution solution) {
		if (solution == null || solution.challenge() == null || solution.signature() == null
				|| solution.salt() == null || !solution.salt().matches(Patterns.CAPTCHA_SALT)) {
			return false;
		}
		long expires = Long.parseLong(solution.salt().substring(solution.salt().indexOf('.') + 1));
		if (Instant.now().getEpochSecond() > expires) {
			return false;
		}
		if (!equal(hmac(solution.challenge()), solution.signature())
				|| !equal(sha256(solution.salt() + solution.number()), solution.challenge())) {
			return false;
		}
		return used.putIfAbsent(solution.challenge(), expires) == null;
	}

	@Scheduled(fixedRate = 5 * 60 * 1000)
	public void removeExpired() {
		long now = Instant.now().getEpochSecond();
		used.values().removeIf(expires -> expires < now);
	}

	private String hmac(String value) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(key);
			return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String sha256(String value) {
		try {
			return HexFormat.of()
				.formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}

	private static boolean equal(String a, String b) {
		return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
	}
}
