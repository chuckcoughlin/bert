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
		 * If unknown return a red-filled circle. Resolute links are "bones".
		 */
		fun drawableForLink(link: LinkLocation,projection:Side): LinkShapeDrawable {
			val drawable: LinkShapeDrawable
			val p1 = projectedPoint(link.source,projection)
			val p2 = projectedPoint(link.end,projection)
			val side = Side.fromString(link.side)
			if(!link.joint.equals("NONE", true)) {
				drawable = BoneDrawable(p1,p2,side)
			}
			else if(link.appendage.equals("NOSE", true)) {
				drawable = NoseDrawable(p1,p2,side)
			}
			else if(link.appendage.contains("FINGER", true)) {
				drawable = HandDrawable(p1,p2,side)
			}
			else if(!link.appendage.equals("NONE", true)) {
				drawable = AppendageDrawable(p1,p2,side)
			}
			else {
				drawable = UnknownDrawable(p1,p2,side)
			}
			return drawable
		}

		fun projectedPoint(loc: Point3D,projection:Side): Point2D {
			var pos: Point2D = when(projection) {
				Side.FRONT-> {Point2D(loc.y,-loc.z)}
				Side.BACK -> {Point2D(-loc.y,-loc.z)}
				Side.LEFT-> {Point2D(loc.x,-loc.z)}
				Side.RIGHT-> {Point2D(-loc.x,-loc.z)}
			}
			return pos
		}
	}


	init {

	}
}
