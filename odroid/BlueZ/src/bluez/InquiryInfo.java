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

import java.util.*;

/**
 * Stores the results of an HCI inquiry. Each result is an InquiryInfoDevice object.
 * 
 * @author Edward Kay, ed.kay@appliancestudio.com
 * @version 1.0
 */
public class InquiryInfo 
{
	/**
	 * The number of devices that responded.
	 */
	public byte num_responses;
	/**
	 * The details of each device which responded to the enquiry. These are represented as InquiryInfoDevice objects.
	 */
	private Vector devices = new Vector();

	/**
	 * Default constructor.
	 */
	public InquiryInfo()
	{
	}
	
	/**
	 * Constructor.
	 * 
	 * @param _num_responses The number of responses.
	 */
	public InquiryInfo(byte _num_responses)
	{
		this.num_responses = _num_responses;
	}

	/**
	 * Adds an InquiryInfoDevice object to the Vector of devices.
	 * 
	 * @param dev An InquiryInfoDevice object.
	 */
	public void addDevice(InquiryInfoDevice dev)
	{
		devices.addElement(dev);
		return;
	}

	/**
	 * Returns the Vector representation of the InquiryInfoDevice objects.
	 * 
	 * @return Devices found.
	 */
	public Vector devices()
	{
		return devices;
	}
}
