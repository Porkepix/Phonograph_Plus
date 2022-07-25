/*
 * Copyright (c) 2022 Abou Zeid (kabouzeid) (original author)
 */
package lib.phonograph.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar
import util.mddesign.core.Themer
import util.mdcolor.pref.ThemeColor

/**
 * @author Aidan Follestad (afollestad)
 */
class SeekBarX : AppCompatSeekBar {
    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        Themer.setTint(this, ThemeColor.accentColor(context))
    }
}