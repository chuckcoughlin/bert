/*
 *  From: Luu Gia Thuy - http://luugiathuy.com/2011/02/android-java-bluetooth/
 */
package bluecove;

public class BluetoothServer {
	public static void main(String[] args) {
        Thread waitThread = new Thread(new WaitThread());
        waitThread.start();
    }
}
