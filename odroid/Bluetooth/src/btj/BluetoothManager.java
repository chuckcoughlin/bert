/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 *                 MIT License.
 */

package btj;

import java.util.List;
import java.util.logging.Logger;


/**
 * For now just a stub.
 *
 */
public class BluetoothManager {
	private final static String CLSS = "BluetoothManager";
	private static Logger LOGGER = Logger.getLogger(CLSS);
	
    private static BluetoothManager instance;
    
    static {
        try {
            System.loadLibrary("btj");
        } 
        catch (UnsatisfiedLinkError ule) {
        	LOGGER.warning(String.format("%s.constructor: Failed to load library: libbluebert.so (%s)",CLSS,ule.getLocalizedMessage()));
        }
    }

	/**
	 * Constructor is private per Singleton pattern.
	 */
    private BluetoothManager() {
    }

    /**
	 * Static method to create and/or fetch the single instance.
	 */
	public static BluetoothManager getInstance() {
		if( instance==null) {
			synchronized(BluetoothManager.class) {
				instance = new BluetoothManager();
			}
		}
		return instance;
	}
	
	
	public List<BluetoothDevice>getDevices() {
		return null;
	}


	public boolean startDiscovery() {
		return false;
	}

}
