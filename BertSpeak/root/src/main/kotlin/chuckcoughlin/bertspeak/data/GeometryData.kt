/**
 * Copyright 2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.data

import java.util.Date
/**
 * This class holds a enough positional information to
 * draw a rendering of the robot. The messaging is internal
 * from GeometryManager to the AnimationFragment
 */
data class GeometryData(val data: String ) {
    val timestamp: Date
    val geom:String

    init {
        timestamp = Date()
        geom = data
    }
}
