package com.oracle.demo.timg.iot.iotdbjdbc.runner;

import java.sql.SQLException;

import com.oracle.demo.timg.iot.iotdbjdbc.dataread.IoTJDBCReader;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Log
/*
 * Using the @Context annotation is general a bad thing as it will force the
 * instantiation at start up, not using lazy instantiation.
 * 
 * We care about creating this class because it's not directly referenced by the
 * controller or classes that are injected / created in it's tree as this is
 * decoupled and only communicates by a queue. If this class wasn't instantiated
 * then there would be nothing to pull events off the queue. That would be a bad
 * thing !
 * 
 * Of course we could tell micronaut to instantiate all the @Singleton beans it
 * controls which would have the same effect, but this way we know for certain
 * it's going to be started
 */
@Context
public class JDBCRunner {
	@Inject
	// as this is a field injection it will happen after the
	// constructor is called, this will let us use the constrictor to play around
	// for example altering the default schema
	private IoTJDBCReader ioTJDBCReader;

	public JDBCRunner(@Property(name = "datasources.default.url") String url,
			@Property(name = "iotdatacache.schemaname") String schemaName) {
		log.info("Using URL " + url);
		log.info("Will eventually use IOT Schama named " + schemaName);
	}

	@EventListener
	public void onStartup(StartupEvent event) throws SQLException {
		log.info("Startup event received, getting data");
		log.info("Raw data entries are :\n" + ioTJDBCReader.getRawData());
	}
}
