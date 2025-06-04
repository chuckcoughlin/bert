/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * This class is a holder for numerical parameters
 * of a single joint.
 */
class JointPropertyHolder (val jnt:String,val angle:Double,val speed:Double,val load:Double,val temperature:Double,
                           val maxSpeed:Double,maxTorque:Double) {
}