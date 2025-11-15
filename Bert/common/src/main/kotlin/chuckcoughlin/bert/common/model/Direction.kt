/**
 * (c) 2025  Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bert.common.model

/**
 * Direction of motion:
 */
enum class Direction {
    RIGHT,LEFT,FRONT,BACK,UP,DOWN,UNKNOWN;

    companion object {
        fun fromString(arg: String): Direction {
            for (dir in Direction.values()) {
                if(dir.name.equals(arg, true)) return dir
            }
            return UNKNOWN
        }
    }
}
