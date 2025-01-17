/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.pages

import player.phonograph.App
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.R
import player.phonograph.adapter.display.DisplayAdapter
import player.phonograph.adapter.display.GenreDisplayAdapter
import player.phonograph.mediastore.GenreLoader
import player.phonograph.model.Genre
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.ui.components.popup.ListOptionsPopup
import player.phonograph.ui.fragments.pages.util.DisplayConfig
import player.phonograph.ui.fragments.pages.util.DisplayConfigTarget
import androidx.recyclerview.widget.GridLayoutManager
import android.annotation.SuppressLint
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class GenrePage : AbsDisplayPage<Genre, DisplayAdapter<Genre>, GridLayoutManager>() {

    override val displayConfigTarget get() = DisplayConfigTarget.GenrePage

    override fun initLayoutManager(): GridLayoutManager {
        return GridLayoutManager(hostFragment.requireContext(), 1)
            .also { it.spanCount = DisplayConfig(displayConfigTarget).gridSize }
    }

    override fun initAdapter(): DisplayAdapter<Genre> {
        return GenreDisplayAdapter(
            hostFragment.mainActivity,
            hostFragment.cabController,
            ArrayList(), // empty until Genre loaded
            R.layout.item_list_no_image
        ) {
            showSectionName = true
        }
    }

    override fun loadDataSet() {
        loaderCoroutineScope.launch {
            val temp = GenreLoader.getAllGenres(App.instance)
            while (!isRecyclerViewPrepared) yield() // wait until ready

            withContext(Dispatchers.Main) {
                if (isRecyclerViewPrepared) adapter.dataset = temp
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun refreshDataSet() {
        adapter.notifyDataSetChanged()
    }

    override fun getDataSet(): List<Genre> {
        return if (isRecyclerViewPrepared) adapter.dataset else emptyList()
    }

    override fun setupSortOrderImpl(
        displayConfig: DisplayConfig,
        popup: ListOptionsPopup
    ) {

        val currentSortMode = displayConfig.sortMode
        if (DEBUG) Log.d(TAG, "Read cfg: sortMode $currentSortMode")

        popup.allowRevert = true
        popup.revert = currentSortMode.revert

        popup.sortRef = currentSortMode.sortRef
        popup.sortRefAvailable = arrayOf(SortRef.DISPLAY_NAME, SortRef.SONG_COUNT)
    }

    override fun saveSortOrderImpl(
        displayConfig: DisplayConfig,
        popup: ListOptionsPopup,
    ) {
        val selected = SortMode(popup.sortRef, popup.revert)
        if (displayConfig.sortMode != selected) {
            displayConfig.sortMode = selected
            loadDataSet()
            Log.d(TAG, "Write cfg: sortMode $selected")
        }
    }

    override fun getHeaderText(): CharSequence {
        val n = getDataSet().size
        return hostFragment.mainActivity.resources.getQuantityString(R.plurals.x_genres, n, n)
    }

    companion object {
        const val TAG = "GenrePage"
    }
}
