/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * This class is a holder for the angle of
 * a single joint.
 */
class JointPosition (jnt:Joint,pos:Double) {
    val joint = jnt
    val position = pos
}