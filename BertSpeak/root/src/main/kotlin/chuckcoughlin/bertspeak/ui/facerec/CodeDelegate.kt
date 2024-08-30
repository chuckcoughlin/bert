/**
 * Copyright 2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.ui.facerec

/**
 * Extend Enum classes using this delegate to associate
 * an integer code with the enumeration constant.
 */
interface CodeDelegate {
    val code : Int
}
