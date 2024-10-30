/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * This class is a holder for a text attribute
 * of a single joint
 */
class JointAttribute (jnt:Joint,att:String) {
    val joint = jnt
    val attribute = att
}