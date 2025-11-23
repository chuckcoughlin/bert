/**
 * Copyright 2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.data

import chuckcoughlin.bert.common.solver.JointTree

/**
 * Interface for listeners (specifically the animation fragment) of
 * positional information from the robot converted into shapes
 * for display.
 */
interface LinkShapeObserver {
    /**
     * Allow only one observer of a given name.
     * @return the name of the observer
     */
    val name: String
    /*
     * Call this method after an observer newly registers. The
     * intention is to allow the observer clear the canvas.
     */
    fun resetGraphics()

    /**
     * Allow the observer to pick whatever type(s) are appropriate.
     * @param shape to be displayed
     */
    fun updateGraphics(skeleton: JointTree)
}
