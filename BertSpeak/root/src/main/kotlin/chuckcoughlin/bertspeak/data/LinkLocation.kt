/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.data

import chuckcoughlin.bertspeak.ui.graphics.Side

/**
 * Start and end of a link in space.
 * The link is identified by its name and
 * end-effector or resolute joint.
 */
class LinkLocation() {
	var name: String
	var source: Point3D
	var end: Point3D
	var joint:String
	var appendage:String
	var side: String  // Link group

	fun locationToText() : String {
		return String.format("%s coordinates: %s",name,end.toText())
	}

	init {
		name = ""
		source = Point3D(0.0,0.0,0.0)
		end    = Point3D(0.0,0.0,0.0)
		joint = "NONE"
		appendage = "NONE"
		side = Side.FRONT.name
	}
}
