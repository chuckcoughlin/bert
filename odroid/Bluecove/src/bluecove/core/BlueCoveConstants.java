/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * (MIT License)
 */
package bluecove.core;

/**
 * Constants needed to configure our minimalist BlueCove installation.
 */
public interface BlueCoveConstants {

    /**
	 * Load the shared library, libbluecove.so.
     */
    public static final String LIBRARY_NAME = "bluecove";
    /**
     * The MAC Addresses of the devices we want to connect.
     */
    public static final String CLIENT_MAC = "C0D3C072946A";   // Remote
    public static final String SERVER_MAC = "001A7DDA7113";   // Local - hcitool dev
    public static final String ADAPTER_MAC= "C048E67AD6CC";   // Local - hcitool scan

}
