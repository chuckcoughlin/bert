/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

/**
 * This interface is required for all fragments in the application.
 */
interface AssistantFragment {
    val pageNumber: Int
    var title: String?

    companion object {
        const val PAGE_ARG  = "page"
        const val TITLE_ARG = "title"
    }
}
