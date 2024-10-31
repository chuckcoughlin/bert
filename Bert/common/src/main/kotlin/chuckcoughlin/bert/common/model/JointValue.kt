/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * This class is a holder for a numerical parameter
 * a single joint.
 */
class JointValue (jnt:Joint,jvalue:Double) {
    val joint = jnt
    val value = jvalue
}