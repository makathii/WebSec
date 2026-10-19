package com.websec.websec.service;

import com.websec.websec.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CaptchaServiceTest {

	private MockRestServiceServer server;

	private CaptchaService service;

	@BeforeEach
	void setUp() {
		String key = Base64.getEncoder().encodeToString(new byte[32]);
		AppProperties properties = new AppProperties(key, key, "http://localhost:5173", true, "uploads", "site",
				"my-secret");
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		service = new CaptchaService(properties, builder.build());
	}

	@Test
	void tokenAcceptedByCloudflareIsValid() {
		server.expect(requestTo(CaptchaService.SITEVERIFY_URL))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content()
				.formDataContains(Map.of("secret", "my-secret", "response", "the-token", "remoteip", "10.0.0.1")))
			.andRespond(withSuccess("{\"success\":true,\"hostname\":\"localhost\",\"error-codes\":[]}",
					MediaType.APPLICATION_JSON));

		assertThat(service.verify("the-token", "10.0.0.1")).isTrue();
		server.verify();
	}

	@Test
	void tokenRejectedByCloudflareIsInvalid() {
		server.expect(requestTo(CaptchaService.SITEVERIFY_URL))
			.andRespond(withSuccess("{\"success\":false,\"error-codes\":[\"timeout-or-duplicate\"]}",
					MediaType.APPLICATION_JSON));

		assertThat(service.verify("used-token", "10.0.0.1")).isFalse();
	}

	@Test
	void cloudflareErrorCountsAsInvalid() {
		server.expect(requestTo(CaptchaService.SITEVERIFY_URL)).andRespond(withServerError());

		assertThat(service.verify("the-token", "10.0.0.1")).isFalse();
	}

	@Test
	void missingTokenIsInvalidWithoutAskingCloudflare() {
		assertThat(service.verify(null, "10.0.0.1")).isFalse();
		assertThat(service.verify("", "10.0.0.1")).isFalse();
		server.verify();
	}
}
