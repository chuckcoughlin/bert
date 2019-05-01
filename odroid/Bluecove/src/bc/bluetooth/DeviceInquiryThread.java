/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 *  @version $Id: DeviceInquiryThread.java 2496 2008-12-04 20:46:25Z skarzhevskyy $
 */
package bc.bluetooth;

import java.util.logging.Logger;

import javax.bluetooth.BluetoothStateException;

import bc.javax.bluetooth.DiscoveryListener;
import bluecove.bluetooth.BluetoothStack;

/**
 * This is Common class to solve JNI call backs problem
 * 
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 * 
 */
public class DeviceInquiryThread extends Thread {
	private final static String CLSS = "DeviceInquiryThread";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	private BluetoothStack stack;

	private DeviceInquiryRunnable inquiryRunnable;

	private int accessCode;

	private DiscoveryListener listener;

	private BluetoothStateException startException;

	private boolean started = false;

	private boolean terminated = false;

	private Object inquiryStartedEvent = new Object();

	private static int threadNumber;

	private static synchronized int nextThreadNum() {
		return threadNumber++;
	}

	private DeviceInquiryThread(BluetoothStack stack, DeviceInquiryRunnable inquiryRunnable, int accessCode,
			DiscoveryListener listener) {
		super("DeviceInquiryThread-" + nextThreadNum());
		this.stack = stack;
		this.inquiryRunnable = inquiryRunnable;
		this.accessCode = accessCode;
		this.listener = listener;
	}

	/**
	 * Start DeviceInquiry and wait for startException or deviceInquiryStartedCallback
	 */
	public static boolean startInquiry(BluetoothStack stack, DeviceInquiryRunnable inquiryRunnable, int accessCode,
			DiscoveryListener listener) throws BluetoothStateException {
		DeviceInquiryThread t = (new DeviceInquiryThread(stack, inquiryRunnable, accessCode, listener));
		// In case the BTStack hangs, exit JVM anyway
		t.setDaemon(true);
		synchronized (t.inquiryStartedEvent) {
			t.start();
			while (!t.started && !t.terminated) {
				try {
					t.inquiryStartedEvent.wait();
				} catch (InterruptedException e) {
					return false;
				}
				if (t.startException != null) {
					throw t.startException;
				}
			}
		}
		LOGGER.info(String.format("%s.startInquiry: thread started",CLSS));
		return t.started;
	}

	public static int getConfigDeviceInquiryDuration() {
		return BlueCoveConfigProperties.PROPERTY_INQUIRY_DURATION_DEFAULT;
	}

	public void run() {
		int discType = DiscoveryListener.INQUIRY_ERROR;
		try {
			discType = inquiryRunnable.runDeviceInquiry(this, accessCode, listener);
		} catch (BluetoothStateException e) {
			LOGGER.info(String.format("%s.run: caught state exception (%s)",CLSS,e.getLocalizedMessage()));
			startException = e;
		} catch (Throwable e) {
			LOGGER.info(String.format("%s.run: caught exception (%s)",CLSS,e.getLocalizedMessage()));
			// Fine, If Not started then startInquiry return false
		} finally {
			terminated = true;
			synchronized (inquiryStartedEvent) {
				inquiryStartedEvent.notifyAll();
			}
			LOGGER.info(String.format("%s.run: thread complete",CLSS));
			if (started) {
				Utils.j2meUsagePatternDellay();
				listener.inquiryCompleted(discType);
			}
		}
	}

	public void deviceInquiryStartedCallback() {
		LOGGER.info(String.format("%s.deviceInquiryStartedCallback",CLSS));
		started = true;
		synchronized (inquiryStartedEvent) {
			inquiryStartedEvent.notifyAll();
		}
	}

}
