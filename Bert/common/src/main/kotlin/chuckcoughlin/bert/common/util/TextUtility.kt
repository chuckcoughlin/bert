/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.util

/**
 * Static helper functions for formatting spoken text strings
 */
object TextUtility {
	/**
	 * Convert a list of strings into a comma and, finally, "and"
	 * separated list.
	 */
	fun createTextForSpeakingFromList(elements:List<String> ) : String {
		val buf = StringBuffer()
		var count = 0
		val size = elements.size
		for(element in elements) {
			buf.append(element)
			if( size>1 ) {
				if (count < elements.size - 2) buf.append(", ")
				else if (count == elements.size - 2) buf.append(" and ")
				count = count + 1
			}
		}
		return buf.toString()
	}

	/** Remove any internal newlines, but and add one to the end */
	fun stripNewLines(text:String) : String {
		val buf = StringBuffer()
		var count = 0
		val size = text.length
		while(count<size) {
			val c = text[count]
			count++
			if( c=='\n' || c=='\r') continue
			buf.append(c)
		}
		buf.append('\n')
		return buf.toString()
	}
}