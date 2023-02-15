package org.reactiverfjava;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.ConfigDescriptor;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import com.g0kla.rtlsdr4java.ComplexBuffer;
import com.g0kla.rtlsdr4java.DeviceException;
import com.g0kla.rtlsdr4java.Listener;
import com.g0kla.rtlsdr4java.R820TTunerController;
import com.g0kla.rtlsdr4java.RTL2832TunerController;
import com.g0kla.rtlsdr4java.RTL2832TunerController.SampleRate;
import com.g0kla.rtlsdr4java.TunerType;

public class UsbDevice {
	private static final Logger logger = LoggerFactory.getLogger(UsbDevice.class);
	public Device device;
	public DeviceDescriptor deviceDescriptor;
	private R820TTunerController rtl;

	public UsbDevice(short vendorId, short productId, int sampleRate) throws Exception {
		// Initialize the default context
		int libUsbSuccessfullyInitialized = LibUsb.init(null);
		if (libUsbSuccessfullyInitialized != LibUsb.SUCCESS)
			throw new LibUsbException("Unable to initialize libusb.", libUsbSuccessfullyInitialized);

		listNumberOfAvailableDevices();
		rtl = findDevice(vendorId, productId, sampleRate);
		if (rtl == null)
			throw new Exception("RTL not available,  is it plugged in?  Does it have the updated drivers?");
		listDeviceConfiguration(this.device);
	}

	public void exit() {
		LibUsb.exit(null);
	}

	public void addListener(Listener<ComplexBuffer> listener) {
		rtl.addListener(listener);
	}

	public void setTunedFrequency(long frequency) throws DeviceException {
		rtl.setTunedFrequency(frequency);
	}

	private R820TTunerController findDevice(short vendorId, short productId, int sampleRate) throws DeviceException {
		// Read the USB device list
		DeviceList list = new DeviceList();
		int result = LibUsb.getDeviceList(null, list);
		if (result < 0)
			throw new LibUsbException("Unable to get device list", result);

		try {
			// Iterate over all devices and scan for the right one
			for (Device device : list) {
				DeviceDescriptor descriptor = new DeviceDescriptor();

				result = LibUsb.getDeviceDescriptor(device, descriptor);
				if (result != LibUsb.SUCCESS)
					throw new LibUsbException("Unable to read device descriptor", result);
				if (descriptor.idVendor() == vendorId && descriptor.idProduct() == productId) {
					logger.info("Found SDR USB Device");
					logger.info("Vendor ID: " + Integer.toHexString(vendorId) + " Product ID: "
							+ Integer.toHexString(productId) + "\n" + descriptor.dump());
					this.device = device;
					this.deviceDescriptor = descriptor;
					TunerType tunerType = TunerType.UNKNOWN;
					tunerType = RTL2832TunerController.identifyTunerType(device);
					logger.info("Found tuner: " + tunerType);
					rtl = new R820TTunerController(device, descriptor);
					SampleRate rate = SampleRate.getClosest(sampleRate);
					rtl.init(rate); // have to call this after the constructor to initialize shadowRegister
					return rtl;
				}
			}
		} finally {
			// Ensure the allocated device list is freed
			// Note don't free the list before we have opened the device that we want,
			// otherwise it fails
			LibUsb.freeDeviceList(list, true);
		}
		return null;
	}

	private void listNumberOfAvailableDevices() {
		int result = LibUsb.init(null);
		if (result != LibUsb.SUCCESS)
			throw new LibUsbException("Unable to initialize libusb.", result);

		// Read the USB device list
		DeviceList list = new DeviceList();
		result = LibUsb.getDeviceList(null, list);
		if (result < 0)
			throw new LibUsbException("Unable to get device list", result);

		logger.info("Found devices: " + list.getSize());
		// Ensure the allocated device list is freed
		// Note that we need to not free ths list before we have opened the device that
		// we want, otherwise that fails
		LibUsb.freeDeviceList(list, true);
	}

	/**
	 * Bulk interface for RF data:
	 * 
	 * Endpoint Descriptor: bLength 7 bDescriptorType 5 bEndpointAddress 0x81 EP 1
	 * IN bmAttributes 2 Transfer Type Bulk Synch Type None Usage Type Data
	 * wMaxPacketSize 512 bInterval 0 extralen 0 extra:
	 */
	private void listDeviceConfiguration(Device device) {
		ConfigDescriptor config = new ConfigDescriptor();
		int result = LibUsb.getConfigDescriptor(device, (byte) 0, config);
		if (result != LibUsb.SUCCESS)
			throw new LibUsbException("Unable to read config descriptor", result);
		logger.info("\n" + config.dump());
	}
}