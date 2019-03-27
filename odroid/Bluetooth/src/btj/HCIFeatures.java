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
 * Representation of the features supported by an HCI device.
 * 
 * This is just an array of 8 shorts, which represent the different features. The mapping between the numeric representation and their meaning is not yet implemented, but can be found in the Bluetooth Specification or in the <code>lmp_features_map</code> array and associated functions, found in the <code>hci.c</code> file (part of the BlueZ libraries).
 * 
 * @author Edward Kay, ed.kay@appliancestudio.com
 * @version 1.0
 */
public class HCIFeatures
{
	public short features0;
	public short features1;
	public short features2;
	public short features3;
	public short features4;
	public short features5;
	public short features6;
	public short features7;

	/**
	 * Default constructor.
	 */
	public HCIFeatures()
	{
	}

	/**
	 * Constructor which sets all the fields in the class. This is provided since when creating a new HCIFeatures object in the native code (namely C), it is much more efficient since we do not have to ask the JVM for pointers to each individual field.
	 * 
	 * Each element of the features field is set separately since there is no ability to create arrays of Java types in the native code (?). In any case, if an array was created in the native code and passed back, the only benefit would be slightly more concise code. It would have no effect on performance.
	 * 
	 * @param _features0 
	 * @param _features1 
	 * @param _features2 
	 * @param _features3 
	 * @param _features4 
	 * @param _features5 
	 * @param _features6 
	 * @param _features7 
	 */
	public HCIFeatures(short _features0,
					   short _features1,
					   short _features2,
					   short _features3,
					   short _features4,
					   short _features5,
					   short _features6,
					   short _features7)
	{
		this.features0 = _features0;
		this.features1 = _features1;
		this.features2 = _features2;
		this.features3 = _features3;
		this.features4 = _features4;
		this.features5 = _features5;
		this.features6 = _features6;
		this.features7 = _features7;
	}

	/**
	 * Convert all the data in the array to a String object for printing.
	 * 
	 * @return A String representing the data held in this object.
	 */
	public String toString()
	{
		// Return all the info in a nice string for printing
		StringBuffer buf = new StringBuffer();

		buf.append("   Features 0: " + features0 + "\n");
		buf.append("   Features 1: " + features1 + "\n");
		buf.append("   Features 2: " + features2 + "\n");
		buf.append("   Features 3: " + features3 + "\n");
		buf.append("   Features 4: " + features4 + "\n");
		buf.append("   Features 5: " + features5 + "\n");
		buf.append("   Features 6: " + features6 + "\n");
		buf.append("   Features 7: " + features7);

		return buf.toString();
	}

}
