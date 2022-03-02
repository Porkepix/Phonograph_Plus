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
import player.phonograph.adapter.display.DisplayAdapter
import player.phonograph.adapter.display.SongDisplayAdapter
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.helper.SortOrder
import player.phonograph.model.Song
import player.phonograph.util.MediaStoreUtil
import player.phonograph.util.Util

class SongPage : AbsDisplayPage<Song, DisplayAdapter<Song>, GridLayoutManager>() {

    override fun initLayoutManager(): GridLayoutManager {
        return GridLayoutManager(hostFragment.requireContext(), 1)
            .also { it.spanCount = DisplayUtil(this).gridSize }
    }

    override fun initAdapter(): DisplayAdapter<Song> {
        val displayUtil = DisplayUtil(this)

        val layoutRes =
            if (displayUtil.gridSize > displayUtil.maxGridSizeForList) R.layout.item_grid
            else R.layout.item_list
        Log.d(
            TAG, "layoutRes: ${ if (layoutRes == R.layout.item_grid) "GRID" else if (layoutRes == R.layout.item_list) "LIST" else "UNKNOWN" }"
        )

        return SongDisplayAdapter(
            hostFragment.mainActivity,
            hostFragment,
            ArrayList(), // empty until songs loaded
            layoutRes
        ) {
            usePalette = displayUtil.colorFooter
        }
    }

    override fun loadDataSet() {
        loaderCoroutineScope.launch {
            val temp = MediaStoreUtil.getAllSongs(App.instance) as List<Song>
            while (!isRecyclerViewPrepared) yield() // wait until ready

            withContext(Dispatchers.Main) {
                if (isRecyclerViewPrepared) adapter.dataset = temp
            }
        }
    }

    override fun getDataSet(): List<Song> {
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

        popup.sortOrderContent.clearCheck()
        popup.sortOrderSong.visibility = View.VISIBLE
        popup.sortOrderAlbum.visibility = View.VISIBLE
        popup.sortOrderArtist.visibility = View.VISIBLE
        popup.sortOrderYear.visibility = View.VISIBLE
        popup.sortOrderDateAdded.visibility = View.VISIBLE
        popup.sortOrderDateModified.visibility = View.VISIBLE
        popup.sortOrderDuration.visibility = View.VISIBLE
        when (currentSortOrder) {
            SortOrder.SongSortOrder.SONG_A_Z, SortOrder.SongSortOrder.SONG_Z_A -> popup.sortOrderContent.check(R.id.sort_order_song)
            SortOrder.SongSortOrder.SONG_ALBUM, SortOrder.SongSortOrder.SONG_ALBUM_REVERT -> popup.sortOrderContent.check(R.id.sort_order_album)
            SortOrder.SongSortOrder.SONG_ARTIST, SortOrder.SongSortOrder.SONG_ARTIST_REVERT -> popup.sortOrderContent.check(R.id.sort_order_artist)
            SortOrder.SongSortOrder.SONG_YEAR, SortOrder.SongSortOrder.SONG_YEAR_REVERT -> popup.sortOrderContent.check(R.id.sort_order_year)
            SortOrder.SongSortOrder.SONG_DATE, SortOrder.SongSortOrder.SONG_DATE_REVERT -> popup.sortOrderContent.check(R.id.sort_order_date_added)
            SortOrder.SongSortOrder.SONG_DATE_MODIFIED, SortOrder.SongSortOrder.SONG_DATE_MODIFIED_REVERT -> popup.sortOrderContent.check(R.id.sort_order_date_modified)
            SortOrder.SongSortOrder.SONG_DURATION, SortOrder.SongSortOrder.SONG_DURATION_REVERT -> popup.sortOrderContent.check(R.id.sort_order_duration)
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
                    R.id.sort_order_song ->
                        when (basicSelected) {
                            R.id.sort_order_a_z -> SortOrder.SongSortOrder.SONG_A_Z
                            R.id.sort_order_z_a -> SortOrder.SongSortOrder.SONG_Z_A
                            else -> ""
                        }
                    R.id.sort_order_album ->
                        when (basicSelected) {
                            R.id.sort_order_a_z -> SortOrder.SongSortOrder.SONG_ALBUM
                            R.id.sort_order_z_a -> SortOrder.SongSortOrder.SONG_ALBUM_REVERT
                            else -> ""
                        }
                    R.id.sort_order_artist ->
                        when (basicSelected) {
                            R.id.sort_order_a_z -> SortOrder.SongSortOrder.SONG_ARTIST
                            R.id.sort_order_z_a -> SortOrder.SongSortOrder.SONG_ARTIST_REVERT
                            else -> ""
                        }
                    R.id.sort_order_year ->
                        when (basicSelected) {
                            R.id.sort_order_a_z -> SortOrder.SongSortOrder.SONG_YEAR
                            R.id.sort_order_z_a -> SortOrder.SongSortOrder.SONG_YEAR_REVERT
                            else -> ""
                        }
                    R.id.sort_order_date_added ->
                        when (basicSelected) {
                            R.id.sort_order_a_z -> SortOrder.SongSortOrder.SONG_DATE
                            R.id.sort_order_z_a -> SortOrder.SongSortOrder.SONG_DATE_REVERT
                            else -> ""
                        }
                    R.id.sort_order_date_modified ->
                        when (basicSelected) {
                            R.id.sort_order_a_z -> SortOrder.SongSortOrder.SONG_DATE_MODIFIED
                            R.id.sort_order_z_a -> SortOrder.SongSortOrder.SONG_DATE_MODIFIED_REVERT
                            else -> ""
                        }
                    R.id.sort_order_duration ->
                        when (basicSelected) {
                            R.id.sort_order_a_z -> SortOrder.SongSortOrder.SONG_DURATION
                            R.id.sort_order_z_a -> SortOrder.SongSortOrder.SONG_DURATION_REVERT
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
        return "${hostFragment.mainActivity.getString(R.string.songs)}: ${getDataSet().size}"
    }

    companion object {
        const val TAG = "SongPage"
    }
}