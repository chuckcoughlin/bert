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

import java.util.StringTokenizer;

/**
 * Java representation of a Bluetooth device address.
 * 
 * A Bluetooth device address is comprised of six pairs of hex digits, for example <code>00:12:34:56:78:9A</code>. This class is based upon the <code>bdaddr_t</code> type, as defined in <code>bluetooth.h</code> of the BlueZ libraries.
 * 
 * @author Edward Kay, ed.kay@appliancestudio.com
 * @version 1.0
 */
public class BTAddress
{
	/**
	 * The address is stored as six 8-bit numbers.
	 */
	public short[] addr_arr = new short[6];

	/**
	 * Default constructor
	 */
	public BTAddress()
	{
	}

	/**
	 * Creates a <code>BTAddress</code> object from the address <code>addr_str</code>. The address string should be in the form "<code>00:12:34:56:78:9A</code>".
	 * 
	 * @param addr_str <code>String</code> representation of the Bluetooth device
	 *      address.
	 * @exception BTAddressFormatException If the String is not a parsable 
	 *     Bluetooth address.
	 */
	public BTAddress(String addr_str) throws BTAddressFormatException
	{
		this.setValue(addr_str);
	}

	/**
	 * Set the Bluetooth device address. Valid string format is "<code>00:12:34:56:78:AB</code>", that is 6, colon separated pairs of hex digits.
	 * 
	 * @param addr_str <code>String</code> representation of the Bluetooth device
	 *      address.
	 * @exception BTAddressFormatException If the String is not a parsable 
	 *     Bluetooth address.
	 */
	public void setValue(String addr_str) throws BTAddressFormatException
	{
		// Parse the given string into a BTAddress object
		// Valid string format is "00:12:34:56:78:AB"
		// i.e. 6, colon separated pairs of hex digits.
		StringTokenizer st = new StringTokenizer(addr_str, ":");

		if (st.countTokens() != 6)
			throw new BTAddressFormatException("Bad number of tokens");		
		
		int i = 0;
		short s;
		String str = new String();
		while (st.hasMoreTokens())
		{
			str = st.nextToken();
			// Try and parse the (hex) token to a short
			try
			{
				s = Short.parseShort(str, 16); 
			}
			catch (NumberFormatException nfe)
			{
				String msg = "NumberFormatExpection occurred whilst trying to parseShort from token " + str + ".";
				throw new BTAddressFormatException(msg);
			}

			// If successful, set that part of the addr_arr array
			// and get ready for the next token.
			addr_arr[i] = s;
			i++;
		}
		return;
	}

	/**
	 * Returns a String representation of the Bluetooth device address in the form "<code>00:12:34:56:78:9A</code>".
	 * 
	 * @return A String representation of the Bluetooth device address.
	 */
	public String toString()
	{
		// Create a StringBuffer of length 17 since
		// there are six two-hex-digit pairs (12)
		// separated by 5 semi-colons (=17).
		StringBuffer buf = new StringBuffer(17);

		String tmp = new String();
		for (int i=0; i<6; i++)
		{
			tmp = Integer.toHexString(addr_arr[i]).toUpperCase();
			// Pad single digits with a leading 0
			if (addr_arr[i] < 10)
				buf.append("0");
			buf.append(tmp);
			if (i < 5)
				buf.append(":");
		}
		return buf.toString();
	}

	/**
	 * Compares two BTAddress objects to see if they represent the same Bluetooth device address.
	 * 
	 * @param compare The BTAddress object to compare to this.
	 * @return True if the Bluetooth device addresses are equal, otherwise false.
	 */
	public boolean equals(BTAddress compare)
	{
		for (int i=0; i<6; i++)
			if (this.addr_arr[i] != compare.addr_arr[i])
				return false;
		
		return true;
	}
}
