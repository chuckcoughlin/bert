/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.ui.graphics

import android.graphics.Rect
import chuckcoughlin.bertspeak.data.LinkLocation
import chuckcoughlin.bertspeak.data.Point2D
import chuckcoughlin.bertspeak.data.Point3D
import chuckcoughlin.bertspeak.ui.graphics.Side.FRONT

/**
 * Create Shape objects appropriate for links
 * and appendages.
 */
class ShapeFactory () {

	companion object {
		/**
		 * If unknown return an oval. Resolute links are "bones".
		 */
		fun drawableForLink(link: LinkLocation, cfg: GraphicsConfiguration): LinkShapeDrawable {
			val drawable: LinkShapeDrawable
			val end = projectedLocation(link.end, cfg.scale, cfg.projection)
			val source = projectedLocation(link.source, cfg.scale, cfg.projection)
			if(!link.joint.equals("NONE", true)) {
				drawable = BoneDrawable(source,end,cfg.scale)
			}
			else {
				drawable = UnknownDrawable(end,cfg.scale)
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