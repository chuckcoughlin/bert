/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.os.Bundle
import androidx.fragment.app.ListFragment


/**
 * This is a base class for all the page fragments that are lists.
 */
open class BasicAssistantListFragment(page:Int) : ListFragment(), AssistantFragment {
    override val pageNumber = page
    override var title: String?
        get() {
            assert(arguments != null)
            return arguments?.getString(AssistantFragment.Companion.TITLE_ARG)
        }
        set(title) {
            assert(arguments != null)
            arguments?.putString(AssistantFragment.Companion.TITLE_ARG, title)
        }

    /**
     * A no-arg constructor is required.
     */
    init {
        val bundle = Bundle()
        arguments = bundle
    }
}
