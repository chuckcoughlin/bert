/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

import com.google.gson.GsonBuilder
import java.util.logging.Logger

/**
 * These are properties that define a stepper motor.
 * These are defined in the URDF file and are not
 * generally modifiable.
 */
enum class JointDefinitionProperty {
    ID,
    MOTORTYPE,
    OFFSET,
    ORIENTATION,
    NONE;

    companion object {
        private val CLSS = "JointDefinitionProperty"
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
         * This is the replacement and is case in-sensitive. On no match, NONE
         * is returned.
         */
        fun fromString(arg: String): JointDefinitionProperty {
            for (prop: JointDefinitionProperty in JointDefinitionProperty.values()) {
                if (prop.name.equals(arg, true)) return prop
            }
            /*
            JointDefinitionProperty.LOGGER.info(
                String.format(
                    "%s.fromString: no match for %s",
                    JointDefinitionProperty.CLSS, arg
                )
            )
             */
            return JointDefinitionProperty.NONE
        }

        /**
         * @return  a JSON pretty-printed String array of all property types. Exclude NONE.
         */
        fun toJSON(): String {
            val gson = GsonBuilder().setPrettyPrinting().create()
            var names = mutableListOf<String>()
            for (type in values()) {
                if (!type.equals(JointDefinitionProperty.NONE))
                    names.add(type.name)
            }
            return gson.toJson(names)
        }
    }
}