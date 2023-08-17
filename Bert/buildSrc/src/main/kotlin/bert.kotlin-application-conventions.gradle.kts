/**
 * Copyright 2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
plugins {
    // Apply the common convention plugin for shared build configuration between library and application projects.
    id("bert.kotlin-common-conventions")
    // Apply the application plugin to add support for building a CLI application in Kotlikn (Java).
    application
}
