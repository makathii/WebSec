package com.websec.websec.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Multipart uploads are limited by spring.servlet.multipart.*, everything else is small JSON.
// Rejecting big bodies early keeps huge strings away from Jackson and the regex validation.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestSizeFilter extends OncePerRequestFilter {

	private static final long MAX_JSON_BYTES = 16 * 1024;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String contentType = request.getContentType();
		boolean multipart = contentType != null && contentType.toLowerCase().startsWith("multipart/form-data");
		boolean chunked = request.getHeader("Transfer-Encoding") != null;

		if (!multipart && (chunked || request.getContentLengthLong() > MAX_JSON_BYTES)) {
			response.setStatus(413);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.getWriter().write("{\"error\":\"Request is too large\"}");
			return;
		}
		chain.doFilter(request, response);
	}
}
