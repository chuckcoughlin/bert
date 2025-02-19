/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import com.google.gson.GsonBuilder
import java.util.logging.Logger

/**
 * These are properties of a stepper motor that are modifiable
 * at runtime. These all have numeric values.
 */
enum class JointDynamicProperty {
    MAXIMUMANGLE,
    MINIMUMANGLE,
    ANGLE,
    LOAD,
    RANGE,
    MAXIMUMSPEED,
    SPEED,
    STATE,
    TEMPERATURE,
    MAXIMUMTORQUE,
    TORQUE,       // Limit
    VOLTAGE,
    NONE;


    companion object {
        private val CLSS = "JointDynamicProperty"
        private val LOGGER = Logger.getLogger(CLSS)

        /**
         * @return  a comma-separated list of all property types in a single String.
         */
        fun names(): String {
            val names = StringBuffer()
            for (type in values()) {
                names.append(type.name + ", ")
            }
            return names.substring(0, names.length - 2)
        }

        /**
         * The enumeration function valueOf appears to always throw an exception.
         * This is the replacement and is case in-sensitive. On no-match
         * return NONE.
         */
        fun fromString(arg: String): JointDynamicProperty {
            for (prop: JointDynamicProperty in JointDynamicProperty.values()) {
                if (prop.name.equals(arg, true)) return prop
            }
            return JointDynamicProperty.NONE
        }

        /**
         * @return  a JSON pretty-printed String array of all property types. Exclude NONE.
         */
        fun toJSON(): String {
            val gson = GsonBuilder().setPrettyPrinting().create()
            var names = mutableListOf<String>()
            for( type in values()) {
                if(!type.equals(NONE))
                    names.add(type.name)
            }
            return gson.toJson(names)
        }
    }
}