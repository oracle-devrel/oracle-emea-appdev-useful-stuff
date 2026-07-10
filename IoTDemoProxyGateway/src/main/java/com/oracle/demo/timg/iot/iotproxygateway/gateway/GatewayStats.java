package com.oracle.demo.timg.iot.iotproxygateway.gateway;

import java.time.Duration;

import com.oracle.demo.timg.iot.iotproxygateway.PropertyNames;

import io.micronaut.context.annotation.Property;
import jakarta.inject.Singleton;
import lombok.Data;
import lombok.extern.java.Log;

@Data
@Log
@Singleton
public class GatewayStats {
	private GatewayCallTracker sucessfullHARetrieveCalls;
	private GatewayCallTracker failedHARetrieveCalls;
	private GatewayCallTracker sucessfullUploadCalls;
	private GatewayCallTracker failedUploadCalls;
	private long sucessfullHARetrieveWindowSeconds;
	private long failedHARetrieveWindowSeconds;
	private long sucessfullUploadWindowSeconds;
	private long failedUploadWindowSeconds;

	public GatewayStats(
			@Property(name = PropertyNames.GATEWAY_STATS_SUCESSFULL_RETRIEVE_WINDOW, defaultValue = "PT10m") Duration sucessfullHARetrieveWindow,
			@Property(name = PropertyNames.GATEWAY_STATS_FAILED_RETRIEVE_WINDOW, defaultValue = "PT10m") Duration failedHARetrieveWindow,
			@Property(name = PropertyNames.GATEWAY_STATS_SUCESSFULL_UPLOAD_WINDOW, defaultValue = "PT10m") Duration sucessfullUploadWindow,
			@Property(name = PropertyNames.GATEWAY_STATS_FAILED_UPLOAD_WINDOW, defaultValue = "PT10m") Duration failedUploadWindow) {
		this.sucessfullHARetrieveCalls = new GatewayCallTracker(sucessfullHARetrieveWindow);
		this.failedHARetrieveCalls = new GatewayCallTracker(failedHARetrieveWindow);
		this.sucessfullUploadCalls = new GatewayCallTracker(sucessfullUploadWindow);
		this.failedUploadCalls = new GatewayCallTracker(failedUploadWindow);
		this.sucessfullHARetrieveWindowSeconds = sucessfullHARetrieveWindow.getSeconds();
		this.failedHARetrieveWindowSeconds = failedHARetrieveWindow.getSeconds();
		this.sucessfullUploadWindowSeconds = sucessfullUploadWindow.getSeconds();
		this.failedUploadWindowSeconds = failedUploadWindow.getSeconds();
	}

	public void trackSucessfullHARetrieveCall() {
		sucessfullHARetrieveCalls.trackCalls();
	}

	public void trackFailedHARetrieveCall() {
		failedHARetrieveCalls.trackCalls();
	}

	public void trackSucessfullUploadCall() {
		sucessfullUploadCalls.trackCalls();
	}

	public void trackFailedUploadCall() {
		failedUploadCalls.trackCalls();
	}

	public GatewayStatsData getGatewayStatsData() {
		return GatewayStatsData.builder()
				.haretrievesuccess(sucessfullHARetrieveCalls.averageCalls(sucessfullHARetrieveWindowSeconds))
				.haretrievefail(failedHARetrieveCalls.averageCalls(failedHARetrieveWindowSeconds))
				.uploadsuccess(sucessfullUploadCalls.averageCalls(sucessfullUploadWindowSeconds))
				.uploadfail(failedUploadCalls.averageCalls(failedUploadWindowSeconds)).build();
	}

	public GatewayConfigData getGatewayConfigData() {
		return GatewayConfigData.builder().successfullharetrievetimewindow(sucessfullHARetrieveWindowSeconds)
				.failedharetrievetimewindow(failedHARetrieveWindowSeconds)
				.successfulluploadtimewindow(sucessfullUploadWindowSeconds)
				.faileduploadtimewindow(failedUploadWindowSeconds).build();
	}
}
