/**
 * Copyright 2022-2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.os.Bundle
import androidx.fragment.app.Fragment


/**
 * This is a base class for all the page fragments, except lists. "open" implies extendable.
 */
open class BasicAssistantFragment(pos:Int): Fragment(), AssistantFragment {
    override val position: Int = pos


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
