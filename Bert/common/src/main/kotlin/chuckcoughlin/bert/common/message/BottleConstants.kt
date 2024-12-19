/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.message

/**
 * Define strings used in requests and responses between server and client.
 */
object BottleConstants {

    // Controller names
    const val CONTROLLER_LOWER = "lower"
    const val CONTROLLER_UPPER = "upper"

    // Message from tablet
    const val HEADER_LENGTH = 4 // Includes semi-colon

    // Default values for some "empty" properties
    const val NO_ARG    = "No Argument"
    const val NO_CONTROLLER = "No Controller"
    const val NO_DELAY = 0L
    const val NO_ERROR = "No Error"

}