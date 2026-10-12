package com.websec.websec.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.websec.websec.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Configuration
public class SecurityConfig {

	public static final String JWT_ISSUER = "websec";

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// The API only accepts Bearer tokens, which browsers never attach on their own, so CSRF
			// can't happen there. The refresh cookie is SameSite=Strict and the session endpoints
			// also check the Origin header (see SessionController).
			.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.requestCache(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST, "/api/users").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/users/*", "/api/users/*/posts").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/*", "/api/posts/*/comments").permitAll()
				.requestMatchers(HttpMethod.GET, "/uploads/*").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/session", "/api/session/tokens").permitAll()
				.requestMatchers(HttpMethod.DELETE, "/api/session").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/posts", "/api/posts/*/comments").authenticated()
				.requestMatchers(HttpMethod.PUT, "/api/posts/*/like", "/api/users/me/password").authenticated()
				.requestMatchers(HttpMethod.DELETE, "/api/users/me", "/api/posts/*", "/api/posts/*/like",
						"/api/posts/*/comments/*")
				.authenticated()
				.requestMatchers("/error").permitAll()
				.anyRequest().denyAll())
			.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
			.headers(headers -> headers
				.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'; sandbox"))
				.referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
				.frameOptions(frame -> frame.deny()));
		return http.build();
	}

	@Bean
	public JwtDecoder jwtDecoder(AppProperties properties) {
		// only HS256 is accepted, so "alg: none" or switching the algorithm doesn't work
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtKey(properties))
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JWT_ISSUER));
		return decoder;
	}

	@Bean
	public JwtEncoder jwtEncoder(AppProperties properties) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey(properties)));
	}

	@Bean
	public PasswordEncoder passwordEncoder(AppProperties properties) {
		return new PepperedPasswordEncoder(properties.pepperKey());
	}

	private SecretKey jwtKey(AppProperties properties) {
		return new SecretKeySpec(properties.jwtKey(), "HmacSHA256");
	}
}
