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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.oracle.demo.timg.iot.iotsonnenuploader.incommingdata.SonnenStatus;
import com.oracle.demo.timg.iot.iotsonnenuploader.sonnenbatteryhttpclient.SonnenBatteryClient;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.scheduling.annotation.Scheduled;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

@Log
@Singleton
@Requires(property = "datacapture.statussave.enabled", value = "true", defaultValue = "false")
// There must be a start / end time, though that's actually managed by the CaptureTimerController
@Requires(property = "datacapture.starttimestamp")
@Requires(property = "datacapture.captureduration")
public class StatusDataCapture {
	@Getter
	@Setter
	private boolean dontWrite = true;
	@Getter
	@Setter
	private String dataCaptureConfig;
	@Inject
	private SonnenBatteryClient sonnenClient;
	@Property(name = "datacapture.statussave.filename", defaultValue = "./saveddata/status.json")
	private String statusFileName;
	@Property(name = "datacapture.statussave.overwrite", defaultValue = "false")
	private boolean overwrite;

	@Inject
	private ObjectMapper mapper;

	private BufferedWriter writer;
	private boolean shutdownInProgress = false;
	private boolean writeDataCaptureConfiguration = true;

	@Scheduled(fixedRate = "${datacapture.statussave.frequency:10s}", initialDelay = "${datacapture.statussave.initialdelay:5s}")
	@ExecuteOn(TaskExecutors.IO)
	public synchronized SonnenStatus processStatus() {
		if (writer == null) {
			log.info("writer is null, not proceeding with this iteration");
			return null;
		}
		if (dontWrite) {
			log.info("dontWrite is set, not proceeding with this iteration");
			return null;
		}
		if (writeDataCaptureConfiguration) {
			if (dataCaptureConfig == null) {
				log.warning("IOException writing the time header, cannot continue with this itteration");
				return null;
			}
			try {
				writer.append(dataCaptureConfig).append("\n").flush();
				writeDataCaptureConfiguration = false;
			} catch (IOException e) {
				log.warning("IOException writing the time header, cannot continue with this itteration");
				return null;
			}
		}
		SonnenStatus status;
		try {
			status = sonnenClient.fetchStatus();
		} catch (HttpClientException e) {
			log.warning(
					"HttpClientException getting status from sonnen, " + e.getLocalizedMessage() + "no data to save");
			return null;
		}
		log.info("Retrieved status from battery : " + status);
		String json;
		try {
			json = mapper.writeValueAsString(status);
		} catch (IOException e) {
			log.warning("Unable to serialise status to json because " + e.getLocalizedMessage());
			return null;
		}
		try {
			if (!shutdownInProgress) {
				writer.append(json).append("\n").flush();
			}
		} catch (IOException e) {
			log.warning("Unable to save status json to file or flush it because " + e.getLocalizedMessage());
			return null;
		}
		return status;
	}

	@EventListener
	public void onStartup(StartupEvent event) {
		log.info("Startup event received for status data capturer, output file=" + statusFileName);
		try {
			if (new File(statusFileName).exists() && (!overwrite)) {
				log.info("output file " + statusFileName + " exists and ovewrwrite is false, nowhere to write data");
			} else {
				writer = new BufferedWriter(new FileWriter(statusFileName));
				log.info("Created writer");
			}
		} catch (IOException e) {
			log.warning("Unable to open writer to file " + statusFileName + " due to " + e.getLocalizedMessage()
					+ "\nNo status data will be saved.");
			return;
		}
	}

	@EventListener
	public void onShutdown(ShutdownEvent event) {
		if (writer != null) {
			try {
				shutdownInProgress = true;
				writer.close();
				writer = null;
			} catch (IOException e) {
				log.warning("Unable to close writer, json file may be incomplete");
			}
		}
	}
}
