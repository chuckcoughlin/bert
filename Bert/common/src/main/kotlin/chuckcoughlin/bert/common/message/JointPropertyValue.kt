/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.message

import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.JointDynamicProperty
import java.util.logging.Logger

/**
 * This class is used within a MessageBottle to hold property-value pairs.
 */
class JointPropertyValue()  {
    var joint : Joint
        private set
    var property : JointDynamicProperty
        private set
    var value : Number

    constructor(j: Joint,prop:JointDynamicProperty) : this() {
       joint = j
       property = prop
    }
    constructor(j: Joint,prop:JointDynamicProperty,valu:Number) : this(j,prop) {
        value = valu
    }

    companion object {
        private const val CLSS = "JointPropertyValue"
        private val LOGGER = Logger.getLogger(CLSS)
    }
    init {
        joint = Joint.NONE
        property = JointDynamicProperty.NONE
        value = Double.NaN
    }
}