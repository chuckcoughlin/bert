/**
 * Copyright 2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.data

/**
 * Interface for listeners of the geometry manager. The
 * listeners are expected to render the result on a screen.
 */
interface GeometryDataObserver {
    /**
     * Allow only one observer of a given name. This is the observer key
     * @return the name of the observer
     */
    val name: String

    /**
     * Call this method after an observer newly registers. The
     * intention is to allow the observer to "catch-up" with the
     * current robot position.
     *
     * @param geom robot position
     */
    fun resetGeometry(geom: GeometryData)

    /**
     * @param geom robot position update.
     */
    fun updateGeometry(geom: GeometryData)
}
