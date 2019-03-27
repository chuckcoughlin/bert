/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package btj;

import java.util.List;

/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
public class BluetoothDevice {
    public void close() {
    	
    }
    
    /**
     * Address is the MAC address.
     * @return address as a String.
     */
    public String getAddress() {
    	return "00:AA:BB:CC:DD:EE:FF";
    }
    
    public String getName() {
    	return "name";
    }
	public List<BluetoothService>getServices() {
		return null;
	}
}
