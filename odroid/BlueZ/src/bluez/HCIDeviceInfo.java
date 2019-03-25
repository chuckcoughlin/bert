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
 * This HCIDeviceInfo class is the information relating to a local HCI device.
 * 
 * This class is essentially a Java representation of the <code>hci_dev_info</code> struct in the <code>hci.h</code> file provided in the BlueZ libraries.
 * 
 * The information stored in the various fields is collected directly from the registers in the Bluetooth device. Therefore, some of it, for example the <code>features</code> is not very "human readable". For more information refer to the Bluetooth specification document, with particular reference to the HCI specification.
 * 
 * @author Edward Kay, ed.kay@appliancestudio.com
 * @version 1.0
 */
public class HCIDeviceInfo 
{
	/**
	 * HCI device ID
	 */
	public int dev_id;
	/**
	 * HCI device name
	 */
	public String name;
	/**
	 * Bluetooth device address
	 * 
	 * @see BTAddress
	 */
	public BTAddress bdaddr;
	/**
	 * HCI device flags
	 */
	public long flags;
	/**
	 * HCI device type
	 */
	public short type;
	// Use the Array type since you cannot set Java array
	// elements via JNI
	/**
	 * HCI device features.
	 */
	public HCIFeatures features;
	/**
	 * HCI packet type(s).
	 */
	public long pkt_type;
	/**
	 * HCI link policy.
	 */
	public long link_policy;
	/**
	 * HCI link mode.
	 */
	public long link_mode;
	/**
	 * HCI ACL (asynchronous connection-less) MTU (max transmission unit).
	 */
	public int acl_mtu;
	/**
	 * HCI ACL (asynchronous connection-less) packets transferred.
	 */
	public int acl_pkts;
	/**
	 * HCI SCO (synchronous connection-oriented) MTU (max transmission unit).
	 */
	public int sco_mtu;
	/**
	 * HCI SCO (synchronous connection-oriented) packets transferred.
	 */
	public int sco_pkts;
	/**
	 * HCI device statistics.
	 * 
	 * @see HCIDeviceStats
	 */
	public HCIDeviceStats stat;

	/**
	 * Default constructor.
	 */
	public HCIDeviceInfo()
	{
	}
	
	/**
	 * Constructor which sets all the fields in the class. This is provided since when creating a new HCIDeviceInfo object in the native code (namely C), it is much more efficient since we do not have to ask the JVM for pointers to each individual field.
	 * 
	 * @param _dev_id 
	 * @param _name 
	 * @param _bdaddr 
	 * @param _flags 
	 * @param _type 
	 * @param _features 
	 * @param _pkt_type 
	 * @param _link_policy 
	 * @param _link_mode 
	 * @param _acl_mtu 
	 * @param _acl_pkts 
	 * @param _sco_mtu 
	 * @param _sco_pkts 
	 * @param _stat 
	 */
	public HCIDeviceInfo(int _dev_id,      
						 String _name,
						 BTAddress _bdaddr,
						 long _flags,
						 short _type,
						 HCIFeatures _features,
						 long _pkt_type,
						 long _link_policy,
						 long _link_mode,
						 int _acl_mtu,
						 int _acl_pkts,
						 int _sco_mtu,
						 int _sco_pkts,
						 HCIDeviceStats _stat)
	{
		this.dev_id = _dev_id;
		this.name = _name;
		this.bdaddr = _bdaddr;
		this.flags = _flags;
		this.type = _type;
		this.features = _features;
		this.pkt_type = _pkt_type;
		this.link_policy = _link_policy;
		this.link_mode = _link_mode;
		this.acl_mtu = _acl_mtu;
		this.acl_pkts = _acl_pkts;
		this.sco_mtu = _sco_mtu;
		this.sco_pkts = _sco_pkts;
		this.stat = _stat;

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

		buf.append("Device ID:   " + dev_id + "\n");
		buf.append("Name:        " + name + "\n");
		buf.append("Address:     " + bdaddr.toString() + "\n");
		buf.append("Flags:       " + flags + "\n");
		buf.append("Type:        " + type + "\n");
		buf.append("Features:\n" + features.toString() + "\n");
		buf.append("Packet Type: " + pkt_type + "\n");
		buf.append("Link Policy: " + link_policy + "\n");
		buf.append("Link Mode:   " + link_mode + "\n");
		buf.append("ACL MTU:     " + acl_mtu + "\n");
		buf.append("ACL Packets: " + acl_pkts + "\n");
		buf.append("SCO MTU:     " + sco_mtu + "\n");
		buf.append("SCO Packets: " + sco_pkts + "\n");
		buf.append("Statistics:\n" + stat.toString());

		return buf.toString();
	}
}
