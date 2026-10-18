package com.websec.websec.security;

import com.websec.websec.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

	private final RateLimitFilter filter = new RateLimitFilter(new RateLimitService());

	@Test
	void encodedLoginPathIsRateLimited() throws Exception {
		for (int i = 0; i < 10; i++) {
			assertThat(login("/api/%73ession")).isEqualTo(200);
		}
		assertThat(login("/api/%73ession")).isEqualTo(429);
	}

	@Test
	void encodedAndPlainPathShareOneLimit() throws Exception {
		for (int i = 0; i < 5; i++) {
			assertThat(login("/api/session")).isEqualTo(200);
			assertThat(login("/api/%73ession")).isEqualTo(200);
		}
		assertThat(login("/api/session")).isEqualTo(429);
	}

	private int login(String requestUri) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", requestUri);
		request.setServletPath("/api/session");
		request.setRemoteAddr("10.0.0.1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		return response.getStatus();
	}
}
