/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.control.controller

/**
 * Recognized names of SequentialQueues managed by the InternalController. These correspond to
 * roughly independent sub-chains of the joint tree.
 */
enum class QueueName {
    HEAD, RIGHT_ARM, RIGHT_LEG, LEFT_ARM, LEFT_LEG, GLOBAL;

    companion object {
        /**
         * @return  a comma-separated list of all queue name in a single String.
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