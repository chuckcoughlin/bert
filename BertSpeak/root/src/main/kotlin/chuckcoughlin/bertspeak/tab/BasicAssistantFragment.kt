/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.os.Bundle
import androidx.fragment.app.Fragment


/**
 * This is a base class for all the page fragments, except lists. "open" implies extendable.
 */
open class BasicAssistantFragment(page:Int): Fragment(), AssistantFragment {
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
     * Everything that matters is in the bundle.
     * @param stateToSave dictionary of values to persist
     */
    override fun onSaveInstanceState(stateToSave: Bundle) {
        super.onSaveInstanceState(stateToSave)
    }

    /**
     * A no-arg constructor is required.
     */
    init {
        val bundle = Bundle()
        arguments = bundle
    }
}
