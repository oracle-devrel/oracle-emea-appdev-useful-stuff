package com.oracle.demo.timg.iot.iotdbjdbc.runner;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.oracle.demo.timg.iot.iotdbjdbc.dataread.IoTDBClient;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Log
/*
 * Using the @Context annotation is general a bad thing as it will force the
 * instantiation at start up, not using lazy instantiation.
 * 
 * Of course we could tell micronaut to instantiate all the @Singleton beans it
 * controls which would have the same effect, but this way we know for certain
 * it's going to be started
 */
@Context
public class JDBCRunner {

	private final List<IoTDBClient> ioTDBClients;
	// we only want a single thread to call things later on
	private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	private final int aqRuntime;

	private final List<IoTDBClient> configuredClients;

	public JDBCRunner(List<IoTDBClient> ioTDBClients, @Property(name = "datasources.default.url") String url,
			@Property(name = "iotdatacache.schemaname") String schemaName,
			@Property(name = "datasources.default.username", defaultValue = "") String username,
			@Property(name = "datasources.default.password", defaultValue = "") String password,
			@Property(name = "iotdatacache.aq.usereader", defaultValue = "false") boolean readFromAq,
			@Property(name = "iotdatacache.aq.uselistener", defaultValue = "false") boolean listenToAq,
			@Property(name = "iotdatacache.jdbc.doconnectiontestread", defaultValue = "false") boolean doJDBCConnectionTestRead,
			@Property(name = "iotdatacache.aqruntime", defaultValue = "120") int aqRuntime) {
		log.info("Using URL " + url);
		log.info("Will use IOT Schema named " + schemaName);
		log.info("username is :" + username + ":");
		log.info("password is :" + password + ":");
		log.info("usereader is :" + listenToAq + ":");
		log.info("uselistener is :" + listenToAq + ":");
		log.info("doconnectiontestread is :" + doJDBCConnectionTestRead + ":");
		log.info("aqRuntime is :" + aqRuntime + ":");

		this.aqRuntime = aqRuntime;
		this.ioTDBClients = ioTDBClients.stream().sorted().toList();
		String iotDBClientDetails = ioTDBClients.stream()
				.map(client -> client.getName() + "(" + client.getConfig() + ")").collect(Collectors.joining(", "));
		log.info("There are " + ioTDBClients.size() + " IoTDBClients configured - " + iotDBClientDetails);
		// place to put the clients we have started
		configuredClients = new ArrayList<>(ioTDBClients.size());
	}

	@EventListener
	public void onStartup(StartupEvent event) throws SQLException, Exception {
		// get the clients running
		executor.execute(() -> configureAndStartDBClients());
		// if there is a time limit then schedule a shutdown
		if (aqRuntime > 0) {
			executor.schedule(() -> {
				stopAndUnconfigureDBClients();
				// get the executor itself to shutdown when we've exited this call
				executor.shutdown();
			}, aqRuntime, TimeUnit.SECONDS);
		}
	}

	/**
	 * 
	 */
	protected void configureAndStartDBClients() {
		log.info("Configuring all the clients");
		ioTDBClients.stream().forEach(client -> {
			log.info("Configuring client " + client.getName());
			try {
				client.configureDBClient();
				// stash it away for the next stage
				configuredClients.add(client);
				log.info("Configured client " + client.getName());
			} catch (Exception e) {
				log.info("Exception configuring client " + client.getName() + " with configuration "
						+ client.getConfig() + ", " + e.getLocalizedMessage());
			}
		});

		log.info("Starting processing of the clients");
		configuredClients.stream().forEach(client -> {
			log.info("Starting processing for client " + client.getName());
			try {
				client.startDBProcessing();
				// stash it away for the next stage
				configuredClients.add(client);
				log.info("Configured client " + client.getName());
			} catch (Exception e) {
				log.info("Exception configuring client " + client.getName() + " with configuration "
						+ client.getConfig() + ", " + e.getLocalizedMessage());
			}
		});
	}

	/**
	 * 
	 */
	protected void stopAndUnconfigureDBClients() {
		log.info("Stopping all the clients");
		configuredClients.stream().forEach(client -> {
			log.info("Stopping client processing" + client.getName());
			try {
				client.stopDBProcessing();
				log.info("Stopped client " + client.getName());
			} catch (Exception e) {
				log.info("Exception stopping client " + client.getName() + " with configuration " + client.getConfig()
						+ ", " + e.getLocalizedMessage());
			}
		});

		log.info("Unconfiguring the clients");
		configuredClients.stream().forEach(client -> {
			log.info("Unconfiguring client " + client.getName());
			try {
				client.unconfigureDBClient();
				// stash it away for the next stage
				configuredClients.add(client);
				log.info("Unconfigured client " + client.getName());
			} catch (Exception e) {
				log.info("Exception unconfigured client " + client.getName() + " with configuration "
						+ client.getConfig() + ", " + e.getLocalizedMessage());
			}
		});
	}
}
