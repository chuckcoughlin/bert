/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.common

/**
 * Create a model class for various lists - a name/value pair.
 * Add a hint to help the user.
 */
class NameValue {
    private var name = ""
    private var value = ""
    private var hint = ""

    constructor() {}
    constructor(nam: String, `val`: String, desc: String) {
        name = nam
        value = `val`
        hint = desc
    }

    fun getHint(): String {
        return hint
    }

    fun getName(): String {
        return name
    }

    fun getValue(): String {
        return value
    }

    fun setHint(desc: String) {
        hint = desc
    }

    fun setName(nam: String) {
        name = nam
    }

    fun setValue(`val`: String) {
        value = `val`
    }

    override fun hashCode(): Int {
        return getName().hashCode() + getValue().hashCode()
    }
}