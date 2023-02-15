package org.reactiverfjava;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

public class RFStreams {
	private static final Logger logger = LoggerFactory.getLogger(RFStreams.class);
	private final BehaviorSubject<float[]> bufferFlow = BehaviorSubject.create();
	private final BehaviorSubject<float[]> bufferFlow2 = BehaviorSubject.create();

	public RFStreams(UsbDevice usb) throws Exception {
		logger.info("Starting RF stream");
		RtlSource rtlDataListener = new RtlSource(bufferFlow);
		usb.addListener(rtlDataListener);
		bufferFlow.subscribe(rtl_buffer -> bufferFlow2.onNext(rtl_buffer));
	}

	public Flowable<float[]> rfFlowable() {
		return bufferFlow.toFlowable(BackpressureStrategy.DROP) //
				.onBackpressureDrop(dropped -> logger.warn("dropped: " + dropped)) //
				.observeOn(Schedulers.io());
	}

	public Flowable<float[]> rfFlowable2() {
		return bufferFlow.toFlowable(BackpressureStrategy.DROP) //
				.onBackpressureDrop(dropped -> logger.warn("dropped: " + dropped)) //
				.observeOn(Schedulers.io());
	}
}
