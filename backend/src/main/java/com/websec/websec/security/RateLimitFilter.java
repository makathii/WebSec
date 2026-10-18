package com.websec.websec.security;

import com.websec.websec.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

// Runs after the Spring Security filter chain, so the logged-in user is known here.
// IP is taken from the connection only. There is no proxy, so X-Forwarded-For is not trusted.
@Component
public class RateLimitFilter extends OncePerRequestFilter {

	private static final Duration MINUTE = Duration.ofMinutes(1);

	private final RateLimitService rateLimitService;

	public RateLimitFilter(RateLimitService rateLimitService) {
		this.rateLimitService = rateLimitService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (!allowed(request)) {
			response.setStatus(429);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.getWriter().write("{\"error\":\"Too many requests, try again later\"}");
			return;
		}
		chain.doFilter(request, response);
	}

	private boolean allowed(HttpServletRequest request) {
		String method = request.getMethod();
		String path = request.getServletPath();
		String ip = request.getRemoteAddr();

		if (path.equals("/api/users/me/password") || path.equals("/api/users/me")) {
			return method.equals("GET") || rateLimitService.tryAcquire("account:" + userKey(ip), 5, MINUTE);
		}
		if (path.equals("/api/session/challenge")) {
			return rateLimitService.tryAcquire("challenge:" + ip, 10, MINUTE);
		}
		if (!method.equals("POST")) {
			return true;
		}

		if (path.equals("/api/session")) {
			return rateLimitService.tryAcquire("login:" + ip, 5, MINUTE);
		}
		if (path.equals("/api/users")) {
			return rateLimitService.tryAcquire("signup:" + ip, 3, MINUTE);
		}
		if (path.equals("/api/session/tokens")) {
			return rateLimitService.tryAcquire("refresh:" + ip, 30, MINUTE);
		}
		if (path.equals("/api/posts")) {
			return rateLimitService.tryAcquire("upload:" + userKey(ip), 10, MINUTE);
		}
		if (path.matches("/api/posts/\\d+/comments")) {
			return rateLimitService.tryAcquire("comment:" + userKey(ip), 20, MINUTE);
		}
		return true;
	}

	private String userKey(String ip) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth instanceof JwtAuthenticationToken ? "user:" + auth.getName() : "ip:" + ip;
	}
}
