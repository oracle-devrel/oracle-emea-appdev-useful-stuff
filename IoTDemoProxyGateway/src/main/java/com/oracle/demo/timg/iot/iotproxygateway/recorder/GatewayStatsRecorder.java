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
package com.oracle.demo.timg.iot.iotproxygateway.recorder;

import java.time.Duration;

import com.oracle.demo.timg.iot.iotproxygateway.PropertyNames;
import com.oracle.demo.timg.iot.iotproxygateway.gateway.GatewayStatsTrackingData;
import com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities.HomeAssistantEntityRetrieveStatus;
import com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities.HomeAssistantMonitoredEntity;
import com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities.HomeAssistantMonitoredEntitySet;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Log
@Singleton
@Requires(property = PropertyNames.OPERATING_MODE_OUTPUT, value = "RECORDER")
public class GatewayStatsRecorder extends GatewayStatsTrackingData {
	private final Recorder recorder;

	@Inject
	public GatewayStatsRecorder(Recorder recorder,
			@Property(name = PropertyNames.GATEWAY_STATS_SUCESSFULL_RETRIEVE_WINDOW, defaultValue = "PT10m") Duration sucessfullHARetrieveWindow,
			@Property(name = PropertyNames.GATEWAY_STATS_FAILED_RETRIEVE_WINDOW, defaultValue = "PT10m") Duration failedHARetrieveWindow,
			@Property(name = PropertyNames.GATEWAY_STATS_SUCESSFULL_UPLOAD_WINDOW, defaultValue = "PT10m") Duration sucessfullUploadWindow,
			@Property(name = PropertyNames.GATEWAY_STATS_FAILED_UPLOAD_WINDOW, defaultValue = "PT10m") Duration failedUploadWindow) {
		super(sucessfullHARetrieveWindow, failedHARetrieveWindow, sucessfullUploadWindow, failedUploadWindow);
		log.info("In constructor");
		this.recorder = recorder;
	}

	/*
	 * @Inject public GatewayStatsRecorder(Recorder recorder){
	 * log.info("In constructor"); this.recorder = recorder; }
	 */

	@Override
	public void trackSucessfullHARetrieveCall(HomeAssistantMonitoredEntitySet homeAssistantMonitoredEntitySet,
			HomeAssistantMonitoredEntity entity) {
		recorder.recordSucessfullHARetrieveCall(homeAssistantMonitoredEntitySet, entity);
		super.trackSucessfullHARetrieveCall(homeAssistantMonitoredEntitySet, entity);
	}

	@Override
	public void trackFailedHARetrieveCall(HomeAssistantEntityRetrieveStatus retrieveStatus,
			HomeAssistantMonitoredEntitySet homeAssistantMonitoredEntitySet, HomeAssistantMonitoredEntity entity) {
		recorder.recordFailedHARetrieveCall(retrieveStatus, homeAssistantMonitoredEntitySet, entity);
		super.trackFailedHARetrieveCall(retrieveStatus, homeAssistantMonitoredEntitySet, entity);
	}
	/*
	 * @Override public void resetHAStats() { // this does nothing at this point,
	 * the reset is actually only called by the // high speed replay }
	 * 
	 * @Override public void trackSucessfullUploadCall() { // when recording we
	 * don't actually do uploads }
	 * 
	 * @Override public void trackFailedUploadCall() { // when recording we don't
	 * actually do uploads }
	 */

	@Override
	public void resetUploadStats() {
		// this does nothing at this point, the reset is actually only called by the
		// high speed replay
	}
}
