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
 * Data object to represent the stats for local devices. It is used as a field in HCIDeviceInfo. This class is essentially a Java representation of the <code>hci_dev_stats</code> struct in the <code>hci.h</code> file provided in the BlueZ libraries.
 * 
 * @author Edward Kay, ed.kay@appliancestudio.com
 * @see HCIDeviceInfo
 * @version 1.0
 */
public class HCIDeviceStats 
{
	public long err_rx;
	public long err_tx;
	public long cmd_tx;
	public long evt_rx;
	public long acl_tx;
	public long acl_rx;
	public long sco_tx;
	public long sco_rx;
	public long byte_rx;
	public long byte_tx;

	/**
	 * Default constructor.
	 */
	public HCIDeviceStats()
	{
	}

	/**
	 * Constructor which sets all the fields in the class. This is provided since when creating a new HCIDeviceStats object in the native code (namely C), it is much more efficient since we do not have to ask the JVM for pointers to each individual field.
	 * 
	 * @param _err_rx 
	 * @param _err_tx 
	 * @param _cmd_tx 
	 * @param _evt_rx 
	 * @param _acl_tx 
	 * @param _acl_rx 
	 * @param _sco_tx 
	 * @param _sco_rx 
	 * @param _byte_rx 
	 * @param _byte_tx 
	 */
	public HCIDeviceStats(long _err_rx,
						  long _err_tx,
						  long _cmd_tx,
						  long _evt_rx,
						  long _acl_tx,
						  long _acl_rx,
						  long _sco_tx,
						  long _sco_rx,
						  long _byte_rx,
						  long _byte_tx)
	{
		this.err_rx = _err_rx;
		this.err_tx = _err_tx;
		this.cmd_tx = _cmd_tx;
		this.evt_rx = _evt_rx;
		this.acl_tx = _acl_tx;
		this.acl_rx = _acl_rx;
		this.sco_tx = _sco_tx;
		this.sco_rx = _sco_rx;
		this.byte_rx = _byte_rx;
		this.byte_tx = _byte_tx;
	}

	/**
	 * Return all the field values as a String for printing.
	 * 
	 * @return A String representation of the data in all the fields.
	 */
	public String toString()
	{
		// Return all the info in a nice string for printing
		StringBuffer buf = new StringBuffer();

		buf.append("   Errors RX:   " + err_rx + "\n");
		buf.append("   Errors TX:   " + err_tx + "\n");
		buf.append("   Commands TX: " + cmd_tx + "\n");
		buf.append("   Events RX:   " + evt_rx + "\n");
		buf.append("   ACL TX:      " + acl_tx + "\n");
		buf.append("   ACL RX:      " + acl_rx + "\n");
		buf.append("   SCO TX:      " + sco_tx + "\n");
		buf.append("   SCO RX:      " + sco_rx + "\n");
		buf.append("   Bytes RX:    " + byte_rx + "\n");
		buf.append("   Bytes TX:    " + byte_tx);

		return buf.toString();
	}
}
