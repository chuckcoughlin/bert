/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.control.model

/**
 * These are the possible types of "LinkPoints" or
 * connection locations on a link.
 */
enum class LinkPointType {
    APPENDAGE, ORIGIN, REVOLUTE;

    companion object {
        /**
         * @return  a comma-separated list of all block states in a single String.
         */
        fun names(): String {
            val names = StringBuffer()
            for (type in values()) {
                names.append(type.name + ", ")
            }
            return names.substring(0, names.length - 2)
        }
    }
}