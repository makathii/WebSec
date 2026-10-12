package com.websec.websec.validation;

import java.text.Normalizer;

public final class Patterns {

	public static final String USERNAME = "^(?!\\.)[a-z0-9_.]{3,20}(?<!\\.)$";

	public static final String EMAIL = "^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,185}\\.[A-Za-z]{2,24}$";

	public static final String LOGIN = "(" + USERNAME + ")|(" + EMAIL + ")";

	// printable ASCII only, the other rules are in PasswordPolicy
	public static final String PASSWORD = "^[\\x20-\\x7E]{12,128}$";

	// for logging in we only check the charset and length, the policy applies when a password is set
	public static final String ANY_PASSWORD = "^[\\x20-\\x7E]{1,128}$";

	// letters, marks, digits, punctuation, spaces, symbols like emoji/currency, + and =, and the
	// zero width joiner for emoji sequences. No control chars, no < >, no bidi overrides.
	public static final String TEXT = "^[\\p{L}\\p{M}\\p{N}\\p{P}\\p{Zs}\\p{So}\\p{Sc}\\p{Sk}+=\\x{200D}]*$";

	public static final String IMAGE_FILENAME =
			"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png)$";

	public static final String REFRESH_TOKEN = "^[A-Za-z0-9_-]{43}$";

	private Patterns() {
	}

	public static String normalizeText(String text) {
		if (text == null) {
			return null;
		}
		return Normalizer.normalize(text, Normalizer.Form.NFC).strip();
	}
}
