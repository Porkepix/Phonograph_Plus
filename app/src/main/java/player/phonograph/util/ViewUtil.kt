@file:Suppress("unused")

package player.phonograph.util

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import mt.util.color.isWindowBackgroundDark
import mt.util.color.primaryTextColor
import mt.util.color.resolveColor
import mt.util.color.withAlpha
import player.phonograph.R
import androidx.annotation.ColorInt
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.view.View

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object ViewUtil {

    fun createSelectorDrawable(context: Context?, @ColorInt color: Int): Drawable {
        val baseSelector = StateListDrawable()
        baseSelector.addState(intArrayOf(android.R.attr.state_activated), ColorDrawable(color))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return RippleDrawable(ColorStateList.valueOf(color), baseSelector, ColorDrawable(Color.WHITE))
        }
        baseSelector.addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
        baseSelector.addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(color))
        return baseSelector
    }

    fun hitTest(v: View, x: Int, y: Int): Boolean {
        val tx = (v.translationX + 0.5f).toInt()
        val ty = (v.translationY + 0.5f).toInt()
        val left = v.left + tx
        val right = v.right + tx
        val top = v.top + ty
        val bottom = v.bottom + ty
        return x in left..right && y >= top && y <= bottom
    }

    fun FastScrollRecyclerView.setUpFastScrollRecyclerViewColor(context: Context, accentColor: Int) {
        setPopupBgColor(accentColor)
        setPopupTextColor(context.primaryTextColor(accentColor))
        setThumbColor(accentColor)
        setTrackColor(withAlpha(resolveColor(context, R.attr.colorControlNormal), 0.12f)) // todo
    }

    fun convertDpToPixel(dp: Float, resources: Resources): Float {
        val metrics = resources.displayMetrics
        return dp * metrics.density
    }

    fun convertPixelsToDp(px: Float, resources: Resources): Float {
        val metrics = resources.displayMetrics
        return px / metrics.density
    }

    fun isWindowBackgroundDarkSafe(context: Context?): Boolean =
        try {
            context?.let {
                isWindowBackgroundDark(context)
            } ?: false
        } catch (e: Exception) {
            false
        }
}
