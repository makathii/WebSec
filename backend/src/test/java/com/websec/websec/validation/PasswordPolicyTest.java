package com.websec.websec.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

	private final PasswordPolicy policy = new PasswordPolicy();

	@Test
	void unicodePasswordIsAccepted() {
		assertThat(policy.check("Grüße-aus-Köln-2026", "alice", "alice@example.com")).isNull();
	}

	@Test
	void asciiPasswordIsStillAccepted() {
		assertThat(policy.check("Corr3ct-Horse-Battery", "alice", "alice@example.com")).isNull();
	}

	@Test
	void longPassphraseWithoutMixedCharacterTypesIsAccepted() {
		assertThat(policy.check("meine katze heisst minka", "alice", "alice@example.com")).isNull();
	}

	@Test
	void repeatedCharactersAreAccepted() {
		assertThat(policy.check("aaaa-meine-katze-minka", "alice", "alice@example.com")).isNull();
	}

	@Test
	void commonPasswordIsStillRefused() {
		assertThat(policy.check("Password123!", "alice", "alice@example.com")).isEqualTo("This password is too common");
	}

	@Test
	void controlCharacterIsRefused() {
		assertThat(policy.check("Corr3ct-Horse\u0007Battery", "alice", "alice@example.com")).isNotNull();
	}

	@Test
	void differentlyTypedAccentsBecomeTheSamePassword() {
		String composed = "Café-Corr3ct-Horse";
		String decomposed = "Café-Corr3ct-Horse";

		assertThat(Patterns.normalizePassword(decomposed)).isEqualTo(Patterns.normalizePassword(composed));
	}
}
