/*
 *  Inspired by: Luu Gia Thuy - http://luugiathuy.com/2011/02/android-java-bluetooth/
 */
package bluecove.bluetooth;

import bluecove.core.BluetoothStackBlueZ;
import bluecove.core.WaitThread;

/**
 * This is where we start bluecove. Implement the manager as a singleton.
 * Note that the "stack" object loads the JNI interface library. 
 *
 */
public class BluetoothManager {
	private static BluetoothManager instance;
	private BluetoothStack stack;
	
    /**
     * Make the constructor private per the Singleton pattern.
     */
    private BluetoothManager() {
    	this.stack = new BluetoothStackBlueZ();
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

	public BluetoothStack getStack() { return this.stack; }
	
	public void start() {
		Thread waitThread = new Thread(new WaitThread());
        waitThread.start();
	}
	
	
	/**
	 * Entry point for testing.
	 * @param args
	 */
	public static void main(String[] args) {
        BluetoothManager mgr = BluetoothManager.getInstance();
        mgr.start();
    }
}
