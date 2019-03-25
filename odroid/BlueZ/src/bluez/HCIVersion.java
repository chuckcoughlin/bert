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

package bluez;

/**
 * Data object to represent the version information of a device.
 * 
 * This class is essentially a Java representation of the <code>hci_version</code> struct in <code>hci_lib.h</code>, provided by the BlueZ libraries.
 * 
 * The information is represented here in numeric form, as is collected directly from the HCI interface. There are standard mappings between these to "human readable" strings, but these have yet to be implemented. For more information, see the Bluetooth Specification or the source code of the BlueZ libraries. (For example, to translate the manufacturer number to the associated company name, see the <code>bt_compidtostr</code> function in <code>bluetooth.c</code>.
 * 
 * @author Edward Kay, ed.kay@appliancestudio.com
 * @version 1.0
 */
public class HCIVersion
{
	/**
	 * Numeric representation of the manufacturer of the HCI device. To convert to a string, see the Bluetooth Specification or the <code>bt_compidtostr</code> function in <code>bluetooth.c</code> (part of the BlueZ libraries).
	 */
	public int   manufacturer;
	/**
	 * HCI version.
	 */
	public short hci_ver;
	/**
	 * HCI revision.
	 */
	public int   hci_rev;
	/**
	 * Link Manager Protocol version.
	 */
	public short lmp_ver;
	/**
	 * Link Manager Protocol sub-version.
	 */
	public int   lmp_subver;

	/**
	 * Default constructor.
	 */
	public HCIVersion()
	{
	}

	/**
	 * Constructor which sets all the fields in the class. This is provided since when creating a new HCIVersion object in the native code (namely C), it is much more efficient since we do not have to ask the JVM for pointers to each individual field.
	 * 
	 * @param _manufacturer 
	 * @param _hci_ver 
	 * @param _hci_rev 
	 * @param _lmp_ver 
	 * @param _lmp_subver 
	 */
	public HCIVersion(int _manufacturer,
					  short _hci_ver,
					  int _hci_rev,
					  short _lmp_ver,
					  int _lmp_subver)
	{
		this.manufacturer = _manufacturer;
		this.hci_ver = _hci_ver;
		this.hci_rev = _hci_rev;
		this.lmp_ver = _lmp_ver;
		this.lmp_subver = _lmp_subver;
	}

	/**
	 * Returns a String representation of all the information represented by this object.
	 * 
	 * @return A String representation of all the information represented by this
	 *      object.
	 */
	public String toString()
	{
		// Return all the info in a nice string for printing
		StringBuffer buf = new StringBuffer();

		buf.append("Manufacturer:    " + manufacturer + "\n");
		buf.append("HCI Version:     " + hci_ver + "\n");
		buf.append("HCI Revision:    " + hci_rev + "\n");
		buf.append("LMP Version:     " + lmp_ver + "\n");
		buf.append("LMP Sub-version: " + lmp_subver);

		return buf.toString();
	}
}
