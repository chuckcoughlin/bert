/**
 * Copyright 2025-2026. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.ui.graphics

import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bertspeak.common.ConfigurationConstants
import chuckcoughlin.bertspeak.data.JointPosition
import chuckcoughlin.bertspeak.data.JointTree
import chuckcoughlin.bertspeak.data.Point2D
import chuckcoughlin.bertspeak.data.Point3D

/**
 * Create Shape objects appropriate for links
 * and end effectors.
 */
class ShapeFactory () {

	companion object {
		/**
		 * If unknown return a red-filled circle. Resolute links are "bones".
		 */
		fun drawableForLink(jp: JointPosition, projection:Side): LinkShapeDrawable {
			val drawable: LinkShapeDrawable

			val parent = jp.parent
			val p1 = projectedPoint(parent.pos,projection)
			val p2 = projectedPoint(jp.pos,projection)
			val side = Side.fromString(jp.side)
			if(parent.joint == Joint.NONE) {
				drawable = UnknownDrawable(jp.joint,p2,side)
			}
			else if(!Joint.isEndEffector(jp.joint)) {
				drawable = BoneDrawable(jp.joint,p1,p2,side)
				if(jp.joint.name.contains("ANKLE")) drawable.selectable = true
			}
			else  {  // Appendage
				if(jp.joint==Joint.NOSE) {
					drawable = NoseDrawable(jp.joint,p1,p2,side)
				}
				else if(jp.joint.name.contains("FINGER", true)) {
					drawable = HandDrawable(jp.joint, p1, p2, side)
				}
				else if(jp.joint.name.contains("HEEL", true) ||
					    jp.joint.name.contains("TOE", true)) {
					drawable = ToeDrawable(jp.joint,p1,p2,side)
				}
				else  {
					drawable = EndEffectorDrawable(jp.joint,p1,p2,side)
				}
				drawable.selectable = true

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
}
