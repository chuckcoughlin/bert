/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

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
	var side: String


	fun updateFromLink(link:Link) {
		name = link.name
		side = link.side.name
	}
	fun updateSource(pos:Point3D) {
		source = pos
	}
	fun updateEnd(pos:Point3D) {
		end = pos
	}
	init {
		name = ""
		source = Point3D(0.0,0.0,0.0)
		end    = Point3D(0.0,0.0,0.0)
		joint = Joint.NONE.name
		appendage = Appendage.NONE.name
		side = Side.FRONT.name
	}
}