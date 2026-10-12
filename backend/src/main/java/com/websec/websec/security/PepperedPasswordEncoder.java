package com.websec.websec.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * BCrypt already uses a random salt per password. On top of that the password is HMACed with a
 * server-side pepper that is not stored in the database, so a leaked DB alone is not enough to
 * crack the hashes. The HMAC output is always 44 chars, which also avoids BCrypt's 72 byte limit.
 */
public class PepperedPasswordEncoder implements PasswordEncoder {

	private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);

	private final SecretKeySpec pepper;

	public PepperedPasswordEncoder(byte[] pepper) {
		this.pepper = new SecretKeySpec(pepper, "HmacSHA256");
	}

	@Override
	public String encode(CharSequence rawPassword) {
		return bcrypt.encode(hmac(rawPassword));
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		return bcrypt.matches(hmac(rawPassword), encodedPassword);
	}

	private String hmac(CharSequence rawPassword) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(pepper);
			byte[] digest = mac.doFinal(rawPassword.toString().getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(digest);
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}
}
