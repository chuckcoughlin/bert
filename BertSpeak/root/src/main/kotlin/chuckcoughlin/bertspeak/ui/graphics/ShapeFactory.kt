/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.ui.graphics

import chuckcoughlin.bertspeak.data.LinkLocation
import chuckcoughlin.bertspeak.data.Point2D
import chuckcoughlin.bertspeak.data.Point3D

/**
 * Create Shape objects appropriate for links
 * and appendages.
 */
class ShapeFactory () {

	companion object {
		/**
		 * If unknown return an oval. Resolute links are "bones".
		 */
		fun drawableForLink(link: LinkLocation): LinkShapeDrawable {
			val drawable: LinkShapeDrawable
			if(link.joint.equals("NONE", true)) {
				drawable = BoneDrawable(link)
			}
			else {
				drawable = UnknownDrawable(link)
			}
			return drawable
		}

		fun projectedLocation(loc: Point3D,scale:Double,grp:Side): Point2D {
			val group = grp
			var pos: Point2D = when(group) {
				Side.FRONT-> {Point2D(loc.x*scale,loc.z*scale)}
				Side.BACK -> {Point2D(loc.x*scale,loc.z*scale)}
				Side.LEFT-> {Point2D(-loc.y*scale,loc.z*scale)}
				Side.RIGHT-> {Point2D(loc.y*scale,loc.z*scale)}
			}
			return pos
		}
	}


	init {

	}
}
