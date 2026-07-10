package com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import com.oracle.demo.timg.iot.iotproxygateway.gateway.GatewayStats;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.TaskScheduler;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.ToString;
import lombok.extern.java.Log;

@Singleton
@Log
@Context
public class HomeAssistantMonitoredEntityManager {
	@ToString.Exclude
	private final TaskScheduler taskScheduler;
	@Inject
	private Collection<HomeAssistantMonitoredEntity> monitoredEntities;
	@Inject
	private GatewayStats gatewayStats;
	private List<ScheduledFuture<?>> scheduledFutures;

	public HomeAssistantMonitoredEntityManager(@Named(TaskExecutors.SCHEDULED) TaskScheduler taskScheduler) {
		log.info("In startup monitor constructor");
		this.taskScheduler = taskScheduler;
	}

	@PostConstruct
	void postConstruct() {
		log.info("Startup has given us " + monitoredEntities.size() + " monitored entities which are "
				+ monitoredEntities);
	}

	@EventListener
	public void startup(StartupEvent event) {
		log.info("Starting scheduling of HA entity monitoring");
		scheduledFutures = monitoredEntities.stream().map(monitoredEntity -> {
			log.info("Starting monitored entity " + monitoredEntity);
			ScheduledFuture<?> sf = taskScheduler.scheduleAtFixedRate(monitoredEntity.getInitaldelay(),
					monitoredEntity.getRetrievalrate(), monitoredEntity);
			log.info("Started monitored entity " + monitoredEntity);
			return sf;
		}).collect(Collectors.toList());
	}

	@EventListener
	public void shutdown(ShutdownEvent event) {
		scheduledFutures.stream().forEach(scheduledFuture -> scheduledFuture.cancel(false));
	}
}
