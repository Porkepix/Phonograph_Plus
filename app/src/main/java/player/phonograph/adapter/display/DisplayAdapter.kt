/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.display

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import mt.util.color.primaryTextColor
import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.actions.click.listClick
import player.phonograph.actions.menu.multiItemsToolbar
import player.phonograph.adapter.base.MultiSelectAdapter
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.adapter.base.UniversalMediaEntryViewHolder
import player.phonograph.model.Displayable
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu

open class DisplayAdapter<I : Displayable>(
    protected val activity: AppCompatActivity,
    cabController: MultiSelectionCabController?,
    dataSet: List<I>,
    @LayoutRes var layoutRes: Int,
    cfg: (DisplayAdapter<I>.() -> Unit)?,
) :
    MultiSelectAdapter<DisplayAdapter<I>.DisplayViewHolder, I>(activity, cabController), FastScrollRecyclerView.SectionedAdapter {

    var dataset: List<I> = dataSet
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
        cfg?.invoke(this)
    }

    var usePalette: Boolean = false

    var useImageText: Boolean = false

    var showSectionName: Boolean = true

    override val multiSelectMenuHandler: ((Toolbar) -> Boolean)?
        get() = {
            multiItemsToolbar(it.menu, context, checkedList, cabTextColorColor) {
                checkAll()
                true
            }
        }

    override fun getItemId(position: Int): Long = dataset[position].getItemID()
    override fun getItem(datasetPosition: Int): I = dataset[datasetPosition]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder =
        DisplayViewHolder(
            LayoutInflater.from(activity).inflate(layoutRes, parent, false)
        )

    protected open val defaultIcon =
        AppCompatResources.getDrawable(activity, R.drawable.default_album_art)

    override fun onBindViewHolder(holder: DisplayViewHolder, position: Int) {
        val item: I = dataset[position]
        holder.shortSeparator?.visibility = View.VISIBLE
        holder.itemView.isActivated = isChecked(item)
        holder.title?.text = item.getDisplayTitle(context = activity)
        holder.text?.text = getDescription(item)
        if (useImageText) {
            setImageText(holder, getRelativeOrdinalText(item))
        } else {
            setImage(holder, position)
        }
    }

    protected open fun getDescription(item: I): CharSequence? =
        item.getDescription(context = activity)

    protected open fun setImage(holder: DisplayViewHolder, position: Int) {
        holder.image?.also {
            it.visibility = View.VISIBLE
            it.setImageDrawable(defaultIcon)
        }
    }

    protected open fun setImageText(holder: DisplayViewHolder, text: String) {
        holder.imageText?.also {
            it.visibility = View.VISIBLE
            it.text = text
        }
    }

    protected open fun getRelativeOrdinalText(item: I): String = "-"

    override fun getItemCount(): Int = dataset.size

    override fun updateItemCheckStatusForAll() = notifyDataSetChanged()
    override fun updateItemCheckStatus(datasetPosition: Int) = notifyItemChanged(datasetPosition)

    override fun getSectionName(position: Int): String =
        if (showSectionName) getSectionNameImp(position) else ""

    // for inheriting
    protected fun setPaletteColors(color: Int, holder: DisplayViewHolder) {
        holder.paletteColorContainer?.let { paletteColorContainer ->
            paletteColorContainer.setBackgroundColor(color)
            holder.title?.setTextColor(
                activity.primaryTextColor(color)
            )
            holder.text?.setTextColor(
                activity.secondaryTextColor(color)
            )
        }
    }

    // for inheriting
    open fun getSectionNameImp(position: Int): String =
        dataset[position].defaultSortOrderReference()?.substring(0..1) ?: ""

    protected open fun onMenuClick(bindingAdapterPosition: Int, menuButtonView: View) {
        if (dataset.isNotEmpty()) {
            PopupMenu(activity, menuButtonView).apply {
                dataset[bindingAdapterPosition].initMenu(activity,this.menu)
            }.show()
        }
    }

    protected open fun onClickItem(bindingAdapterPosition: Int, view: View, imageView: ImageView?) {
        when (isInQuickSelectMode) {
            true -> toggleChecked(bindingAdapterPosition)
            false -> {
                listClick(dataset, bindingAdapterPosition, activity, imageView)
            }
        }
    }

    protected open fun onLongClickItem(bindingAdapterPosition: Int, view: View): Boolean {
        return toggleChecked(bindingAdapterPosition)
    }

    open inner class DisplayViewHolder(itemView: View) : UniversalMediaEntryViewHolder(itemView) {

        init {
            // Item Click
            itemView.setOnClickListener {
                this@DisplayAdapter.onClickItem(bindingAdapterPosition, it, image)
            }
            // Item Long Click
            itemView.setOnLongClickListener {
                this@DisplayAdapter.onLongClickItem(bindingAdapterPosition, it)
            }
            // Menu Click
            menu?.setOnClickListener {
                onMenuClick(bindingAdapterPosition, it)
            }
            // Setup MenuItem
            dataset.getOrNull(0)?.let {
                menu?.visibility = if (it.hasMenu()) View.VISIBLE else View.GONE
            }
        }
    }
}
