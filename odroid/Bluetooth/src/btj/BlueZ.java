// BlueZ library access (via JNI)

/*
   Copyright (c) 2002 The Appliance Studio Limited.
   Written by Edward Kay <ed.kay@appliancestudio.com>
   http://www.appliancestudio.com

   This program is free software; you can redistribute it and/or modify it under
   the terms of the GNU General Public License version 2 as published by the
   Free Software Foundation.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF THIRD PARTY RIGHTS.

   IN NO EVENT SHALL THE COPYRIGHT HOLDER(S) AND AUTHOR(S) BE LIABLE FOR ANY
   CLAIM, OR ANY SPECIAL INDIRECT OR CONSEQUENTIAL DAMAGES, OR ANY DAMAGES
   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION
   OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
   CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

   ALL LIABILITY, INCLUDING LIABILITY FOR INFRINGEMENT OF ANY PATENTS,
   COPYRIGHTS, TRADEMARKS OR OTHER RIGHTS, RELATING TO USE OF THIS SOFTWARE IS
   DISCLAIMED.

   $Id:$
*/

package btj;

/**
 * This class provides the methods to access the underlying BlueZ functions.
 * All of the native methods defined in this class are implemented, via the
 * Java Native Interface (JNI) in C. See the btj.c file and associated
 * comments for full details.
 * 
 * If you wish to incorporate Bluetooth functionality (using the BlueZ stack)
 * into your Java application, create a new instance of this BlueZ class.
 * Calling the methods provided here will call the functions defined by the
 * BlueZ libraries, primarily those defined in the files <code>hci.c</code>
 * and <code>hci_lib.h</code>.
 * 
 * @author Edward Kay, ed.kay@appliancestudio.com
 * @version 1.0
 */
public class BlueZ 
{

	/* HCI Open Device */
	/**
	 * Opens the HCI device.
	 * 
	 * @param hciDevID The local HCI device ID (see the hciconfig tool provided  
	 *     by BlueZ for further information).
	 * @exception BlueZException Unable to open the HCI device.
	 * @return A device descriptor (often named <code>dd</code>) for the HCI   
	 *     device.
	 */
	public native int hciOpenDevice(int hciDevID) throws BlueZException;

	/* HCI Close Device */
	/**
	 * Close the HCI device.
	 * 
	 * @param dd The HCI device descriptor (as returned from 
	 *     <code>hciOpenDevice</code>)
	 */
	public native void hciCloseDevice(int dd);

	/* HCI Create Connection */
	/**
	 * Create an HCI connection. Note that this requires higher than normal
	 * privileges, and so will often only work when executed as root. If you
	 * call this method with insufficient privileges, an exception will be
	 * thrown with the message text "Unable to create connection".
	 * 
	 * See HCI_Create_Connection in the Bluetooth Specification for further
	 * details of the various arguments.
	 * 
	 * @param dd HCI device descriptor.
	 * @param bdaddr Bluetooth address String in the form   
	 *     <code>"00:12:34:56:78:9A"</code>.
	 * @param ptype Packet type, for example 0x0008 for DM1.
	 * @param clkoffset Clock offset (usually set to 0).
	 * @param rswitch Role switch (usually set to 0 or 1).
	 * @param timeOut Timeout, in milliseconds.
	 * @exception BlueZException Unable to create the connection.
	 * @return A handle for the connection.
	 */
	public native int hciCreateConnection(int dd, String bdaddr, int ptype, int clkoffset, short rswitch, int timeOut) throws BlueZException;
	/**
	 * Create an HCI connection. Note that this requires higher than normal
	 * privileges, and so will often only work when executed as root. If you
	 * call this method with insufficient privileges, an exception will be
	 * thrown with the message text "Unable to create connection".
	 * 
	 * See HCI_Create_Connection in the Bluetooth Specification for further
	 * details of the various arguments.
	 * 
	 * @param dd HCI device descriptor.
	 * @param bdaddr Bluetooth address as a BTAddress object.
	 * @param ptype Packet type, for example 0x0008 for DM1.
	 * @param clkoffset Clock offset (usually set to 0).
	 * @param rswitch Role switch (usually set to 0 or 1).
	 * @param timeOut Timeout, in milliseconds.
	 * @exception BlueZException Unable to create the connection.
	 * @return A handle for the connection.
	 */
	public int hciCreateConnection(int dd, BTAddress bdaddr, int ptype, int clkoffset, short rswitch, int timeOut) throws BlueZException
	{	return this.hciCreateConnection(dd, bdaddr.toString(), ptype, clkoffset, rswitch, timeOut);	}

	/* HCI Disconnect */
	/**
	 * Disconnect an established HCI connection.
	 * 
	 * See HCI_Disconnect in the Bluetooth Specification for further details
	 * of the various arguments.
	 * 
	 * @param dd HCI device descriptor.
	 * @param handle HCI connection handle.
	 * @param reason Code detailing reason for disconnection (often 0x13).
	 * @param timeOut Timeout, in milliseconds.
	 * @exception BlueZException Unable to disconnect.
	 */
	public native void hciDisconnect(int dd, int handle, short reason, int timeOut) throws BlueZException;

	/* HCI Inquiry */
	/**
	 * Perform an HCI inquiry to discover remote Bluetooth devices.
	 * 
	 * See HCI_Inquiry in the Bluetooth Specification for further details of
	 * the various arguments.
	 * 
	 * @exception BlueZException If the inquiry failed.
	 * @param hciDevID The local HCI device ID (see the hciconfig tool provided 
	 *     by BlueZ for further information).
	 * @param len Maximum amount of time before inquiry is halted. Time = len
	 *      x 1.28 secs.
	 * @param max_num_rsp Maximum number of responses allowed before inquiry 
	 *     is halted. For an unlimited number, set to 0.
	 * @param flags Additional flags. See BlueZ documentation and source code
	 *      for details.
	 * @return An InquiryInfo object containing the results of the inquiry.
	 * @see #hciInquiry(int hciDevID)
	 */
	public native InquiryInfo hciInquiry(int hciDevID, int len, int max_num_rsp, long flags) throws BlueZException;
	/**
	 * Perform an HCI inquiry to discover remote Bluetooth devices. This is the
	 * same as <code>hciInquiry(int hciDevID, int len, int max_num_rsp, long
	 * flags)</code>, except that the <code>len</code>, <code>max_num_rsp</code>
	 * and <code>flags</code> fields are preset to 'default' values. These
	 * values are 8, 10 and 0, respectively.
	 * 
	 * @exception BlueZException If the inquiry failed.
	 * @param hciDevID The local HCI device ID (see the hciconfig tool provided 
	 *     by BlueZ for further information)
	 * @return An InquiryInfo object containing the results of the inquiry.
	 */
	public InquiryInfo hciInquiry(int hciDevID) throws BlueZException
	{	return this.hciInquiry(hciDevID, 8, 10, 0);	}

	/* HCI Device Info */
	/**
	 * Gets the device information for a specified local HCI device.
	 * 
	 * @exception BlueZException If unable to get the device information.
	 * @param hciDevID The local HCI device ID (see the hciconfig tool provided 
	 *     by BlueZ for further information)
	 * @return An HCIDeviceInfo object representing the local HCI device 
	 *     information.
	 */
	public native HCIDeviceInfo hciDevInfo(int hciDevID) throws BlueZException;

	/* HCI Device Bluetooth Address */
	/**
	 * Gets the Bluetooth device address for a specified local HCI device.
	 * 
	 * @exception BlueZException If unable to get the Bluetooth device address.
	 * @param hciDevID The local HCI device ID (see the hciconfig tool provided 
	 *     by BlueZ for further information)
	 * @return A BTAddress object representing the Bluetooth device address.
	 */
	public native BTAddress hciDevBTAddress(int hciDevID) throws BlueZException;

	/* HCI Device ID */
	/**
	 * Gets the device ID for a specified local HCI device.
	 * 
	 * @exception BlueZException If unable to get the device ID.
	 * @param bdaddr Bluetooth address String in the form 
	 *     <code>"00:12:34:56:78:9A"</code>.
	 * @return The device ID for the local device.
	 */
	public native int hciDeviceID(String bdaddr) throws BlueZException;
	/**
	 * Gets the device ID for a specified local HCI device.
	 * 
	 * @exception BlueZException If unable to get the device ID.
	 * @param bdaddr Bluetooth address as a BTAddress object.
	 * @return The device ID for the local device.
	 */
	public int hciDeviceID(BTAddress bdaddr) throws BlueZException
	{	return this.hciDeviceID(bdaddr.toString());	}

	/* HCI Local Name */
	/**
	 * Get the name of a local device. The device must be opened using
	 * <code>hciOpenDevice</code> before calling this method.
	 * 
	 * @exception BlueZException If unable to get the local device name.
	 * @param dd HCI device descriptor.
	 * @param timeOut Timeout, in milliseconds.
	 * @return A String containing the name of the specified local device.
	 */
	public native String hciLocalName(int dd, int timeOut) throws BlueZException;
	/**
	 * Get the name of a local device. The device must be opened using
	 * <code>hciOpenDevice</code> before calling this method. This is the same
	 * as <code>hciLocalName(int dd, int timeOut)</code> with the
	 * <code>timeOut</code> argument set to 10000 (i.e. 10 seconds).
	 * 
	 * @param dd HCI device descriptor.
	 * @exception BlueZException If unable to get the local device name.
	 * @return A String containing the name of the specified local device.
	 */
	public String hciLocalName(int dd) throws BlueZException
	{	return this.hciLocalName(dd, 10000);	}

	/* HCI Remote Name */
	/**
	 * Get the name of a remote device, as specified by its Bluetooth device
	 * address. The local device must be opened using <code>hciOpenDevice</code>
	 * before calling this method.
	 * 
	 * @exception BlueZException If unable to get the remote device name.
	 * @param dd HCI device descriptor.
	 * @param bdaddr Bluetooth address String in the form 
	 *     <code>"00:12:34:56:78:9A"</code>.
	 * @param timeOut Timeout, in milliseconds.
	 * @return A String containing the name of the specified remote device.
	 */
	public native String hciRemoteName(int dd, String bdaddr, int timeOut) throws BlueZException;
	/**
	 * Get the name of a remote device, as specified by its Bluetooth device
	 * address. The local device must be opened using <code>hciOpenDevice</code>
	 * before calling this method. This is the same as
	 * <code>hciRemoteName(int dd, String bdaddr, int timeOut)</code> with the
	 * <code>timeOut</code> argument set to 10000 (i.e. 10 seconds).
	 * 
	 * @param dd HCI device descriptor.
	 * @param bdaddr Bluetooth address String in the form 
	 *     <code>"00:12:34:56:78:9A"</code>.
	 * @exception BlueZException If unable to get the remote device name.
	 * @return A String containing the name of the specified remote device.
	 */
	public String hciRemoteName(int dd, String bdaddr) throws BlueZException
	{	return this.hciRemoteName(dd, bdaddr, 10000);	}
	/**
	 * Get the name of a remote device, as specified by its Bluetooth device
	 * address. The local device must be opened using <code>hciOpenDevice</code>
	 * before calling this method.
	 * 
	 * @param dd HCI device descriptor.
	 * @param bdaddr Bluetooth address as a BTAddress object.
	 * @param timeOut Timeout, in milliseconds.
	 * @exception BlueZException If unable to get the remote device name.
	 * @return A String containing the name of the specified remote device.
	 */
	public String hciRemoteName(int dd, BTAddress bdaddr, int timeOut) throws BlueZException
	{	return this.hciRemoteName(dd, bdaddr.toString(), timeOut);	}
	/**
	 * Get the name of a remote device, as specified by its Bluetooth device
	 * address. The local device must be opened using <code>hciOpenDevice</code>
	 * before calling this method. This is the same as
	 * <code>hciRemoteName(int dd, BTAddress bdaddr, int timeOut)</code> with
	 * the <code>timeOut</code> argument set to 10000 (i.e. 10 seconds).
	 * 
	 * @param dd HCI device descriptor.
	 * @param bdaddr Bluetooth address as a BTAddress object.
	 * @exception BlueZException If unable to get the remote device name.
	 * @return A String containing the name of the specified remote device.
	 */
	public String hciRemoteName(int dd, BTAddress bdaddr) throws BlueZException
	{	return this.hciRemoteName(dd, bdaddr.toString(), 10000);	}

	/* HCI Read Remote Features */
	/**
	 * Get the features of a remote Bluetooth device. In order to call this
	 * method, a local device must have been opened using
	 * <code>hciOpenDevice</code> and an HCI connection created to the remote
	 * device using <code>hciCreateConnection</code>.
	 * 
	 * @exception BlueZException If unable to read the features of the remote 
	 *     device.
	 * @param dd HCI device descriptor.
	 * @param handle HCI connection handle.
	 * @param timeOut Timeout, in milliseconds.
	 * @return The remote features as an <code>HCIFeatures</code> object.
	 */
	public native HCIFeatures hciReadRemoteFeatures(int dd, int handle, int timeOut) throws BlueZException;

	/* HCI Read Remote Version */
	/**
	 * Get the version information of a remote Bluetooth device. In order to
	 * call this method, a local device must have been opened using
	 * <code>hciOpenDevice</code> and an HCI connection created to the remote
	 * device using <code>hciCreateConnection</code>.
	 * 
	 * @exception BlueZException If unable to read the remote version 
	 *     information.
	 * @param dd HCI device descriptor.
	 * @param handle HCI connection handle.
	 * @param timeOut Timeout, in milliseconds.
	 * @return An <code>HCIVersion</code> object representing the remote version 
	 *     information.
	 */
	public native HCIVersion hciReadRemoteVersion(int dd, int handle, int timeOut) throws BlueZException;

	/* HCI Read Local Version */
	/**
	 * Get the version information of a local Bluetooth device. In order to call
	 * this method, the local device must have been opened using
	 * <code>hciOpenDevice</code>.
	 * 
	 * @exception BlueZException If unable to read the features of the local 
	 *     device.
	 * @param dd HCI device descriptor.
	 * @param timeOut Timeout, in milliseconds.
	 * @return An <code>HCIVersion</code> object representing the local version 
	 *     information.
	 */
	public native HCIVersion hciReadLocalVersion(int dd, int timeOut) throws BlueZException;
}
