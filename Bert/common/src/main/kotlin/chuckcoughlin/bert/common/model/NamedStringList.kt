/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.model

/**
 * This class is a holder for a list of strings.
 * Its purpose is to make it easy to format JSON.
 * nam - the name of the list
 * jtype  - the name as a key.
 */
class NamedStringList (val nam:String,val jtype:String) {
    val name = nam
    val key  = jtype
    val list = mutableListOf<String>()

    fun add(value:String) {
        list.add(value)
    }
}