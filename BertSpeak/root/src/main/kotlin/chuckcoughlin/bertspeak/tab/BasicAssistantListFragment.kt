/**
 * Copyright 2022-2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.os.Bundle
import androidx.fragment.app.ListFragment


/**
 * This is a base class for all the page fragments that are lists.
 */
open class BasicAssistantListFragment(pos:Int) : ListFragment(), AssistantFragment {
    override val position: Int = pos

    /**
     * A no-arg constructor is required.
     */
    init {
        val bundle = Bundle()
        arguments = bundle
    }
}
