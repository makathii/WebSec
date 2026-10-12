package com.websec.websec.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Simple in-memory fixed window counters. Good enough for a single instance.
@Service
public class RateLimitService {

	private record Window(long start, long length, int count) {
	}

	private static final int MAX_FAILED_LOGINS = 10;

	private static final Duration FAILED_LOGIN_WINDOW = Duration.ofMinutes(15);

	private final Map<String, Window> windows = new ConcurrentHashMap<>();

	// Failed password checks are counted per account (login, change password, delete account),
	// so a password can't be brute forced from many IPs.
	public boolean tooManyFailedLogins(String key) {
		return isOverLimit("failed-login:" + key, MAX_FAILED_LOGINS);
	}

	public void recordFailedLogin(String key) {
		tryAcquire("failed-login:" + key, Integer.MAX_VALUE, FAILED_LOGIN_WINDOW);
	}

	public void resetFailedLogins(String key) {
		reset("failed-login:" + key);
	}

	public boolean tryAcquire(String key, int limit, Duration window) {
		long now = System.currentTimeMillis();
		Window current = windows.compute(key, (k, old) -> {
			if (old == null || now - old.start() >= old.length()) {
				return new Window(now, window.toMillis(), 1);
			}
			return new Window(old.start(), old.length(), old.count() + 1);
		});
		return current.count() <= limit;
	}

	public boolean isOverLimit(String key, int limit) {
		Window current = windows.get(key);
		return current != null
				&& System.currentTimeMillis() - current.start() < current.length()
				&& current.count() >= limit;
	}

	public void reset(String key) {
		windows.remove(key);
	}

	@Scheduled(fixedRate = 5 * 60 * 1000)
	public void removeOldWindows() {
		long now = System.currentTimeMillis();
		windows.values().removeIf(w -> now - w.start() >= w.length());
	}
}
