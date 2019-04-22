/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package jbluez;

import java.util.List;
import java.util.UUID;


public class BluetoothService {
	private final UUID uuid = UUID.randomUUID();
	
    public void close() {
    	
    }
   
	public List<BluetoothCharacteristic>getCharacteristics() {
		return null;
	}
	
    public String getUUID() { return this.uuid.toString(); } 
}
