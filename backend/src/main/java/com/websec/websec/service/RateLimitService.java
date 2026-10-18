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

	private static final int MAX_FAILED_LOGINS_PER_IP = 10;

	private static final int MAX_FAILED_LOGINS_PER_ACCOUNT = 1000;

	private static final int CAPTCHA_AFTER_FAILED_LOGINS_PER_IP = 3;

	private static final int CAPTCHA_AFTER_FAILED_LOGINS_PER_ACCOUNT = 5;

	private static final Duration FAILED_LOGIN_WINDOW = Duration.ofMinutes(15);

	private final Map<String, Window> windows = new ConcurrentHashMap<>();

	public boolean tooManyFailedLogins(String account, String ip) {
		return isOverLimit(pairKey(account, ip), MAX_FAILED_LOGINS_PER_IP)
				|| isOverLimit(accountKey(account), MAX_FAILED_LOGINS_PER_ACCOUNT);
	}

	public boolean captchaRequired(String account, String ip) {
		return isOverLimit(pairKey(account, ip), CAPTCHA_AFTER_FAILED_LOGINS_PER_IP)
				|| isOverLimit(accountKey(account), CAPTCHA_AFTER_FAILED_LOGINS_PER_ACCOUNT);
	}

	public void recordFailedLogin(String account, String ip) {
		tryAcquire(pairKey(account, ip), Integer.MAX_VALUE, FAILED_LOGIN_WINDOW);
		tryAcquire(accountKey(account), Integer.MAX_VALUE, FAILED_LOGIN_WINDOW);
	}

	public void resetFailedLogins(String account, String ip) {
		reset(pairKey(account, ip));
	}

	private static String pairKey(String account, String ip) {
		return "failed-login:" + account + "|ip:" + ip;
	}

	private static String accountKey(String account) {
		return "failed-login:" + account;
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
