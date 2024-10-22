/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.message

import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.JointDynamicProperty
import java.util.logging.Logger

/**
 * This class is used within a MessageBottle to hold joint-property-value objects.
 * These are used to define a pose.
 */
class JointPropertyValue()  {
    var joint : Joint
        private set
    var property : JointDynamicProperty
        private set
    var value : Double

    constructor(j: Joint,prop:JointDynamicProperty) : this() {
       joint = j
       property = prop
    }
    constructor(j: Joint,prop:JointDynamicProperty,valu:Double) : this(j,prop) {
        value = valu
    }

    private val CLSS = "JointPropertyValue"
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        joint = Joint.NONE
        property = JointDynamicProperty.NONE
        value = Double.NaN
    }
}