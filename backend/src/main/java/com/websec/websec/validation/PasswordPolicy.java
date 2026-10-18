package com.websec.websec.validation;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Rules for new passwords (length and charset are already checked by Patterns.PASSWORD):
 * no username or email in it, and not one of the 10k most common passwords
 * (also catches simple variations like "Password123!").
 */
@Component
public class PasswordPolicy {

	private final Set<String> commonPasswords = new HashSet<>();

	public PasswordPolicy() {
		ClassPathResource resource = new ClassPathResource("common-passwords.txt");
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
			reader.lines().forEach(commonPasswords::add);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// returns null if the password is ok, otherwise a message for the user
	public String check(String password, String username, String email) {
		if (password == null || !password.matches(Patterns.PASSWORD)) {
			return "Password must be 12 to 128 characters (letters, digits, spaces and symbols)";
		}

		String lower = password.toLowerCase(Locale.ROOT);
		if (username != null && lower.contains(username.toLowerCase(Locale.ROOT))) {
			return "Password must not contain your username";
		}
		if (email != null) {
			String localPart = email.toLowerCase(Locale.ROOT).split("@")[0];
			if (localPart.length() >= 3 && lower.contains(localPart)) {
				return "Password must not contain your email address";
			}
		}

		String lettersOnly = lower.replaceAll("[^a-z]", "");
		String withoutEnding = lower.replaceAll("[^a-z]+$", "");
		if (commonPasswords.contains(lower) || commonPasswords.contains(lettersOnly)
				|| commonPasswords.contains(withoutEnding)) {
			return "This password is too common";
		}
		return null;
	}
}
