/**
 * Copyright 2023-2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 * @See http://hoodaandroid.blogspot.com/2012/10/vertical-seek-bar-or-slider-in-android.html
 */
package chuckcoughlin.bertspeak.ui

import android.content.Context
import android.util.AttributeSet
import chuckcoughlin.bertspeak.R
import chuckcoughlin.bertspeak.service.ManagerState

/**
 * Image Button that appears on the Cover fragment. Image changes with state.
 */
class StatusImageButton : androidx.appcompat.widget.AppCompatImageButton {
    var state: ManagerState

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) :
            super(context,attrs,defStyle) {}

    constructor(context: Context, attrs: AttributeSet?) :
            super(context, attrs) {}

    fun setButtonState(status: ManagerState) {
        state = status
        when(state) {
            ManagerState.OFF    -> {
                setImageResource(R.drawable.ball_gray)
            }
            ManagerState.ACTIVE ->{
                setImageResource(R.drawable.ball_green)
            }
            ManagerState.ERROR  -> {
                setImageResource(R.drawable.ball_red)
            }
            ManagerState.PENDING->{
                setImageResource(R.drawable.ball_yellow)
            }
            ManagerState.NONE->{
                setImageResource(R.drawable.ball_blue)
            }
        }

    }
    init {
        state = ManagerState.OFF

    }
}
