/*Copyright (c) 2026 Oracle and/or its affiliates.

The Universal Permissive License (UPL), Version 1.0

Subject to the condition set forth below, permission is hereby granted to any
person obtaining a copy of this software, associated documentation and/or data
(collectively the "Software"), free of charge and under any and all copyright
rights in the Software, and any and all patent rights owned or freely
licensable by each licensor hereunder covering either (i) the unmodified
Software as contributed to or provided by such licensor, or (ii) the Larger
Works (as defined below), to deal in both

(a) the Software, and
(b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
one is included with the Software (each a "Larger Work" to which the Software
is contributed by such licensors),

without restriction, including without limitation the rights to copy, create
derivative works of, display, perform, and distribute the Software and make,
use, sell, offer for sale, import, export, have made, and have sold the
Software and the Larger Work(s), and to sublicense the foregoing rights on
either these or other terms.

This license is subject to the following condition:
The above copyright notice and either this complete permission notice or at
a minimum a reference to the UPL must be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package com.oracle.demo.timg.iot.iotproxygateway.inputs.gateway;

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

	public synchronized void reset() {
		callTimes.clear();
	}

	public synchronized void trackCalls(Instant instant) {
		if (instant == null) {
			callTimes.addLast(clock.instant());
		} else {
			callTimes.addLast(instant);
		}
	}

	public synchronized void trackCalls() {
		trackCalls(null);
	}

	public synchronized int callCount() {
		return callCount(null);
	}

	public synchronized int callCount(Instant instant) {
		if (instant == null) {
			pruneCallsBefore(clock.instant());
		} else {
			pruneCallsBefore(instant);
		}
		return callTimes.size();
	}

	public synchronized double averageCalls(long periodSeconds) {
		validatePeriodSeconds(periodSeconds);
		pruneCallsBefore(clock.instant());

		double windowSeconds = trackingWindow.getSeconds() + trackingWindow.getNano() / 1_000_000_000.0;
		return callTimes.size() * periodSeconds / windowSeconds;
	}

	public synchronized double averageCalls(long periodSeconds, Instant instant) {
		validatePeriodSeconds(periodSeconds);
		pruneCallsBefore(instant);

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
