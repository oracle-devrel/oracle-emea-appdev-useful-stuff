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
	private GatewayCallTracker sucessfullRetrieveCalls;
	private GatewayCallTracker failedRetrieveCalls;
	private GatewayCallTracker sucessfullUploadCalls;
	private GatewayCallTracker failedUploadCalls;
	private long sucessfullRetrieveWindowSeconds;
	private long failedRetrieveWindowSeconds;
	private long sucessfullUploadWindowSeconds;
	private long failedUploadWindowSeconds;

	public GatewayStats(
			@Property(name = PropertyNames.GATEWAY_STATS_SUCESSFULL_RETRIEVE_WINDOW, defaultValue = "PT10m") Duration sucessfullRetrieveWindow,
			@Property(name = PropertyNames.GATEWAY_STATS_FAILED_RETRIEVE_WINDOW, defaultValue = "PT10m") Duration failedRetrieveWindow,
			@Property(name = PropertyNames.GATEWAY_STATS_SUCESSFULL_UPLOAD_WINDOW, defaultValue = "PT10m") Duration sucessfullUploadWindow,
			@Property(name = PropertyNames.GATEWAY_STATS_FAILED_UPLOAD_WINDOW, defaultValue = "PT10m") Duration failedUploadWindow) {
		this.sucessfullRetrieveCalls = new GatewayCallTracker(sucessfullRetrieveWindow);
		this.failedRetrieveCalls = new GatewayCallTracker(failedRetrieveWindow);
		this.sucessfullUploadCalls = new GatewayCallTracker(sucessfullUploadWindow);
		this.failedUploadCalls = new GatewayCallTracker(failedUploadWindow);
		this.sucessfullRetrieveWindowSeconds = sucessfullRetrieveWindow.getSeconds();
		this.failedRetrieveWindowSeconds = failedRetrieveWindow.getSeconds();
		this.sucessfullUploadWindowSeconds = sucessfullUploadWindow.getSeconds();
		this.failedUploadWindowSeconds = failedUploadWindow.getSeconds();
	}

	public void trackSucessfullRetrieveCall() {
		sucessfullRetrieveCalls.trackCalls();
	}

	public void trackFailedRetrieveCall() {
		failedRetrieveCalls.trackCalls();
	}

	public void trackSucessfullUploadCall() {
		sucessfullUploadCalls.trackCalls();
	}

	public void trackFailedUploadCall() {
		failedUploadCalls.trackCalls();
	}

	public GatewayStatsData getGatewayStatsData() {
		return GatewayStatsData.builder()
				.haretrievesuccess(sucessfullRetrieveCalls.averageCalls(sucessfullRetrieveWindowSeconds))
				.haretrievefail(failedRetrieveCalls.averageCalls(failedRetrieveWindowSeconds))
				.uploadsuccess(sucessfullUploadCalls.averageCalls(sucessfullUploadWindowSeconds))
				.uploadfail(failedUploadCalls.averageCalls(failedUploadWindowSeconds)).build();
	}

	public GatewayConfigData getGatewayConfigData() {
		return GatewayConfigData.builder().successfullharetrievetimewindow(sucessfullRetrieveWindowSeconds)
				.failedharetrievetimewindow(failedRetrieveWindowSeconds)
				.successfulluploadtimewindow(sucessfullUploadWindowSeconds)
				.faileduploadtimewindow(failedUploadWindowSeconds).build();
	}
}
