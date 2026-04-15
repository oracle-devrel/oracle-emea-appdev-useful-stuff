package com.oracle.demo.timg.iot.iotdbjdbc.dataread;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.NormalizedDataMessageHandlerService;
import com.oracle.demo.timg.iot.iotdbjdbc.oci.DBConnectionSupplier;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.Min;
import lombok.extern.java.Log;
import oracle.jdbc.aq.AQDequeueOptions;
import oracle.jdbc.aq.AQMessage;

@Singleton
@Log
@Requires(property = "iotdatacache.aq.reader.enabled", value = "true", defaultValue = "false")
@Requires(property = "iotdatacache.aq.reader.order")
public class IoTAQNormalizedDataReader extends IoTAQNormalizedDataCore implements IoTDBClient, Runnable {

	public final static String QUEUE_SUBSCRIBER_SUFFIX = "reader";
	private boolean stopped = false;
	private final AQDequeueOptions dequeueOptions;
	private final int aqReadTimeout;
	private final int aqBatchSize;
	private final int order;
	private Thread currentThread;
	private ExecutorService executor;

	@Inject
	private NormalizedDataMessageHandlerService normalizedDataMessageHandlerService;

	@Inject
	public IoTAQNormalizedDataReader(DBConnectionSupplier dbConnectionSupplier,
			@Property(name = "iotdatacache.schemaname") String schemaName,
			@Property(name = "iotdatacache.validationtimeout", defaultValue = "5") @Min(value = 1) int jdbcValidationTimeout,
			@Property(name = "iotdatacache.aq.subscribername", defaultValue = "aqclient") String aqsubscribername,
			@Property(name = "iotdatacache.aq.readtimeout", defaultValue = "10") @Min(value = 0) int aqReadTimeout,
			@Property(name = "iotdatacache.aq.batchsize", defaultValue = "10") @Min(value = 1) int aqBatchSize,
			@Property(name = "iotdatacache.aq.reader.order") @Min(value = 0) int order) throws SQLException, Exception {
		super(dbConnectionSupplier, schemaName, jdbcValidationTimeout, aqsubscribername + QUEUE_SUBSCRIBER_SUFFIX);
		this.aqReadTimeout = aqReadTimeout;
		this.aqBatchSize = aqBatchSize;
		this.order = order;
		dequeueOptions = new AQDequeueOptions();
		dequeueOptions.setDequeueMode(AQDequeueOptions.DequeueMode.REMOVE);
		dequeueOptions.setWait(aqReadTimeout);
		dequeueOptions.setNavigation(AQDequeueOptions.NavigationOption.FIRST_MESSAGE);
		dequeueOptions.setConsumerName(aqsubscribername + QUEUE_SUBSCRIBER_SUFFIX);
	}

	@Override
	public void configureDBClient(String filteringRule) throws Exception {
		executor = Executors.newCachedThreadPool();
		// the aq options have been set in the constructor, but we need to tell the
		// database about us
		super.addSubscriber(filteringRule);
	}

	private void readAQMessages() throws SQLException {
		log.info("Starting to read AQ messages");
		int readCounter = 0;
		while (!stopped) {
			// read a value
			AQMessage messages[];
			try {
				messages = connection.dequeue(normalisedQueueName, dequeueOptions, "JSON", aqBatchSize);
			} catch (SQLException e) {
				if (e.getErrorCode() == 25228) {
					log.info("Timeout reading messages");
					continue;
				}
				log.info("SQLException getting messages, " + e.getLocalizedMessage());
				continue;
			}
			// I guess if we have a timeout while waiting for a message to be available to
			// dequeue there will be a null message
			if (messages == null) {
				log.info("Received a null message");
				continue;
			}
			log.info("Queue returned " + messages.length + " out of a max read size of " + aqBatchSize);
			// ultimately this could be a stream, but we want to track the number when
			// processing so let's use a loop.
			for (int i = 0; i < messages.length; i++) {
				try {
					NormalizedData normalizedData = convertToNormalizedData(messages[i].getJSONPayload());
					log.info("Received message block " + readCounter + ", message no " + i + ", " + normalizedData);
					normalizedDataMessageHandlerService.handle(normalizedData);
				} catch (SQLException e) {
					log.info("SQLException processing message block " + readCounter + ", message" + i);
				}
			}
			readCounter++;
			connection.commit();
		}
		log.info("Finished to reading AQ messages");
	}

	@Override
	public void startDBProcessing() throws Exception {
		// start this in a separate loop
		executor.execute(() -> this.run());
	}

	@Override
	public void stopDBProcessing() throws Exception {
		log.info("Stopping reading");
		this.stopped = true;
		// interrupt the thread if it's not null
		if (currentThread != null) {
			log.info("Interrupting thread");
			currentThread.interrupt();
		}
	}

	@Override
	public void unconfigureDBClient() throws Exception {
		// stop the executors from accepting new tasks
		executor.shutdown();
		// unconfigure the queue
		removeSubscriber();
	}

	@Override
	public void run() {
		// save the thread we're running in so we can interrupt it later
		currentThread = Thread.currentThread();
		try {
			log.info("Starting read loop");
			readAQMessages();
		} catch (SQLException e) {
			log.severe("SQLException in read loop, " + e.getLocalizedMessage());
		}
		log.info("Completed read thread");
	}

	@Override
	public int getOrder() {
		return order;
	}
}
