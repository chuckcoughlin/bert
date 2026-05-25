/**
 * Copyright 2025-2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.ui.graphics

import android.util.Log
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bertspeak.data.JointLink
import chuckcoughlin.bertspeak.data.JointPosition
import chuckcoughlin.bertspeak.data.Point2D
import chuckcoughlin.bertspeak.data.Point3D
import chuckcoughlin.bertspeak.data.Side
import chuckcoughlin.bertspeak.service.DispatchService.Companion.CLSS

/**
 * Create Shape objects appropriate for links
 * and end effectors.
 */
class ShapeFactory () {

	companion object {
		/**
		 * If unknown return a red-filled circle. Resolute links are "bones".
		 */
		fun drawableForLink(jlink: JointLink, jp1 : JointPosition, jp2:JointPosition, projection: Side): LinkShapeDrawable {
			val drawable: LinkShapeDrawable

			val p1 = projectedPoint(jp1.pos,projection)
			val p2 = projectedPoint(jp2.pos,projection)
			val side = Side.fromString(jlink.side)
			if(jp2.joint == Joint.NONE) {
				drawable = UnknownDrawable(jp1.joint,p2,side)
			}
			else if(!Joint.isEndEffector(jp2.joint)) {
				drawable = BoneDrawable(jp1.joint,p1,p2,side)
				if(jp1.joint.name.contains("ANKLE")) drawable.selectable = true
			}
			else  {  // Appendage
				Log.i(CLSS, String.format("drawableForLink end = %s (%02f %02f %02f)",jp2.joint.name,jp2.pos.x,jp2.pos.y,jp2.pos.z))
				if(jp2.joint==Joint.NOSE) {
					drawable = NoseDrawable(jp2.joint,p1,p2,side)
				}
				else if(jp2.joint.name.contains("FINGER", true)) {
					drawable = HandDrawable(jp2.joint, p1, p2, side)
				}
				else if(jp2.joint.name.contains("HEEL", true) ||
					    jp2.joint.name.contains("TOE", true)) {
					drawable = ToeDrawable(jp2.joint,p1,p2,side)
				}
				else  {
					drawable = EndEffectorDrawable(jp1.joint,p1,p2,side)
				}
				drawable.selectable = true

			}
			return drawable
		}

		fun projectedPoint(loc: Point3D,projection: Side): Point2D {
			var pos: Point2D = when(projection) {
				Side.FRONT-> {Point2D(-loc.y,-loc.z)}
				Side.BACK -> {Point2D(loc.y,-loc.z)}
				Side.LEFT-> {Point2D(loc.x,-loc.z)}
				Side.RIGHT-> {Point2D(-loc.x,-loc.z)}
			}
			return pos
		}
		private val CLSS = "ShapeFactory"
	}
}
