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
 *  @author vlads
 *  @version $Id: BluetoothL2CAPClientConnection.java 2476 2008-12-01 17:41:59Z skarzhevskyy $
 */
package bc.bluetooth;

import java.io.IOException;
import java.util.logging.Logger;

import bluecove.bluetooth.BluetoothStack;

/**
 *
 */
public class BluetoothL2CAPClientConnection extends BluetoothL2CAPConnection {
	private final static String CLSS = "BluetoothL2CAPClientConnection";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	
	public BluetoothL2CAPClientConnection(BluetoothStack bluetoothStack, BluetoothConnectionParams params,
			int receiveMTU, int transmitMTU) throws IOException {
		super(bluetoothStack, bluetoothStack.l2OpenClientConnection(params, receiveMTU, transmitMTU));
		boolean initOK = false;
		try {
			this.securityOpt = bluetoothStack.l2GetSecurityOpt(this.handle, Utils.securityOpt(params.authenticate,
					params.encrypt));
			RemoteDeviceHelper.connected(this);
			initOK = true;
		} finally {
			if (!initOK) {
				try {
					bluetoothStack.l2CloseClientConnection(this.handle);
				} catch (IOException ioe) {
					LOGGER.info(String.format("%s: ERROR closing %s", CLSS,ioe.getLocalizedMessage()));
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.intel.bluetooth.BluetoothL2CAPConnection#closeConnectionHandle(long)
	 */
	public void closeConnectionHandle(long handle) throws IOException {
		RemoteDeviceHelper.disconnected(this);
		bluetoothStack.l2CloseClientConnection(handle);
	}

}