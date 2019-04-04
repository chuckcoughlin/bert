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

package jbluez;

/**
 * Represents the information gather about a remote device during an HCI Inquiry.
 * 
 * Many devices may be found for an inquiry. The <code>InquiryInfoDevice</code> class is the information relating to a single device. All the discovered devices are collectively kept in the <code>InquiryInfo</code> class.
 * 
 * @author Edward Kay, ed.kay@appliancestudio.com
 * @see InquiryInfo
 * @version 1.0
 */
public class InquiryInfoDevice 
{
	public BTAddress bdaddr = new BTAddress();
	public short pscan_rep_mode;
	public short pscan_period_mode;
	public short pscan_mode;

	// An array can't be used for dev_class since you
	// cannot set array fields via JNI
	public short dev_class0;
	public short dev_class1;
	public short dev_class2;
	public int clock_offset;

	/**
	 * Default constructor.
	 */
	public InquiryInfoDevice()
	{
	}

	/**
	 * Constructor which sets all the fields in the class. This is provided since when creating a new InquiryInfoDevice object in the native code (namely C), it is much more efficient since we do not have to ask the JVM for pointers to each individual field.
	 * 
	 * @param _bdaddr 
	 * @param _pscan_rep_mode 
	 * @param _pscan_period_mode 
	 * @param _pscan_mode 
	 * @param _dev_class0 
	 * @param _dev_class1 
	 * @param _dev_class2 
	 * @param _clock_offset 
	 */
	public InquiryInfoDevice(BTAddress _bdaddr,
							 short _pscan_rep_mode,
							 short _pscan_period_mode,
							 short _pscan_mode,
							 short _dev_class0,
							 short _dev_class1,
							 short _dev_class2,
							 int _clock_offset)
	{
		this.bdaddr = _bdaddr;
		this.pscan_rep_mode = _pscan_rep_mode;
		this.pscan_period_mode = _pscan_period_mode;
		this.pscan_mode = _pscan_mode;
		this.dev_class0 = _dev_class0;
		this.dev_class1 = _dev_class1;
		this.dev_class2 = _dev_class2;
		this.clock_offset = _clock_offset;
	}

	/**
	 * Returns a String representation of all the data held in this object. 
	 * 
	 * @return A String representation of all the information represented by this
	 *      object.
	 */
	public String toString()
	{
		// Return all the info in a nice string for printing
		StringBuffer buf = new StringBuffer();

		buf.append("Address:                 " + bdaddr.toString() + "\n");
		buf.append("Page Scan Response Mode: " + pscan_rep_mode + "\n");
		buf.append("Page Scan Period Mode:   " + pscan_period_mode + "\n");
		buf.append("Page Scan Mode:          " + pscan_mode + "\n");
		buf.append("Device Class:            " + dev_class0 + ", " + dev_class1 + ", " + dev_class2 + "\n");
		buf.append("Clock Offset:            " + clock_offset);

		return buf.toString();
	}

	/**
	 * Compares two InquiryInfoDevice objects to see if they represent the same Bluetooth device. This is done by calling the <code>equals</code> method on the <code>bdaddr</code> field, hence seeing if they have the same Bluetooth device address.
	 * 
	 * @param dev The InquiryInfoDevice object for comparison.
	 * @return True if the two InquiryInfoDevice objects represent the same 
	 *     Bluetooth device, otherwise false.
	 */
	public boolean equals(InquiryInfoDevice dev)
	{	
		return this.bdaddr.equals(dev.bdaddr);
	}
}
