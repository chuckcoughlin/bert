/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
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
 *  @version $Id: SimpleServer.java 2471 2008-12-01 03:44:20Z skarzhevskyy $
 */ 
package test.bluecove;

import java.io.DataInputStream;
import java.io.IOException;

import bluecove.javax.bluetooth.BluetoothStateException;
import bluecove.javax.bluetooth.DiscoveryAgent;
import bluecove.javax.bluetooth.LocalDevice;
import bluecove.javax.bluetooth.UUID;
import bluecove.javax.microedition.io.Connector;
import bluecove.javax.microedition.io.StreamConnection;
import bluecove.javax.microedition.io.StreamConnectionNotifier;

public class SimpleServer {
	
	public static final UUID uuid = new UUID(Consts.TEST_UUID, false);

	public SimpleServer(String name) throws BluetoothStateException {
		
		LocalDevice localDevice = LocalDevice.getLocalDevice();
		System.out.println("Local bt address " + localDevice.getBluetoothAddress());
 	    System.out.println("Local bt name    " + localDevice.getFriendlyName());
 	    
 	    localDevice.setDiscoverable(DiscoveryAgent.GIAC);
 	   
		int connectionsCount = 0;
		
		while (run(name) && connectionsCount < 10) {
			connectionsCount ++;
		}
		
		System.exit(0);
	}
	
	public boolean run(String name) {
		try {
			StreamConnectionNotifier server = (StreamConnectionNotifier) Connector
					.open("btspp://localhost:"
							+ uuid
							+ ";name="
							+ name
							+ ";authorize=false;authenticate=false;encrypt=false");

			System.out.println("Server started " + name);
			
			StreamConnection conn = server.acceptAndOpen();

			System.out.println("Server received connection");
			
			DataInputStream dis = new DataInputStream(conn.openInputStream());

			System.out.print("Got message[");
			System.out.print(dis.readUTF());
			System.out.println("]");
			
			dis.close();

			conn.close();

			server.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void main(String[] args) {
		try {
			if (args.length == 1)
				new SimpleServer(Consts.TEST_SERVERNAME_PREFIX + args[0]);
			else {
				System.out.println("syntax: SimpleServer <service name>");
				new SimpleServer(Consts.TEST_SERVERNAME_PREFIX + "1");
			}
		} catch (Throwable e) {
			System.err.println("initialization error"+e.getLocalizedMessage());
		}
	}
}