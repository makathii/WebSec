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
 * at least 3 of lowercase/uppercase/digit/symbol, not more than 3 equal characters in a row,
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

		int classes = 0;
		if (password.matches(".*[a-z].*")) {
			classes++;
		}
		if (password.matches(".*[A-Z].*")) {
			classes++;
		}
		if (password.matches(".*[0-9].*")) {
			classes++;
		}
		if (password.matches(".*[^a-zA-Z0-9].*")) {
			classes++;
		}
		if (classes < 3) {
			return "Password needs at least 3 of: lowercase letter, uppercase letter, digit, symbol";
		}

		if (password.matches(".*(.)\\1{3}.*")) {
			return "Password must not repeat the same character more than 3 times in a row";
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
