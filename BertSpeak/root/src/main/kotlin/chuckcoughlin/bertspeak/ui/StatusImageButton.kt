/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 * @See http://hoodaandroid.blogspot.com/2012/10/vertical-seek-bar-or-slider-in-android.html
 */
package chuckcoughlin.bertspeak.ui

import android.content.Context
import android.util.AttributeSet
import chuckcoughlin.bertspeak.R
import chuckcoughlin.bertspeak.service.ControllerState

/**
 * Image Button that appears on the Cover fragment. Image changes with state.
 */
class StatusImageButton : androidx.appcompat.widget.AppCompatImageButton {
    var state: ControllerState

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) :
            super(context,attrs,defStyle) {}

    constructor(context: Context, attrs: AttributeSet?) :
            super(context, attrs) {}

    fun setButtonState(status: ControllerState) {
        state = status
        when(state) {
            ControllerState.OFF -> {
                setImageResource(R.drawable.ball_gray)
            }
            ControllerState.ACTIVE->{
                setImageResource(R.drawable.ball_green)
            }
            ControllerState.ERROR-> {
                setImageResource(R.drawable.ball_red)
            }
            ControllerState.PENDING->{
                setImageResource(R.drawable.ball_yellow)
            }
        }

    }
    init {
        state = ControllerState.OFF

    }
}
