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
 *  @version $Id: SearchServicesThread.java 2556 2008-12-11 08:06:04Z skarzhevskyy $
 */
package bc.bluetooth;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;

import javax.bluetooth.BluetoothStateException;

import bc.javax.bluetooth.DiscoveryListener;
import bc.javax.bluetooth.RemoteDevice;
import bc.javax.bluetooth.ServiceRecord;
import bc.javax.bluetooth.UUID;
import bluecove.bluetooth.BluetoothStack;



/**
 * This is Common class to solve JNI call backs problem
 *
 * <p>
 * <b><u>Your application should not use this class directly.</u></b>
 *
 */
public class SearchServicesThread extends Thread {
	private final static String CLSS = "SearchServicesThread";
	private static Logger LOGGER = Logger.getLogger(CLSS);

	private static int transIDGenerator = 0;
	private static Hashtable threads = new Hashtable();
	private BluetoothStack stack;
	private SearchServicesRunnable searchRunnable;
	private int transID;
	private int[] attrSet;
	private Vector servicesRecords = new Vector();

	UUID[] uuidSet;

	private RemoteDevice device;
	private DiscoveryListener listener;
	private BluetoothStateException startException;
	private boolean started = false;
	private boolean finished = false;
	private boolean terminated = false;
	private Object serviceSearchStartedEvent = new Object();

	private static synchronized int nextThreadNum() {
		return ++transIDGenerator;
	}

	private SearchServicesThread(int transID, BluetoothStack stack, SearchServicesRunnable serachRunnable,
			int[] attrSet, UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) {
		super("SearchServicesThread-" + transID);
		this.stack = stack;
		this.searchRunnable = serachRunnable;
		this.transID = transID;
		this.attrSet = attrSet;
		this.listener = listener;
		this.uuidSet = uuidSet;
		this.device = device;
	}

	/**
	 * Start Services Search and wait for startException or
	 * searchServicesStartedCallback
	 */
	public static int startSearchServices(BluetoothStack stack, SearchServicesRunnable searchRunnable, int[] attrSet,
			UUID[] uuidSet, RemoteDevice device, DiscoveryListener listener) throws BluetoothStateException {
		SearchServicesThread t;
		synchronized (threads) {
			int runningCount = countRunningSearchServicesThreads(stack);
			int concurrentAllow = Integer.valueOf(stack.getLocalDeviceProperty(BluetoothConsts.PROPERTY_BLUETOOTH_SD_TRANS_MAX)).intValue();
			if (runningCount >= concurrentAllow) {
				throw new BluetoothStateException("Already running " + runningCount + " service discovery transactions");
			}
			t = (new SearchServicesThread(nextThreadNum(), stack, searchRunnable, attrSet, uuidSet, device, listener));
			threads.put(new Integer(t.getTransID()), t);
		}
		// In case the BTStack hangs, exit JVM anyway
		t.setDaemon(true);
		synchronized (t.serviceSearchStartedEvent) {
			t.start();
			while (!t.started && !t.finished) {
				try {
					t.serviceSearchStartedEvent.wait();
				} catch (InterruptedException e) {
					return 0;
				}
				if (t.startException != null) {
					throw t.startException;
				}
			}
		}
		if (t.started) {
			return t.getTransID();
		} else {
			// This is arguable according to JSR-82 we can probably return 0...
			throw new BluetoothStateException();
		}
	}

	private static int countRunningSearchServicesThreads(BluetoothStack stack) {
		int count = 0;
		for (Enumeration en = threads.elements(); en.hasMoreElements();) {
			SearchServicesThread t = (SearchServicesThread) en.nextElement();
			if (t.stack == stack) {
				count++;
			}
		}
		return count;
	}

	public static SearchServicesThread getServiceSearchThread(int transID) {
		return (SearchServicesThread) threads.get(transID);
	}

	public void run() {
		int respCode = DiscoveryListener.SERVICE_SEARCH_ERROR;
		try {
			//BlueCoveImpl.setThreadBluetoothStack(stack);
			respCode = searchRunnable.runSearchServices(this, attrSet, uuidSet, device, listener);
		} catch (BluetoothStateException e) {
			startException = e;
			return;
		} finally {
			finished = true;
			unregisterThread();
			synchronized (serviceSearchStartedEvent) {
				serviceSearchStartedEvent.notifyAll();
			}
			LOGGER.info(String.format("%s.run: runSearchServices ends (%d)",CLSS, getTransID()));
			if (started) {
				Utils.j2meUsagePatternDellay();
				listener.serviceSearchCompleted(getTransID(), respCode);
			}
		}
	}

	private void unregisterThread() {
		synchronized (threads) {
			threads.remove(new Integer(getTransID()));
		}
	}

	public void searchServicesStartedCallback() {
		LOGGER.info(String.format("%s.searchServicesStartedCallback: ID = %d",CLSS, getTransID()));
		started = true;
		synchronized (serviceSearchStartedEvent) {
			serviceSearchStartedEvent.notifyAll();
		}
	}

	public int getTransID() {
		return this.transID;
	}

	public boolean setTerminated() {
		if (isTerminated()) {
			return false;
		}
		terminated = true;
		unregisterThread();
		return true;
	}

	public boolean isTerminated() {
		return terminated;
	}

	public RemoteDevice getDevice() {
		return this.device;
	}

	public DiscoveryListener getListener() {
		return this.listener;
	}

	public void addServicesRecords(ServiceRecord servRecord) {
		this.servicesRecords.addElement(servRecord);
	}

	public Vector getServicesRecords() {
		return this.servicesRecords;
	}

	public int[] getAttrSet() {
		final int[] requiredAttrIDs = new int[] { BluetoothConsts.ServiceRecordHandle,
				BluetoothConsts.ServiceClassIDList, BluetoothConsts.ServiceRecordState, BluetoothConsts.ServiceID,
				BluetoothConsts.ProtocolDescriptorList };
		if (this.attrSet == null) {
			return requiredAttrIDs;
		}
		// Append unique attributes from attrSet
		int len = requiredAttrIDs.length + this.attrSet.length;
		for (int i = 0; i < this.attrSet.length; i++) {
			for (int k = 0; k < requiredAttrIDs.length; k++) {
				if (requiredAttrIDs[k] == this.attrSet[i]) {
					len--;
					break;
				}
			}
		}

		int[] allIDs = new int[len];
		System.arraycopy(requiredAttrIDs, 0, allIDs, 0, requiredAttrIDs.length);
		int appendPosition = requiredAttrIDs.length;
		nextAttribute: for (int i = 0; i < this.attrSet.length; i++) {
			for (int k = 0; k < requiredAttrIDs.length; k++) {
				if (requiredAttrIDs[k] == this.attrSet[i]) {
					continue nextAttribute;
				}
			}
			allIDs[appendPosition] = this.attrSet[i];
			appendPosition++;
		}
		return allIDs;
	}

}
