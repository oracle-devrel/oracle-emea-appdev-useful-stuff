/*Copyright (c) 2025 Oracle and/or its affiliates.

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
package com.oracle.demo.timg.iot.iotsonnenuploader.sonnendatacapture;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.scheduling.annotation.Scheduled;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Log
@Singleton
@Requires(property = "datacapture.enabled", value = "true", defaultValue = "false")
@Requires(property = "datacapture.starttimestamp")
@Requires(property = "datacapture.captureduration")
public class CaptureTimerController {
	@Property(name = "datacapture.starttimestamp")
	private ZonedDateTime startTimestamp;
	@Property(name = "datacapture.captureduration")
	private Duration captureDuration;

	@Inject
	private ApplicationContext appCtxt;

	@Inject
	private ConfigurationDataCapture configurationDataCapture;

	@Inject
	private StatusDataCapture statusDataCapture;

	@Inject
	private ObjectMapper mapper;

	private boolean waitingToStartDataCapture = true;
	private ZonedDateTime endTimestamp;

	@Scheduled(fixedRate = "${datacapture.timer.frequency:60s}", initialDelay = "${datacapture.timer.initialdelay:1s}")
	@ExecuteOn(TaskExecutors.IO)
	public synchronized void checkTimer() {
		if (waitingToStartDataCapture) {
			if (ZonedDateTime.now().isAfter(startTimestamp)) {
				DataCaptureConfig dataCaptureConfig = DataCaptureConfig.builder()
						.starttime(ZonedDateTime.from(startTimestamp)).duration(captureDuration).build();
				String dataCaptureConfigString;
				try {
					dataCaptureConfigString = mapper.writeValueAsString(dataCaptureConfig);
				} catch (IOException e) {
					log.severe(
							"Major problem, can't serialize the data capture config, will try again but this could be an issue");
					return;
				}

				configurationDataCapture.setDataCaptureConfig(dataCaptureConfigString);
				statusDataCapture.setDataCaptureConfig(dataCaptureConfigString);
				log.info("Reached start time, enabling data capture and saving using configuration "
						+ dataCaptureConfig);
				configurationDataCapture.setDontWrite(false);
				statusDataCapture.setDontWrite(false);
				waitingToStartDataCapture = false;
				// force an entry to be generated
				configurationDataCapture.processConfiguration();
				statusDataCapture.processStatus();
			} else {
				log.info("Not yet reached start time");
			}
		} else {
			if (endTimestamp.isBefore(ZonedDateTime.now())) {
				// force an entry to be generated
				configurationDataCapture.processConfiguration();
				statusDataCapture.processStatus();
				configurationDataCapture.setDontWrite(true);
				statusDataCapture.setDontWrite(true);
				// give things a minute to shutdown, this allows for delays or timeouts in
				// getting data from the battery
				try {
					Thread.sleep(Duration.ofMinutes(1));
				} catch (InterruptedException e) {
					log.warning(
							"Shutdown sleep interupted, this may result in the saved data files not closing cleanly");
				}
				log.info("Completed run, stopping application context");
				appCtxt.stop();
				// stopping the context doesn't seem to stop the scheduled tasks, so force a jvm
				// shutdown, this isn't the nicest way to do things, but it means we don't have
				// JVM's left running
				log.info("Completed run, stopping JVM");
				System.exit(0);
			}
		}
	}

	@EventListener
	public void onStartup(StartupEvent event) {
		endTimestamp = startTimestamp.plus(captureDuration);
		log.info("Will start data saving at " + startTimestamp.format(DateTimeFormatter.ISO_DATE_TIME) + " and finish "
				+ captureDuration + " later at " + endTimestamp.format(DateTimeFormatter.ISO_DATE_TIME));
	}
}
