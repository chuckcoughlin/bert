/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.common

/**
 * Create a model class for various lists - a name/value pair.
 * Add a hint to help the user. This is a data class to make it comparable.
 */
data class NameValue (var name: String,var value: String, var hint: String?="" )
