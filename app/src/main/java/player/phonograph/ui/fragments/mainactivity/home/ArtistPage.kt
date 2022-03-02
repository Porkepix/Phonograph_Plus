/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.util.Log
import android.view.View
import android.widget.PopupWindow
import android.widget.RadioButton
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import player.phonograph.App
import player.phonograph.R
import player.phonograph.adapter.display.ArtistDisplayAdapter
import player.phonograph.adapter.display.DisplayAdapter
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.helper.SortOrder
import player.phonograph.loader.ArtistLoader
import player.phonograph.model.Artist
import player.phonograph.util.Util

class ArtistPage : AbsDisplayPage<Artist, DisplayAdapter<Artist>, GridLayoutManager>() {

    override fun initLayoutManager(): GridLayoutManager {
        return GridLayoutManager(hostFragment.requireContext(), 1)
            .also { it.spanCount = DisplayUtil(this).gridSize }
    }

    override fun initAdapter(): DisplayAdapter<Artist> {
        val displayUtil = DisplayUtil(this)

        val layoutRes =
            if (displayUtil.gridSize > displayUtil.maxGridSizeForList) R.layout.item_grid
            else R.layout.item_list
        Log.d(
            TAG, "layoutRes: ${ if (layoutRes == R.layout.item_grid) "GRID" else if (layoutRes == R.layout.item_list) "LIST" else "UNKNOWN" }"
        )

        return ArtistDisplayAdapter(
            hostFragment.mainActivity,
            hostFragment,
            ArrayList(), // empty until Artist loaded
            layoutRes
        ) {
            usePalette = displayUtil.colorFooter
        }
    }

    override fun loadDataSet() {
        loaderCoroutineScope.launch {
            val temp = ArtistLoader.getAllArtists(App.instance) as List<Artist>
            while (!isRecyclerViewPrepared) yield() // wait until ready

            withContext(Dispatchers.Main) {
                if (isRecyclerViewPrepared) adapter.dataset = temp
            }
        }
    }

    override fun getDataSet(): List<Artist> {
        return if (isRecyclerViewPrepared) adapter.dataset else emptyList()
    }

    override fun configPopup(popupMenu: PopupWindow, popup: PopupWindowMainBinding) {
        val displayUtil = DisplayUtil(this)

        // grid size
        popup.textGridSize.visibility = View.VISIBLE
        popup.gridSize.visibility = View.VISIBLE
        if (Util.isLandscape(resources)) popup.textGridSize.text = resources.getText(R.string.action_grid_size_land)
        val current = displayUtil.gridSize
        val max = displayUtil.maxGridSize
        for (i in 0 until max) popup.gridSize.getChildAt(i).visibility = View.VISIBLE
        popup.gridSize.clearCheck()
        (popup.gridSize.getChildAt(current - 1) as RadioButton).isChecked = true

        // color footer
        popup.actionColoredFooters.visibility = View.VISIBLE
        popup.actionColoredFooters.isChecked = displayUtil.colorFooter
        popup.actionColoredFooters.isEnabled = displayUtil.gridSize > displayUtil.maxGridSizeForList

        // sort order
        popup.sortOrderBasic.visibility = View.VISIBLE
        popup.textSortOrderBasic.visibility = View.VISIBLE
        popup.sortOrderContent.visibility = View.VISIBLE
        popup.textSortOrderContent.visibility = View.VISIBLE
        for (i in 0 until popup.sortOrderContent.childCount) popup.sortOrderContent.getChildAt(i).visibility = View.GONE

        val currentSortOrder = displayUtil.sortOrder
        Log.d(TAG, "Read cfg: $currentSortOrder")

        // todo
        when (currentSortOrder) {
            SortOrder.SongSortOrder.SONG_Z_A, SortOrder.AlbumSortOrder.ALBUM_Z_A, SortOrder.ArtistSortOrder.ARTIST_Z_A,
            SortOrder.SongSortOrder.SONG_DURATION_REVERT, SortOrder.AlbumSortOrder.ALBUM_ARTIST_REVERT,
            SortOrder.SongSortOrder.SONG_YEAR_REVERT, SortOrder.SongSortOrder.SONG_DATE_REVERT, SortOrder.SongSortOrder.SONG_DATE_MODIFIED_REVERT,
            -> popup.sortOrderBasic.check(R.id.sort_order_z_a)
            else
            -> popup.sortOrderBasic.check(R.id.sort_order_a_z)
        }

        popup.sortOrderArtist.visibility = View.VISIBLE
        when (currentSortOrder) {
            SortOrder.ArtistSortOrder.ARTIST_A_Z, SortOrder.ArtistSortOrder.ARTIST_Z_A -> popup.sortOrderContent.check(R.id.sort_order_artist)
            else -> { popup.sortOrderContent.clearCheck() }
        }
    }

    override fun initOnDismissListener(popupMenu: PopupWindow, popup: PopupWindowMainBinding): PopupWindow.OnDismissListener {
        val displayUtil = DisplayUtil(this)
        return PopupWindow.OnDismissListener {

            //  Grid Size
            var gridSizeSelected = 0
            for (i in 0 until displayUtil.maxGridSize) {
                if ((popup.gridSize.getChildAt(i) as RadioButton).isChecked) {
                    gridSizeSelected = i + 1
                    break
                }
            }

            if (gridSizeSelected > 0 && gridSizeSelected != displayUtil.gridSize) {

                displayUtil.gridSize = gridSizeSelected
                val itemLayoutRes =
                    if (gridSizeSelected > displayUtil.maxGridSizeForList) R.layout.item_grid else R.layout.item_list

                if (adapter.layoutRes != itemLayoutRes) {
                    loadDataSet()
                    initRecyclerView() // again
                }
                layoutManager.spanCount = gridSizeSelected
            }

            // color footer
            val coloredFootersSelected = popup.actionColoredFooters.isChecked
            if (displayUtil.colorFooter != coloredFootersSelected) {
                displayUtil.colorFooter = coloredFootersSelected
                adapter.usePalette = coloredFootersSelected
                adapter.dataset = getDataSet() // just refresh
            }

            // sort order
            val basicSelected = popup.sortOrderBasic.checkedRadioButtonId
            val sortOrderSelected: String =

                when (popup.sortOrderContent.checkedRadioButtonId) {
                    R.id.sort_order_artist ->
                        when (basicSelected) {
                            R.id.sort_order_a_z -> SortOrder.ArtistSortOrder.ARTIST_A_Z
                            R.id.sort_order_z_a -> SortOrder.ArtistSortOrder.ARTIST_Z_A
                            else -> ""
                        }
                    else -> ""
                }

            if (sortOrderSelected.isNotBlank() && displayUtil.sortOrder != sortOrderSelected) {
                displayUtil.sortOrder = sortOrderSelected
                loadDataSet()
                Log.d(TAG, "Write cfg: $sortOrderSelected")
            }
        }
    }

    override fun getHeaderText(): CharSequence {
        return "${hostFragment.mainActivity.getString(R.string.artists)}: ${getDataSet().size}"
    }

    companion object {
        const val TAG = "ArtistPage"
    }
}