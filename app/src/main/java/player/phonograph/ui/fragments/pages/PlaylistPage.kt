/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.pages

import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.ArrayList
import kotlinx.coroutines.*
import mt.pref.ThemeColor
import mt.util.color.lightenColor
import player.phonograph.App
import player.phonograph.BROADCAST_PLAYLISTS_CHANGED
import player.phonograph.R
import player.phonograph.adapter.PlaylistAdapter
import player.phonograph.databinding.FragmentDisplayPageBinding
import player.phonograph.ui.dialogs.CreatePlaylistDialog
import player.phonograph.misc.PlaylistsModifiedReceiver
import player.phonograph.model.playlist.FavoriteSongsPlaylist
import player.phonograph.model.playlist.HistoryPlaylist
import player.phonograph.model.playlist.LastAddedPlaylist
import player.phonograph.model.playlist.MyTopTracksPlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.settings.Setting
import player.phonograph.util.PlaylistsUtil
import player.phonograph.util.ViewUtil.setUpFastScrollRecyclerViewColor

class PlaylistPage : AbsPage() {

    private var _viewBinding: FragmentDisplayPageBinding? = null
    private val binding get() = _viewBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        loadPlaylist()
        _viewBinding = FragmentDisplayPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    private lateinit var adapter: PlaylistAdapter
    private lateinit var layoutManager: RecyclerView.LayoutManager
    private lateinit var adapterDataObserver: RecyclerView.AdapterDataObserver

    private var isRecyclerViewPrepared: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.panel.visibility = View.GONE

        layoutManager = LinearLayoutManager(requireActivity())

        adapter = PlaylistAdapter(
            hostFragment.mainActivity,
            ArrayList<Playlist>(), R.layout.item_list_single_row,
            hostFragment.cabController
        )

        binding.recyclerView.setUpFastScrollRecyclerViewColor(
            requireActivity(),
            ThemeColor.accentColor(requireActivity())
        )
        binding.recyclerView.apply {
            layoutManager = this@PlaylistPage.layoutManager
            adapter = this@PlaylistPage.adapter
        }
        isRecyclerViewPrepared = true

        adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkEmpty()
            }
        }
        adapter.registerAdapterDataObserver(adapterDataObserver)

        // Receiver
        playlistsModifiedReceiver = PlaylistsModifiedReceiver(this::loadPlaylist)
        LocalBroadcastManager.getInstance(App.instance).registerReceiver(
            playlistsModifiedReceiver!!,
            IntentFilter().also { it.addAction(BROADCAST_PLAYLISTS_CHANGED) }
        )

        setUpFloatingActionButton()
    }

    private val loaderCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private fun loadPlaylist() {
        loaderCoroutineScope.launch {
            val context = hostFragment.mainActivity
            val cache = mutableListOf<Playlist>(
                LastAddedPlaylist(context),
                HistoryPlaylist(context),
                MyTopTracksPlaylist(context),
            ).also {
                if (!Setting.instance.useLegacyFavoritePlaylistImpl)
                    it.add(FavoriteSongsPlaylist(context),)
            }.also { it.addAll(PlaylistsUtil.getAllPlaylists(context)) }

            while (!isRecyclerViewPrepared) yield() // wait until ready

            withContext(Dispatchers.Main) {
                if (isRecyclerViewPrepared) adapter.dataSet = cache
            }
        }
    }

    private val emptyMessage: Int = R.string.no_playlists
    private fun checkEmpty() {
        if (isRecyclerViewPrepared) {
            binding.empty.setText(emptyMessage)
            binding.empty.visibility = if (adapter.dataSet.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onMediaStoreChanged() {
        loadPlaylist()
        super.onMediaStoreChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
        isRecyclerViewPrepared = false
        LocalBroadcastManager.getInstance(App.instance).unregisterReceiver(playlistsModifiedReceiver!!)
        playlistsModifiedReceiver = null
    }

    private var playlistsModifiedReceiver: PlaylistsModifiedReceiver? = null

    private fun setUpFloatingActionButton() {
        val primaryColor = ThemeColor.primaryColor(hostFragment.mainActivity)
        val accentColor = ThemeColor.accentColor(hostFragment.mainActivity)

        binding.addNewItem.backgroundTintList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_activated),
                intArrayOf(android.R.attr.state_pressed),
                intArrayOf(),
            ),
            intArrayOf(
                lightenColor(primaryColor), accentColor, primaryColor
            )
        )

        binding.addNewItem.setOnClickListener {
            CreatePlaylistDialog.create(null).show(childFragmentManager, "CREATE_NEW_PLAYLIST")
        }

        binding.addNewItem.visibility = View.VISIBLE
    }

    companion object {
        const val TAG = "PlaylistPage"
    }
}
