package org.reactiverfjava;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.reactiverfjava.protos.GeneratedDataProtos.GeneratedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.subjects.BehaviorSubject;

/**
 * Singleton Class that provides access to the RabbitMQ broker
 */
public class KafkaProvider {

	private static final Logger logger = LoggerFactory.getLogger(KafkaProvider.class);
	private static final String TOPIC_ONE = "hello-world";
	private static final String REQUEST_ONE = "request-one";

	private final Producer<String, GeneratedData> kafkaProducer;
	private final BehaviorSubject<GeneratedData> producerSubject = BehaviorSubject.create();
	private static KafkaProvider kp = null;

	private KafkaProvider() {
		logger.info("Configuring and connecting to Kafka server");
		Properties props = new Properties();
		props.put("bootstrap.servers", "localhost:9092");
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer"); // k: String
		props.put("value.serializer", "io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer"); // v: protobuf
		props.put("schema.registry.url", "http://127.0.0.1:8081");
		kafkaProducer = new KafkaProducer<>(props);

		producerSubject.subscribe(msg -> {
			ProducerRecord<String, GeneratedData> record = new ProducerRecord<>(TOPIC_ONE, msg);
			kafkaProducer.send(record).get();
			kafkaProducer.close();
		});
	}

	public BehaviorSubject<GeneratedData> producerSubject() {
		return producerSubject;
	}

	public void closeKafkaProducer() {
		kafkaProducer.close();
	}

	public static KafkaProvider instance() {
		if (kp == null) {
			kp = new KafkaProvider();
		}
		return kp;
	}

}
