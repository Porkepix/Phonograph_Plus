/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.afollestad.materialdialogs.utils.MDUtil.getStringArray
import com.google.android.material.appbar.AppBarLayout
import com.simplecityapps.recyclerview_fastscroll.interfaces.OnFastScrollStateChangeListener
import lib.phonograph.cab.*
import player.phonograph.App
import player.phonograph.PlaylistType
import player.phonograph.R
import player.phonograph.adapter.display.PlaylistSongAdapter
import player.phonograph.databinding.ActivityPlaylistDetailBinding
import player.phonograph.helper.menu.PlaylistMenuHelper.handleMenuClick
import player.phonograph.interfaces.MultiSelectionCabProvider
import player.phonograph.misc.SAFCallbackHandlerActivity
import player.phonograph.misc.SafLauncher
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.GeneratedPlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.MusicUtil
import player.phonograph.util.PhonographColorUtil
import player.phonograph.util.PlaylistsUtil
import player.phonograph.util.ViewUtil.setUpFastScrollRecyclerViewColor
import util.mdcolor.ColorUtil
import util.mdcolor.pref.ThemeColor
import util.mddesign.core.Themer
import util.mddesign.util.MaterialColorHelper

class PlaylistDetailActivity : AbsSlidingMusicPanelActivity(), SAFCallbackHandlerActivity, MultiSelectionCabProvider {

    private lateinit var binding: ActivityPlaylistDetailBinding

    private val model: PlaylistModel by viewModels()

    private lateinit var adapter: PlaylistSongAdapter // init in OnCreate() -> setUpRecyclerView()

    // for saf callback
    private lateinit var safLauncher: SafLauncher
    override fun getSafLauncher(): SafLauncher = safLauncher

    /* ********************
     *
     *  First Initialization
     *
     * ********************/

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityPlaylistDetailBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)

        setDrawUnderStatusbar()

        setStatusbarColorAuto()
        setNavigationbarColorAuto()
        setTaskDescriptionColorAuto()

        Themer.setActivityToolbarColorAuto(this, binding.toolbar)

        model.playlist.observe(this) {
            model.fetchPlaylist(this, playlistCallBack)
        }
        model.playlist.value = intent.extras!!.getParcelable(EXTRA_PLAYLIST)!!

        safLauncher = SafLauncher(activityResultRegistry)
        lifecycle.addObserver(safLauncher)

        setUpToolbar()
        setUpRecyclerView()
        setUpDashBroad()
    }

    private val primaryColor: Int get() = ThemeColor.primaryColor(this)
    private val accentColor: Int get() = ThemeColor.accentColor(this)

    override fun createContentView(): View = wrapSlidingMusicPanel(binding.root)

    private fun setUpToolbar() {
        binding.toolbar.setBackgroundColor(primaryColor)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    private fun setUpRecyclerView() {
        binding.recyclerView.setUpFastScrollRecyclerViewColor(this, accentColor)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        binding.recyclerView.setOnFastScrollStateChangeListener(
            object : OnFastScrollStateChangeListener {
                override fun onFastScrollStart() {
                    binding.dashBroad.setExpanded(false, false)
                    // hide dashboard instantly
                }
                override fun onFastScrollStop() { }
            }
        )

        model.playlist.value ?: FilePlaylist()
        // Init adapter
        adapter = PlaylistSongAdapter(
            this, this, ArrayList(),
            R.layout.item_list
        ) {}
        binding.recyclerView.adapter = adapter

        model.isRecyclerViewReady = true
    }

    private fun setUpDashBroad() {
        with(binding) {
            dashBroad.addOnOffsetChangedListener(
                AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                    recyclerView.setPadding(
                        recyclerView.paddingLeft,
                        dashBroad.totalScrollRange + verticalOffset,
                        recyclerView.paddingRight,
                        recyclerView.paddingBottom
                    )
                }
            )
            recyclerView.setPadding(
                recyclerView.paddingLeft, recyclerView.paddingTop + dashBroad.height, recyclerView.paddingRight, recyclerView.paddingBottom
            )
        }
    }
    private val playlistCallBack: PlaylistCallback
        get() = { playlist: Playlist, songs: List<Song> ->
            adapter.dataset = songs
            binding.empty.visibility = if (songs.isEmpty()) View.VISIBLE else View.GONE
            supportActionBar!!.title = playlist.name
            if (playlist !is SmartPlaylist && !PlaylistsUtil.doesPlaylistExist(this, playlist.id)) {
                // File Playlist was deleted
                finish()
            }
            updateDashboard()
        }

    private fun updateDashboard() {

        // colors

        val textColor = MaterialColorHelper.getSecondaryTextColor(this, ColorUtil.isColorLight(primaryColor))
        val iconColor = MaterialColorHelper.getSecondaryDisabledTextColor(this, ColorUtil.isColorLight(primaryColor))
        with(binding) {

            dashBroad.background = ColorDrawable(primaryColor)

            nameIcon.setImageDrawable(getTintedDrawable(R.drawable.ic_description_white_24dp, iconColor, BlendModeCompat.SRC_ATOP))
            songCountIcon.setImageDrawable(getTintedDrawable(R.drawable.ic_music_note_white_24dp, iconColor, BlendModeCompat.SRC_ATOP))
            durationIcon.setImageDrawable(getTintedDrawable(R.drawable.ic_timer_white_24dp, iconColor, BlendModeCompat.SRC_ATOP))
            pathIcon.setImageDrawable(getTintedDrawable(R.drawable.ic_file_music_white_24dp, iconColor, BlendModeCompat.SRC_ATOP))

            icon.setImageDrawable(getTintedDrawable(R.drawable.ic_queue_music_white_24dp, textColor))

            nameText.setTextColor(textColor)
            songCountText.setTextColor(textColor)
            durationText.setTextColor(textColor)
            pathText.setTextColor(textColor)
        }
        // text

        val playlist = model.playlist.value
        with(binding) {
            nameText.text = playlist?.name ?: "-"
            songCountText.text = adapter.dataset.size.toString()
            durationText.text = MusicUtil.getReadableDurationString(MusicUtil.getTotalDuration(this@PlaylistDetailActivity, adapter.dataset))
            if (playlist is FilePlaylist) {
                pathText.text = playlist.associatedFilePath
            } else {
                pathText.visibility = View.GONE
                pathIcon.visibility = View.GONE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val playlist: Playlist = model.playlist.value ?: FilePlaylist()
        menuInflater.inflate(
            if (playlist is SmartPlaylist) R.menu.menu_smart_playlist_detail else R.menu.menu_playlist_detail, menu
        )
        if (playlist.type == PlaylistType.LAST_ADDED)
            menu.add(Menu.NONE, R.id.action_setting_last_added_interval, Menu.NONE, R.string.pref_title_last_added_interval)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_shuffle_playlist -> {
                MusicPlayerRemote.openAndShuffleQueue(adapter.dataset, true)
                true
            }
            R.id.action_edit_playlist -> {
                startActivityForResult(
                    Intent(this, PlaylistEditorActivity::class.java).apply {
                        putExtra(EXTRA_PLAYLIST, model.playlist.value!!)
                    },
                    EDIT_PLAYLIST
                )
                true
            }
            R.id.action_refresh -> {
                val playlist: Playlist = model.playlist.value ?: FilePlaylist()
                adapter.dataset = emptyList()
                if (playlist is GeneratedPlaylist) {
                    playlist.refresh(this)
                }
                model.triggerUpdate()
                true
            }
            R.id.action_setting_last_added_interval -> {
                val prefValue = App.instance.getStringArray(R.array.pref_playlists_last_added_interval_values)
                val currentChoice = prefValue.indexOf(Setting.instance.lastAddedCutoffPref)
                MaterialDialog(this)
                    .listItemsSingleChoice(
                        res = R.array.pref_playlists_last_added_interval_titles,
                        initialSelection = currentChoice.let { if (it == -1) 0 else it },
                        checkedColor = accentColor
                    ) { dialog, index, _ ->
                        runCatching {
                            Setting.instance.lastAddedCutoffPref = prefValue[index]
                        }.apply {
                            if (isSuccess) {
                                model.triggerUpdate()
                            }
                        }
                        dialog.dismiss()
                    }
                    .title(R.string.pref_title_last_added_interval)
                    .show()
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> handleMenuClick(this, model.playlist.value!!, item)
        }
    }

    override fun onBackPressed() {
        if (multiSelectionCab != null && multiSelectionCab!!.status == CabStatus.STATUS_ACTIVE) {
            dismissCab()
            return
        } else if (multiSelectionCab != null) {
            multiSelectionCab!!.destroy()
            multiSelectionCab = null
        }
        binding.recyclerView.stopScroll()
        super.onBackPressed()
    }

    /* *******************
     *
     *    States Changed
     *
     * *******************/

    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        model.triggerUpdate()
    }

    override fun onDestroy() {
        super.onDestroy()
        multiSelectionCab?.destroy()
        multiSelectionCab = null
        binding.recyclerView.adapter = null
    }

    /* *******************
     *
     *     cabCallBack
     *
     * *******************/

    private var multiSelectionCab: MultiSelectionCab? = null

    override fun deployCab(
        menuRes: Int,
        initCallback: InitCallback?,
        showCallback: ShowCallback?,
        selectCallback: SelectCallback?,
        hideCallback: HideCallback?,
        destroyCallback: DestroyCallback?,
    ): MultiSelectionCab {
        val cfg: CabCfg = {
            val primaryColor = ThemeColor.primaryColor(this@PlaylistDetailActivity)
            val textColor = Color.WHITE

            backgroundColor = PhonographColorUtil.shiftBackgroundColorForLightText(primaryColor)
            titleTextColor = textColor

            closeDrawable = getTintedDrawable(R.drawable.ic_close_white_24dp, textColor)!!

            this.menuRes = menuRes

            onInit(initCallback)
            onShow(showCallback)
            onSelection(selectCallback)
            onHide(hideCallback)
            onClose { dismissCab() }
            onDestroy(destroyCallback)
        }

        if (multiSelectionCab == null) multiSelectionCab =
            createMultiSelectionCab(this, R.id.cab_stub, R.id.multi_selection_cab, cfg)
        else {
            multiSelectionCab!!.applyCfg = cfg
            multiSelectionCab!!.refresh()
        }

        return multiSelectionCab!!
    }

    override fun getCab(): MultiSelectionCab? = multiSelectionCab

    override fun showCab() {
        multiSelectionCab?.let { cab ->
            binding.toolbar.visibility = View.INVISIBLE
            cab.refresh()
            cab.show()
        }
    }

    override fun dismissCab() {
        multiSelectionCab?.hide()
        binding.toolbar.visibility = View.VISIBLE
    }

    /* *******************
     *   companion object
     * *******************/

    companion object {
        private const val TAG = "PlaylistDetail"
        const val EXTRA_PLAYLIST = "extra_playlist"
        const val EDIT_PLAYLIST: Int = 100
    }
}
