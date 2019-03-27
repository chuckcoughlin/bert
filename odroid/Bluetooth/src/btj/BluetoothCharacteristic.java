/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
package btj;

/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */
public class BluetoothCharacteristic {
	private boolean notifyListeners = false;
	
    public void close() {
    	
    }
    
    public void disableValueNotifications() { this.notifyListeners=false; }
    public void enableValueNotifications()  { this.notifyListeners=true; }
    
    public byte[] readValue() {
    	return new byte[0];
    }
    
    public void writeValue(String text) {
    	
    }
}
