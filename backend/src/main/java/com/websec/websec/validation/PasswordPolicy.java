package com.websec.websec.validation;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Rules for new passwords (length and charset are already checked by Patterns.PASSWORD):
 * no username, email or site name in it, no repeated pattern or sequence like "aaaa", "abcabc"
 * or "1234abcd", and not one of the 10k most common passwords
 * (also catches simple variations like "Password123!").
 */
@Component
public class PasswordPolicy {

	private static final List<String> SITE_NAMES = List.of("websecgram", "websec");

	private static final List<String> SEQUENCES = sequences("abcdefghijklmnopqrstuvwxyz", "01234567890123456789",
			"qwertzuiopü", "asdfghjklöä", "yxcvbnm", "qwertyuiop", "asdfghjkl", "zxcvbnm");

	private static final int MIN_DISTINCT_CHARACTERS = 5;

	private static final int MIN_SEQUENCE_LENGTH = 3;

	private static final int MAX_CHARACTERS_OUTSIDE_SEQUENCES = 2;

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
			return "Password must be 15 to 128 characters (letters, digits, spaces and symbols)";
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

		for (String siteName : SITE_NAMES) {
			if (lower.contains(siteName)) {
				return "Password must not contain the name of this site";
			}
		}
		if (isRepetitive(lower)) {
			return "Password must not be a repeated character or pattern";
		}
		if (isSequence(lower)) {
			return "Password must not be a sequence like 1234, abcd or qwertz";
		}

		String lettersOnly = lower.replaceAll("[^a-z]", "");
		String withoutEnding = lower.replaceAll("[^a-z]+$", "");
		if (commonPasswords.contains(lower) || commonPasswords.contains(lettersOnly)
				|| commonPasswords.contains(withoutEnding)) {
			return "This password is too common";
		}
		return null;
	}

	private static boolean isRepetitive(String password) {
		return password.matches("(.+?)\\1+") || password.codePoints().distinct().count() < MIN_DISTINCT_CHARACTERS;
	}

	private static boolean isSequence(String password) {
		String compact = password.replaceAll("\\s", "");
		int uncovered = 0;
		int i = 0;
		while (i < compact.length()) {
			int run = longestSequenceAt(compact, i);
			if (run >= MIN_SEQUENCE_LENGTH) {
				i += run;
			}
			else {
				uncovered++;
				i++;
			}
		}
		return uncovered <= MAX_CHARACTERS_OUTSIDE_SEQUENCES;
	}

	private static int longestSequenceAt(String text, int start) {
		int longest = 0;
		for (String sequence : SEQUENCES) {
			for (int pos = sequence.indexOf(text.charAt(start)); pos >= 0;
					pos = sequence.indexOf(text.charAt(start), pos + 1)) {
				int length = 0;
				while (start + length < text.length() && pos + length < sequence.length()
						&& sequence.charAt(pos + length) == text.charAt(start + length)) {
					length++;
				}
				longest = Math.max(longest, length);
			}
		}
		return longest;
	}

	private static List<String> sequences(String... forward) {
		List<String> all = new ArrayList<>();
		for (String sequence : forward) {
			all.add(sequence);
			all.add(new StringBuilder(sequence).reverse().toString());
		}
		return List.copyOf(all);
	}
}
