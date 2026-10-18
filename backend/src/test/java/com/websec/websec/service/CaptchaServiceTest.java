package com.websec.websec.service;

import com.websec.websec.dto.CaptchaChallenge;
import com.websec.websec.dto.CaptchaSolution;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaServiceTest {

	private final CaptchaService service = new CaptchaService();

	@Test
	void solvedChallengeIsAccepted() throws Exception {
		CaptchaChallenge challenge = service.createChallenge();

		assertThat(service.verify(solve(challenge))).isTrue();
	}

	@Test
	void solutionCanBeUsedOnlyOnce() throws Exception {
		CaptchaSolution solution = solve(service.createChallenge());

		assertThat(service.verify(solution)).isTrue();
		assertThat(service.verify(solution)).isFalse();
	}

	@Test
	void wrongNumberIsRejected() throws Exception {
		CaptchaSolution solution = solve(service.createChallenge());
		long wrong = solution.number() == 0 ? 1 : solution.number() - 1;

		assertThat(service.verify(new CaptchaSolution(solution.challenge(), solution.salt(), wrong, solution.signature())))
			.isFalse();
	}

	@Test
	void challengeNotIssuedByTheServerIsRejected() throws Exception {
		String salt = "0123456789abcdef01234567." + Instant.now().plusSeconds(60).getEpochSecond();
		String challenge = sha256(salt + 7);

		assertThat(service.verify(new CaptchaSolution(challenge, salt, 7, sha256("fake")))).isFalse();
	}

	@Test
	void challengeFromAnotherServerIsRejected() throws Exception {
		CaptchaSolution solution = solve(new CaptchaService().createChallenge());

		assertThat(service.verify(solution)).isFalse();
	}

	@Test
	void missingSolutionIsRejected() {
		assertThat(service.verify(null)).isFalse();
	}

	private static CaptchaSolution solve(CaptchaChallenge challenge) throws Exception {
		for (long n = 0; n <= challenge.maxNumber(); n++) {
			if (sha256(challenge.salt() + n).equals(challenge.challenge())) {
				return new CaptchaSolution(challenge.challenge(), challenge.salt(), n, challenge.signature());
			}
		}
		throw new IllegalStateException("not solvable");
	}

	private static String sha256(String value) throws Exception {
		return HexFormat.of()
			.formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
	}
}
