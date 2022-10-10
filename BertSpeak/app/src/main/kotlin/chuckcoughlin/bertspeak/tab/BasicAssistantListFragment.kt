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
open class BasicAssistantListFragment : ListFragment(), AssistantFragment {
    override var pageNumber: Int
        get() {
            assert(getArguments() != null)
            return getArguments().getInt(AssistantFragment.Companion.PAGE_ARG)
        }
        set(page) {
            assert(getArguments() != null)
            getArguments().putInt(AssistantFragment.Companion.PAGE_ARG, page)
        }
    override var title: String?
        get() {
            assert(getArguments() != null)
            return getArguments().getString(AssistantFragment.Companion.TITLE_ARG)
        }
        set(title) {
            assert(getArguments() != null)
            getArguments().putString(AssistantFragment.Companion.TITLE_ARG, title)
        }

    /**
     * The saved state becomes the fragment's state.
     */
    fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /**
     * Everything that matters is in the bundle.
     * @param stateToSave dictionary of values to persist
     */
    fun onSaveInstanceState(stateToSave: Bundle?) {
        super.onSaveInstanceState(stateToSave)
    }

    /**
     * A no-arg constructor is required.
     */
    init {
        val bundle = Bundle()
        setArguments(bundle)
    }
}
