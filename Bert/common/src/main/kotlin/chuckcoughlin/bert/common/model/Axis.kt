/**
 * (c) 2025  Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bert.common.model

/**
 * Joint alignment
 */
enum class Axis {
    X,Y,Z,UNKNOWN;

    companion object {
        fun fromString(arg: String): Axis {
            for (axis in Axis.values()) {
                if(axis.name.equals(arg, true)) return axis
            }
            return Axis.UNKNOWN
        }
    }
}
