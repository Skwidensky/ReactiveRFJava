package org.reactiverfjava;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.reactiverfjava.protos.GeneratedDataProtos.GeneratedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReactiveRFJavaMain {

	private static final Logger logger = LoggerFactory.getLogger(ReactiveRFJavaMain.class);
	private static final short VENDOR_ID = 0x0bda; // Used to find SDR USB device
	private static final short PRODUCT_ID = 0x2838; // Used to find SDR USB device
	private static final int SAMPLE_RATE = 240000;
	private static final int AF_SAMPLE_RATE = 48000;
	private static final int DECIMATION_RATE = SAMPLE_RATE / AF_SAMPLE_RATE;
	private static KafkaProvider kp;
	private static RabbitMqProvider rmq;

	public static void main(String[] args) throws Exception {
		logger.info("Starting ReactiveRXJava");
		/** Set frequency and other stuff for {@link R820TTunerController} */
		UsbDevice usb = new UsbDevice(VENDOR_ID, PRODUCT_ID, SAMPLE_RATE);
		/** Provides Rx streams for RF frames */
		RFStreams rfstreams = new RFStreams(usb);
		/** Kafka producer */
//		kp = KafkaProvider.instance();
		/** RabbitMQ messenger */
		rmq = RabbitMqProvider.instance();

		rfstreams.rfFlowable2().subscribe(rtl_buffer -> {
			System.out.println("Got samples:" + rtl_buffer.length);
			GeneratedData.Builder bldr = GeneratedData.newBuilder();
			List<Double> ds = IntStream.range(0, rtl_buffer.length).mapToDouble(i -> rtl_buffer[i]).boxed()
					.collect(Collectors.toList());
			bldr.setTimestamp(System.currentTimeMillis()).addAllRfFrame(ds);
//			kp.producerSubject().onNext(bldr.build());
			rmq.sendGeneratedDataMessageToDb(bldr.build());
		});

		// yuck
		Thread.currentThread().join();
	}
}
