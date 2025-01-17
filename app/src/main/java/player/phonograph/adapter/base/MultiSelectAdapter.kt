/*
 * Copyright (c) 2022 chr_56 & Karim Abou Zeid (kabouzeid)
 */
package player.phonograph.adapter.base

import android.content.Context
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import lib.phonograph.cab.CabStatus

/**
 * @author chr_56 & Karim Abou Zeid (kabouzeid)
 */
abstract class MultiSelectAdapter<VH : RecyclerView.ViewHolder, I>(
    protected val context: Context,
    private val cabController: MultiSelectionCabController?,
) : RecyclerView.Adapter<VH>() {

    open val multiSelectMenuHandler: ((Toolbar) -> Boolean)? = null

    protected var checkedList: MutableList<I> = ArrayList()
        private set

    private fun updateCab() {
        // todo
        cabController?.onDismiss = ::clearChecked

        if (multiSelectMenuHandler != null) {
            cabController?.menuHandler = multiSelectMenuHandler
        }

        cabController?.showContent(context, checkedList.size)
    }

    /** must return a real item **/
    protected abstract fun getItem(datasetPosition: Int): I

    abstract fun updateItemCheckStatusForAll()
    abstract fun updateItemCheckStatus(datasetPosition: Int)

    protected fun toggleChecked(datasetPosition: Int): Boolean {
        if (cabController != null) {
            val item = getItem(datasetPosition) ?: return false
            if (!checkedList.remove(item)) checkedList.add(item)
            updateItemCheckStatus(datasetPosition)
            updateCab()
            return true
        }
        return false
    }

    protected fun checkAll() {
        if (cabController != null) {
            checkedList.clear()
            for (i in 0 until itemCount) {
                val item = getItem(i)
                if (item != null) {
                    checkedList.add(item)
                }
            }
            updateItemCheckStatusForAll()
            updateCab()
        }
    }

    private fun clearChecked() {
        checkedList.clear()
        updateItemCheckStatusForAll()
    }

    protected fun isChecked(identifier: I): Boolean = checkedList.contains(identifier)

    protected val isInQuickSelectMode: Boolean
        get() = cabController?.cab != null && cabController.cab.status == CabStatus.STATUS_ACTIVE

    protected open fun getName(obj: I): String = obj.toString()

    protected var cabTextColorColor: Int
        get() = cabController?.textColor ?: 0
        set(value) {
            cabController?.textColor = value
        }

}
