package jbluez;


public class Main {
	private static final String USAGE = "usage: jbluez";
	
	public Main() {
        try {
            System.loadLibrary("bluetoothjni");
        } 
        catch (UnsatisfiedLinkError e) {
            System.err.println("Native code library failed to load.\n" + e);
        }
	}

	/**
	 * Entry point for hand-crafted bluetooth application "jbluez". 
	 * 
	 */
	public static void main(String[] args) {
			
		// Make sure there is command-line argument
		if( args.length < 1) {
			System.out.println(USAGE);
			System.exit(1);
		}
		Main runner = new Main();
	}
}
