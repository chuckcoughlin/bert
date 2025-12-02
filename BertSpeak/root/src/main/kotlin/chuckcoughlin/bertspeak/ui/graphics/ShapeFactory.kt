/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.ui.graphics

import chuckcoughlin.bertspeak.common.ConfigurationConstants
import chuckcoughlin.bertspeak.data.JointPosition
import chuckcoughlin.bertspeak.data.JointTree
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
		fun drawableForLink(tree: JointTree, jp: JointPosition, projection:Side): LinkShapeDrawable {
			val drawable: LinkShapeDrawable

			val parent = tree.getParent(jp)
			val p1 = projectedPoint(parent.pos,projection)
			val p2 = projectedPoint(jp.pos,projection)
			val side = Side.fromString(jp.side)
			if(jp.parent == ConfigurationConstants.NO_ID ) {
				drawable = UnknownDrawable(jp.name,p2,side)
			}
			else if(!jp.isAppendage) {
				drawable = BoneDrawable(jp.name,p1,p2,side)
				if(jp.name.contains("ANKLE")) drawable.selectable = true
			}
			else  {  // Appendage
				if(jp.name.equals("NOSE", true)) {
					drawable = NoseDrawable(jp.name,p1,p2,side)
				}
				else if(jp.name.contains("FINGER", true)) {
					drawable = HandDrawable(jp.name, p1, p2, side)
				}
				else if(jp.name.contains("HEEL", true) ||
					    jp.name.contains("TOE", true)) {
					drawable = ToeDrawable(jp.name,p1,p2,side)
				}
				else  {
					drawable = AppendageDrawable(jp.name,p1,p2,side)
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
