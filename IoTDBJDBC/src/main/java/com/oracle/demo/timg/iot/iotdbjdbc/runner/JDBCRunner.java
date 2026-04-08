package com.oracle.demo.timg.iot.iotdbjdbc.runner;

import java.sql.SQLException;
import java.util.stream.Collectors;

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
public class JDBCRunner implements Runnable {
	@Inject
	// as this is a field injection it will happen after the
	// constructor is called, this will let us use the constrictor to play around
	// for example altering the default schema
	private IoTJDBCReader ioTJDBCReader;
//	@Inject
//	private IoTAQJMSListener ioTAQJMSListener;

	private final int aqRuntime;
	private final boolean listenToAq;

	public JDBCRunner(@Property(name = "datasources.default.url") String url,
			@Property(name = "iotdatacache.schemaname") String schemaName,
			@Property(name = "datasources.default.username", defaultValue = "") String username,
			@Property(name = "datasources.default.password", defaultValue = "") String password,
			@Property(name = "iotdatacache.aqname", defaultValue = "raw_data_in") String aqname,
			@Property(name = "iotdatacache.listentoaq", defaultValue = "true") boolean listenToAq,
			@Property(name = "iotdatacache.aqruntime", defaultValue = "120") int aqRuntime) {
		log.info("Using URL " + url);
		log.info("Will eventually use IOT Schama named " + schemaName);
		log.info("username is :" + username + ":");
		log.info("password is :" + password + ":");
		log.info("aqname is :" + aqname + ":");
		log.info("listenToAq is :" + listenToAq + ":");
		log.info("aqRuntime is :" + aqRuntime + ":");
		this.listenToAq = listenToAq;
		this.aqRuntime = aqRuntime;
	}

	@EventListener
	public void onStartup(StartupEvent event) throws SQLException, Exception {
		log.info("Startup event received, getting data");
		String entries = ioTJDBCReader.getRawData().stream().map(rd -> rd.toString()).collect(Collectors.joining("\n"));
		log.info("Raw data entries are :\n" + entries);

//		if (listenToAq) {
//			// yes this should use executors and the like, but this is a basic
//			// demo, not a production setup
//			Thread t = new Thread(this);
//			t.start();
//		}
	}

	@Override
	public void run() {
//		log.info("Starting AQ processing");
//		try {
//			ioTAQJMSListener.connectToAQ(ioTAQJMSListener);
//			if (aqRuntime > 0) {
//				log.info("Entering run wait for AQ");
//				Thread.sleep(aqRuntime * 1000);
//				ioTAQJMSListener.disconnectFromAQ();
//			}
//		} catch (JMSException e) {
//			// TODO Auto-generated catch block
//			log.warning("JMW Exception setting up queue " + e.getLocalizedMessage());
//		} catch (InterruptedException e) {
//			log.warning("Opps, got interruped while waiting for the AQ time out, " + e.getLocalizedMessage());
//		} catch (Exception e) {
//			log.warning("Some unexpected exception occured, " + e.getLocalizedMessage());
//		}
//		log.info("Completed AQ processing");
	}
}
