/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.data

/**
 * Interface for entities which need to be informed about the
 * status of joints in the robot
 */
interface GeometryDataObserver {
    /**
     * Allow only one observer of a given name.
     * @return the name of the observer
     */
    val name: String

    /**
     * Call this method after an observer newly registers. This
     * allows the observer to "catch-up" with the
     * current state of the joint list.
     */
    fun resetGeometry(list: List<GeometryData>)

    /**
     * Notify the observer that a new positional entry has been added
     * to the manager's list.
     * @param msg the new message
     */
    fun updateGeometry(msg: GeometryData)
}
