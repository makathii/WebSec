package com.websec.websec.security;

import com.websec.websec.config.AppProperties;
import com.websec.websec.model.User;
import com.websec.websec.repository.UserRepository;
import com.websec.websec.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtValidationTest {

	private static final byte[] KEY = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.US_ASCII);

	private final UserRepository userRepository = mock(UserRepository.class);

	private JwtDecoder decoder;

	private TokenService tokenService;

	@BeforeEach
	void setUp() {
		String key = Base64.getEncoder().encodeToString(KEY);
		AppProperties properties = new AppProperties(key, key, "http://localhost:5173", true, "uploads", "site", "secret");
		SecurityConfig config = new SecurityConfig();
		decoder = config.jwtDecoder(properties, userRepository);
		tokenService = new TokenService(config.jwtEncoder(properties));
	}

	@Test
	void validTokenIsAccepted() {
		when(userRepository.findTokenVersionById(1L)).thenReturn(Optional.of(0));

		assertThat(decoder.decode(tokenFor(1L, 0)).getSubject()).isEqualTo("1");
	}

	@Test
	void tokenIsRejectedAfterPasswordChange() {
		String token = tokenFor(1L, 0);
		when(userRepository.findTokenVersionById(1L)).thenReturn(Optional.of(1));

		assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void tokenIsRejectedAfterAccountDeletion() {
		String token = tokenFor(1L, 0);
		when(userRepository.findTokenVersionById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void tokenWithoutVersionIsRejected() {
		when(userRepository.findTokenVersionById(1L)).thenReturn(Optional.of(0));

		assertThatThrownBy(() -> decoder.decode(sign(KEY, payload(1L, null)))).isInstanceOf(JwtException.class);
	}

	@Test
	void unsignedTokenIsRejected() {
		when(userRepository.findTokenVersionById(1L)).thenReturn(Optional.of(0));
		String header = encode("{\"alg\":\"none\",\"typ\":\"JWT\"}");

		assertThatThrownBy(() -> decoder.decode(header + "." + encode(payload(1L, 0)) + "."))
			.isInstanceOf(JwtException.class);
	}

	@Test
	void tokenSignedWithOtherKeyIsRejected() {
		when(userRepository.findTokenVersionById(1L)).thenReturn(Optional.of(0));
		byte[] otherKey = "another-key-another-key-another-".getBytes(StandardCharsets.US_ASCII);

		assertThatThrownBy(() -> decoder.decode(sign(otherKey, payload(1L, 0)))).isInstanceOf(JwtException.class);
	}

	private String tokenFor(long id, int version) {
		User user = mock(User.class);
		when(user.getId()).thenReturn(id);
		when(user.getTokenVersion()).thenReturn(version);
		return tokenService.createAccessToken(user);
	}

	private static String payload(long id, Integer version) {
		long now = Instant.now().getEpochSecond();
		return "{\"iss\":\"websec\",\"sub\":\"" + id + "\",\"iat\":" + now + ",\"exp\":" + (now + 600)
				+ (version == null ? "" : ",\"ver\":" + version) + "}";
	}

	private static String sign(byte[] key, String payload) {
		String content = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}") + "." + encode(payload);
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(key, "HmacSHA256"));
			return content + "."
					+ Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(content.getBytes(StandardCharsets.US_ASCII)));
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static String encode(String json) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}
}
