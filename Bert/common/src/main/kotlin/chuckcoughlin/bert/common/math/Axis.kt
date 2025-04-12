/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.math

import chuckcoughlin.bert.common.model.Limb
import chuckcoughlin.bert.common.util.TextUtility

/**
 * The axes are possible alignments of joints when the
 * robot is "straightened up".
 * X - side to side, positive to the right
 * Y - front to back, positive to the front
 * Z - up and down, positive up
 */
enum class Axis {
    X,Y,Z;

    companion object {
        /**
         * Convert the axis enumeration to text that can be pronounced.
         * @param axis the enumeration
         * @return user-recognizable text
         */
        fun toText(axis: Axis): String {
            var text = ""
            when (axis) {
                X -> text = "x"
                Y -> text = "y"
                Z -> text = "z"
            }
            return text
        }

        /**
         * @return  a comma-separated list of all axes in a single String.
         */
        fun names(): String {
            val names = StringBuffer()
            for (type in values()) {
                names.append(type.name + ", ")
            }
            return names.substring(0, names.length - 2)
        }

        /** @return  a comma-separated list of common names for the joints.
         */
        fun nameList(): String {
            val list = mutableListOf<String>()
            for (axis in Axis.values()) {
                list.add(Axis.toText(axis))
            }
            return TextUtility.createTextForSpeakingFromList(list)
        }

        /**
         * The enumeration function valueOf appears to always throw an exception.
         * This is the replacement. Case insensitive.
         */
        fun fromString(arg: String): Axis {
            for (axis in Axis.values()) {
                if (axis.name.equals(arg, true)) return axis
            }
            return Axis.X
        }
    }
}