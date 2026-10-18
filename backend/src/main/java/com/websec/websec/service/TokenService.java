package com.websec.websec.service;

import com.websec.websec.model.User;
import com.websec.websec.security.SecurityConfig;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class TokenService {

	public static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(10);

	private final JwtEncoder jwtEncoder;

	private final SecureRandom random = new SecureRandom();

	public TokenService(JwtEncoder jwtEncoder) {
		this.jwtEncoder = jwtEncoder;
	}

	public String createAccessToken(User user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(SecurityConfig.JWT_ISSUER)
			.subject(user.getId().toString())
			.claim(SecurityConfig.TOKEN_VERSION_CLAIM, user.getTokenVersion())
			.issuedAt(now)
			.expiresAt(now.plus(ACCESS_TOKEN_TTL))
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public String createRefreshToken() {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	// only this hash is stored in the database, never the token itself
	public String hash(String refreshToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(refreshToken.getBytes(StandardCharsets.US_ASCII)));
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
