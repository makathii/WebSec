package com.websec.websec.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.websec.websec.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

// Cloudflare Turnstile: the widget in the browser hands out a token, the token is checked here with
// Cloudflare's siteverify API. Tokens are single use and expire after 5 minutes, Cloudflare enforces both.
@Service
public class CaptchaService {

	public static final String TEST_SECRET_KEY = "1x0000000000000000000000000000000AA";

	static final String SITEVERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

	@JsonIgnoreProperties(ignoreUnknown = true)
	record SiteverifyResponse(boolean success, @JsonProperty("error-codes") List<String> errorCodes) {
	}

	private final AppProperties properties;

	private final RestClient restClient;

	@Autowired
	public CaptchaService(AppProperties properties) {
		this(properties, RestClient.builder()
			.requestFactory(requestFactory())
			.build());
	}

	CaptchaService(AppProperties properties, RestClient restClient) {
		this.properties = properties;
		this.restClient = restClient;
		if (properties.turnstileSecretKey().equals(TEST_SECRET_KEY)) {
			log.warn("Turnstile test secret key in use, every captcha passes. Set TURNSTILE_SECRET_KEY for real checks.");
		}
	}

	public String siteKey() {
		return properties.turnstileSiteKey();
	}

	// fails closed: if Cloudflare can't be reached the captcha counts as not solved
	public boolean verify(String token, String ip) {
		if (token == null || token.isEmpty()) {
			return false;
		}
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("secret", properties.turnstileSecretKey());
		form.add("response", token);
		form.add("remoteip", ip);
		try {
			SiteverifyResponse response = restClient.post()
				.uri(SITEVERIFY_URL)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(SiteverifyResponse.class);
			if (response == null || !response.success()) {
				log.info("Captcha rejected by Turnstile: {}", response == null ? "empty response" : response.errorCodes());
				return false;
			}
			return true;
		}
		catch (RestClientException e) {
			log.error("Turnstile siteverify failed: {}", e.getMessage());
			return false;
		}
	}

	private static JdkClientHttpRequestFactory requestFactory() {
		HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
		factory.setReadTimeout(TIMEOUT);
		return factory;
	}
}
