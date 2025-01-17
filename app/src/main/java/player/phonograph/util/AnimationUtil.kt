/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util

import androidx.annotation.ColorInt
import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.view.View
import android.view.animation.PathInterpolator
import android.widget.TextView

object AnimationUtil {

    const val PHONOGRAPH_ANIM_TIME = 1000L

    fun View.backgroundColorTransitionAnimator(
        @ColorInt startColor: Int,
        @ColorInt endColor: Int
    ): Animator = createColorAnimator(this, "backgroundColor", startColor, endColor)

    fun TextView.textColorTransitionAnimator(
        @ColorInt startColor: Int,
        @ColorInt endColor: Int
    ): Animator = createColorAnimator(this, "textColor", startColor, endColor)

    @SuppressLint("ObsoleteSdkInt")
    private fun createColorAnimator(
        target: Any,
        propertyName: String,
        @ColorInt startColor: Int,
        @ColorInt endColor: Int,
    ): Animator =
        if (SDK_INT >= LOLLIPOP) {
            ObjectAnimator.ofArgb(target, propertyName, startColor, endColor).also {
                it.interpolator = PathInterpolator(0.4f, 0f, 1f, 1f)
            }
        } else {
            ObjectAnimator.ofInt(target, propertyName, startColor, endColor).also {
                it.setEvaluator(ArgbEvaluator())
            }
        }.apply {
            duration = PHONOGRAPH_ANIM_TIME
        }
}