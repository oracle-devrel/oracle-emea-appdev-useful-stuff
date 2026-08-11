package com.oracle.demo.timg.iot.resthandler.controllers;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Requires(property = "restservice.bastion.keepalive.enabled", value = "true", defaultValue = "false")
@Log
public class BastionSessionMaintainer {
	private int counter = 0;
	@Property(name = "restservice.bastion.keepalive.frequency", defaultValue = "60s")
	private String keepaliveString;

	@ExecuteOn(TaskExecutors.IO)
	@Scheduled(fixedRate = "${restservice.bastion.keepalive.frequency:60s}")
	public void printKeepAlive() {
		log.info("bastion keep alive, count " + counter);
		counter++;
	}

	@PostConstruct
	public void postConstruct() {
		log.info("BastionSessionMaintainer is running with time loop of " + keepaliveString);
	}
}
