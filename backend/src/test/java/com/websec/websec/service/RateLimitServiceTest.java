package com.websec.websec.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitServiceTest {

	private final RateLimitService service = new RateLimitService();

	@Test
	void attackerBlocksOnlyHimself() {
		fail("user:1", "10.0.0.66", 10);

		assertThat(service.tooManyFailedLogins("user:1", "10.0.0.66")).isTrue();
		assertThat(service.tooManyFailedLogins("user:1", "10.0.0.1")).isFalse();
	}

	@Test
	void nineFailuresDoNotBlock() {
		fail("user:1", "10.0.0.66", 9);

		assertThat(service.tooManyFailedLogins("user:1", "10.0.0.66")).isFalse();
	}

	@Test
	void manyIpsTogetherBlockTheAccount() {
		for (int ip = 0; ip < 100; ip++) {
			fail("user:1", "10.0.1." + ip, 1);
		}

		assertThat(service.tooManyFailedLogins("user:1", "10.0.2.1")).isTrue();
		assertThat(service.tooManyFailedLogins("user:2", "10.0.2.1")).isFalse();
	}

	@Test
	void successResetsOnlyTheOwnIp() {
		fail("user:1", "10.0.0.1", 9);
		fail("user:1", "10.0.0.66", 10);

		service.resetFailedLogins("user:1", "10.0.0.1");

		assertThat(service.tooManyFailedLogins("user:1", "10.0.0.1")).isFalse();
		assertThat(service.tooManyFailedLogins("user:1", "10.0.0.66")).isTrue();
	}

	private void fail(String account, String ip, int times) {
		for (int i = 0; i < times; i++) {
			service.recordFailedLogin(account, ip);
		}
	}
}
