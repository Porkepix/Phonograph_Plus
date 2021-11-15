/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewStub
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chr_56.MDthemer.core.ThemeColor
import com.afollestad.materialcab.MaterialCab
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import player.phonograph.R
import player.phonograph.adapter.song.PlaylistEditorAdapter
import player.phonograph.databinding.ActivityPlaylistEditorBinding
import player.phonograph.helper.menu.PlaylistMenuHelper
import player.phonograph.loader.PlaylistSongLoader
import player.phonograph.model.Playlist
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.PhonographColorUtil
import player.phonograph.util.PlaylistsUtil
import player.phonograph.util.ViewUtil

class PlaylistEditorActivity : AbsSlidingMusicPanelActivity() {
    private lateinit var binding: ActivityPlaylistEditorBinding // init in OnCreate()

    // init/bind in OnCreate() -> bindingView()
    private lateinit var recyclerView: RecyclerView
    private lateinit var mToolbar: Toolbar
    private lateinit var empty: TextView
    private lateinit var cabStub: ViewStub

    private lateinit var playlist: Playlist // init in OnCreate()

    private lateinit var adapter: PlaylistEditorAdapter

    private var cab: MaterialCab? = null // init in OnCreate() -> setUpRecyclerView()

    private lateinit var recyclerViewDragDropManager: RecyclerViewDragDropManager // init in OnCreate() -> setUpRecyclerView()
    private var wrappedAdapter: RecyclerView.Adapter<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityPlaylistEditorBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)

        bindingViews()

        setDrawUnderStatusbar()

        setStatusbarColorAuto()
        setNavigationbarColorAuto()
        setTaskDescriptionColorAuto()

        playlist = intent.extras!!.getParcelable(PlaylistDetailActivity.EXTRA_PLAYLIST)!!

        setUpRecyclerView()

        setUpToolbar()
    }

    private fun bindingViews() {
        mToolbar = binding.toolbar

        recyclerView = binding.recyclerView
        empty = binding.empty
        cabStub = binding.cabStub
    }
    override fun createContentView(): View {
        return wrapSlidingMusicPanel(binding.root)
    }

    private fun setUpToolbar() {
        mToolbar.setBackgroundColor(ThemeColor.primaryColor(this))
        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        setToolbarTitle(playlist.name)
    }
    private fun setToolbarTitle(title: String) {
        supportActionBar!!.title = title
    }
    private fun checkIsEmpty() {
        empty.visibility =
            if (adapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    private fun setUpRecyclerView() {
        ViewUtil.setUpFastScrollRecyclerViewColor(
            this, recyclerView as FastScrollRecyclerView, ThemeColor.accentColor(this)
        )
        recyclerView.layoutManager = LinearLayoutManager(this)

        recyclerViewDragDropManager = RecyclerViewDragDropManager()
        val animator: GeneralItemAnimator = RefactoredDefaultItemAnimator()

        adapter = PlaylistEditorAdapter(
            this, playlist
        ) { menuRes, callback ->
            // finish existed cab
            cab?.also { if (it.isActive) it.finish() }
            // create new
            MaterialCab(this, R.id.cab_stub)
                .setMenu(menuRes)
                .setCloseDrawableRes(R.drawable.ic_close_white_24dp)
                .setBackgroundColor(
                    PhonographColorUtil.shiftBackgroundColorForLightText(ThemeColor.primaryColor(this))
                )
                .start(callback)
                .apply {
                    cab = this // also set activity's cab to this
                }
        }

        wrappedAdapter = recyclerViewDragDropManager.createWrappedAdapter(adapter)
        recyclerView.adapter = wrappedAdapter
        recyclerView.itemAnimator = animator
        recyclerViewDragDropManager.attachRecyclerView(recyclerView)

        checkIsEmpty()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_playlist_editor, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_refresh -> {
                onMediaStoreChanged()
                return true
            }
            R.id.action_add -> {
                Toast.makeText(this, "Not available now", Toast.LENGTH_SHORT).show()
                return true
            }
            else -> PlaylistMenuHelper.handleMenuClick(this, playlist, item)
        }
    }

    override fun onBackPressed() {
        cab?.let {
            if (it.isActive) it.finish()
            else {
                recyclerView.stopScroll()
                super.onBackPressed()
            }
        } ?: super.onBackPressed()
    }

    override fun onPause() {
        recyclerViewDragDropManager.cancelDrag()
        super.onPause()
    }
    override fun onDestroy() {
        recyclerView.itemAnimator = null
        recyclerView.adapter = null
        wrappedAdapter?.let {
            WrapperAdapterUtils.releaseAll(it)
            wrappedAdapter = null
        }
        super.onDestroy()
    }

    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        // Playlist deleted
        if (!PlaylistsUtil.doesPlaylistExist(this, playlist.id)) {
            finish()
            return
        }
        // Playlist renamed
        val newPlaylistName = PlaylistsUtil.getNameForPlaylist(this, playlist.id)
        if (newPlaylistName != playlist.name) {
            setToolbarTitle(newPlaylistName)
        }

        // refresh playlist content
        adapter.playlistSongs = PlaylistSongLoader.getPlaylistSongList(this, playlist.id)
    }
}