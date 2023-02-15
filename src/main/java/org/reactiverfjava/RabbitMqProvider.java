package org.reactiverfjava;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import org.reactiverfjava.protos.GeneratedDataProtos.GeneratedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

/**
 * Singleton Class that provides access to the RabbitMQ broker
 */
public class RabbitMqProvider {

	private static final Logger logger = LoggerFactory.getLogger(RabbitMqProvider.class);
	private static final String QUEUE_ONE = "queue-one";
	private static final String REQUEST_ONE = "request-one";

	private final BehaviorSubject<GeneratedData> predRequestObs = BehaviorSubject.create();
	private static RabbitMqProvider rmq = null;
	Channel channel;

	private RabbitMqProvider() {
		logger.info("Starting RabbitMQ service");
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		factory.setPort(5672);
		factory.setUsername("guest");
		factory.setPassword("guest");
		try {
			connectToRabbitMq(factory);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		logger.info("RabbitMQ service finished initializing");
	}

	private void connectToRabbitMq(ConnectionFactory factory) throws InterruptedException {
		Connection connection;
		try {
			connection = factory.newConnection();
			channel = connection.createChannel();
			logger.info("Data Service connected to RabbitMQ :: " + QUEUE_ONE);
			channel.queueDeclare(QUEUE_ONE, true, false, false, null);
			channel.queueDeclare(REQUEST_ONE, true, false, false, null);

			DeliverCallback profileRequestCallback = (consumerTag, delivery) -> {
				GeneratedData msg = GeneratedData.parseFrom(delivery.getBody());
				predRequestObs.onNext(msg);
			};

			channel.basicConsume(REQUEST_ONE, true, profileRequestCallback, consumerTag -> {
			});

		} catch (TimeoutException | ConnectException e) {
			logger.error("Timed out trying to connect to RabbitMq -- sleeping(5s) and trying again");
			Thread.sleep(5000);
			connectToRabbitMq(factory);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendGeneratedDataMessageToDb(GeneratedData data) {
		try {
			channel.basicPublish("", QUEUE_ONE, null, data.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Observable<GeneratedData> requestObs() {
		return predRequestObs;
	}

	public static RabbitMqProvider instance() {
		if (rmq == null) {
			rmq = new RabbitMqProvider();
		}
		return rmq;
	}

}
