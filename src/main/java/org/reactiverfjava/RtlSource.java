package org.reactiverfjava;

import com.g0kla.rtlsdr4java.ComplexBuffer;
import com.g0kla.rtlsdr4java.Listener;

import io.reactivex.subjects.BehaviorSubject;

/**
 * RTL = RealTek Limited -- they created the chipset used for these SDR's
 */
public class RtlSource implements Listener<ComplexBuffer> {
	private BehaviorSubject<float[]> rx;

	public RtlSource(BehaviorSubject<float[]> rx) {
		super();
		this.rx = rx;
	}

	@Override
	public void receive(ComplexBuffer t) {
		float[] IQbuffer = t.getSamples();
		rx.onNext(IQbuffer);
	}
}
