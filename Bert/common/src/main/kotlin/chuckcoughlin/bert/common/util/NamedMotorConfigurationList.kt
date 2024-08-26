/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.util

import chuckcoughlin.bert.common.model.MotorConfiguration

/**
 * This class is a holder for a list of motor configurations.
 * Its purpose is to make it easy to format JSON.
 * nam - the name of the list
 * jtype  - the name as a 4 character key.
 */
class NamedMotorConfigurationList (val nam:String,val jtype:String) {
    val name = nam
    val key  = jtype
    val list = mutableListOf<MotorConfiguration>()

    fun add(mc:MotorConfiguration) {
        list.add(mc)
    }
}