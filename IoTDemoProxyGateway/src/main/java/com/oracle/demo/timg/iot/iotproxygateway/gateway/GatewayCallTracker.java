package com.oracle.demo.timg.iot.iotproxygateway.gateway;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public class GatewayCallTracker {
	private static final Duration DEFAULT_TRACKING_WINDOW = Duration.ofMinutes(10);

	private final Clock clock;
	private final Duration trackingWindow;
	private final Deque<Instant> callTimes = new ArrayDeque<>();

	public GatewayCallTracker() {
		this(DEFAULT_TRACKING_WINDOW);
	}

	public GatewayCallTracker(Duration trackingWindow) {
		this(trackingWindow, Clock.systemUTC());
	}

	GatewayCallTracker(Duration trackingWindow, Clock clock) {
		this.trackingWindow = validateTrackingWindow(trackingWindow);
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	public synchronized void trackCalls() {
		callTimes.addLast(clock.instant());
	}

	public synchronized int callCount() {
		pruneCallsBefore(clock.instant());
		return callTimes.size();
	}

	public synchronized double averageCalls(long periodSeconds) {
		validatePeriodSeconds(periodSeconds);
		pruneCallsBefore(clock.instant());

		double windowSeconds = trackingWindow.getSeconds() + trackingWindow.getNano() / 1_000_000_000.0;
		return callTimes.size() * periodSeconds / windowSeconds;
	}

	private void pruneCallsBefore(Instant now) {
		Instant cutoff = now.minus(trackingWindow);

		while (!callTimes.isEmpty() && callTimes.peekFirst().isBefore(cutoff)) {
			callTimes.removeFirst();
		}
	}

	private static Duration validateTrackingWindow(Duration trackingWindow) {
		Objects.requireNonNull(trackingWindow, "trackingWindow");

		if (trackingWindow.isZero() || trackingWindow.isNegative()) {
			throw new IllegalArgumentException("trackingWindow must be greater than zero");
		}

		return trackingWindow;
	}

	private static void validatePeriodSeconds(long periodSeconds) {
		if (periodSeconds <= 0) {
			throw new IllegalArgumentException("periodSeconds must be greater than zero");
		}
	}
}
