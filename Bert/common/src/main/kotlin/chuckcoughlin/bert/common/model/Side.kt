/**
 * (c) 2025  Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bert.common.model

/**
 * Side has multiple uses:
 *    1) Side of the body, determines drawing order
 *    2) Viewing side defines the projection.
 */
enum class Side {
    RIGHT,LEFT,FRONT,BACK,TOP,BOTTOM,UNKNOWN;

    companion object {
        fun fromString(arg: String): Side {
            for (side in Side.values()) {
                if(side.name.equals(arg, true)) return side
            }
            return UNKNOWN
        }
    }
}
