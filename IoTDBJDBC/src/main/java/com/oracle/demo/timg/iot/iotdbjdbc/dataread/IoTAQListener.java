package com.oracle.demo.timg.iot.iotdbjdbc.dataread;

import java.sql.SQLException;

import io.micronaut.context.annotation.Property;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import jakarta.jms.TopicConnection;
import jakarta.jms.TopicConnectionFactory;
import jakarta.jms.TopicSession;
import jakarta.jms.TopicSubscriber;
import lombok.extern.java.Log;
import oracle.jakarta.jms.AQjmsFactory;
import oracle.jdbc.pool.OracleDataSource;

@Singleton
@Log
/*
 * This class sits on the data source which uses the file system based db token.
 * it really needs to be updated to handle a dynamic token refresh, but that can
 * be done later
 * 
 */
public class IoTAQListener implements MessageListener {

	private final String url;
	private final String schemaName;
	private final String username;
	private final String password;
	private final String aqname;
	private final OracleDataSource dataSource;

	@Inject
	public IoTAQListener(@Property(name = "datasources.default.url") String url,
			@Property(name = "iotdatacache.schemaname") String schemaName,
			@Property(name = "datasources.default.username", defaultValue = "") String username,
			@Property(name = "datasources.default.password", defaultValue = "") String password,
			@Property(name = "iotdatacache.aqname", defaultValue = "raw_data_in") String aqname) throws SQLException {
		this.url = url;
		this.schemaName = schemaName;
		this.username = username;
		this.password = password;
		this.aqname = aqname;
		dataSource = new OracleDataSource();
		dataSource.setURL(url);
		if (username.length() > 0) {
			log.info("Setting username");
			dataSource.setUser(username);
		}

		if (password.length() > 0) {
			log.info("Setting password");
			dataSource.setPassword(password);
		}

	}

	@Override
	public void onMessage(Message message) {
		try {
			// Handle common payload types
			if (message instanceof TextMessage tm) {
				String body = tm.getText();
				// Process message
				log.info("Received TextMessage: " + body);
			} else {
				// don't know this type of message, just output the name and move on
				log.info("Received message type: " + message.getClass().getName());
			}
			// Acknowledge only after successful processing
			message.acknowledge();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private TopicConnectionFactory qcf;
	private TopicConnection conn;
	private TopicSession session;
	private Topic topic;
	private TopicSubscriber subscriber;

	public void connectToAQ() throws JMSException {
		qcf = AQjmsFactory.getTopicConnectionFactory(dataSource);
		conn = qcf.createTopicConnection();
		session = conn.createTopicSession(false, Session.CLIENT_ACKNOWLEDGE);
		topic = session.createTopic(schemaName + "." + aqname);
		subscriber = session.createSubscriber(topic);
		subscriber.setMessageListener(this);
	}

	public void disconnectFromAQ() throws JMSException {
		subscriber.close();
		session.close();
		conn.close();
	}

}
