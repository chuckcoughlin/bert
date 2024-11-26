/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * This class is used within a JSON response for to hold joint-property-values.
 */
class JointPropertyValue(jnt:Joint,prop: JointDynamicProperty,v:Double)  {
    val joint = jnt
    val property = prop
    var value = v
}