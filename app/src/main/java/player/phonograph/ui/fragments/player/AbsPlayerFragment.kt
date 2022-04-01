package player.phonograph.ui.fragments.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import kotlinx.coroutines.*
import player.phonograph.R
import player.phonograph.adapter.song.PlayingQueueAdapter
import player.phonograph.dialogs.*
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.helper.menu.SongMenuHelper
import player.phonograph.interfaces.PaletteColorHolder
import player.phonograph.model.Song
import player.phonograph.model.lyrics2.AbsLyrics
import player.phonograph.ui.fragments.AbsMusicServiceFragment
import player.phonograph.util.FavoriteUtil
import player.phonograph.util.FavoriteUtil.toggleFavorite
import player.phonograph.util.MusicUtil
import player.phonograph.util.NavigationUtil.goToAlbum
import player.phonograph.util.NavigationUtil.goToArtist
import player.phonograph.util.NavigationUtil.openEqualizer
import util.phonograph.tageditor.AbsTagEditorActivity
import util.phonograph.tageditor.SongTagEditorActivity

abstract class AbsPlayerFragment :
    AbsMusicServiceFragment(),
    Toolbar.OnMenuItemClickListener,
    PaletteColorHolder {

    protected var callbacks: Callbacks? = null
        private set
    protected lateinit var playerAlbumCoverFragment: PlayerAlbumCoverFragment // setUpSubFragments() in derived class //todo make sure field gets inited
    protected val viewModel: PlayerFragmentViewModel by viewModels()

    lateinit var handler: Handler

    // recycle view
    protected lateinit var layoutManager: LinearLayoutManager
    protected lateinit var playingQueueAdapter: PlayingQueueAdapter
    protected lateinit var wrappedAdapter: RecyclerView.Adapter<*>
    protected lateinit var recyclerViewDragDropManager: RecyclerViewDragDropManager

    // toolbar
    protected lateinit var playerToolbar: Toolbar

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = try { context as Callbacks } catch (e: ClassCastException) { throw RuntimeException("${context.javaClass.simpleName} must implement ${Callbacks::class.java.simpleName}") }
        handler = Handler(Looper.getMainLooper()) { msg ->
            if (msg.what == UPDATE_LYRICS) {
                playerAlbumCoverFragment.setLyrics(msg.data.get(LYRICS) as AbsLyrics)
            }
            // then lock
            viewModel.lockLyricsWithSong(MusicPlayerRemote.getCurrentSong())
            false
        }
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        implementRecyclerView()
        initToolbar()
    }

    private fun initToolbar() {
        playerToolbar = getImplToolbar()
        playerToolbar.inflateMenu(R.menu.menu_player)
        playerToolbar.setNavigationIcon(R.drawable.ic_close_white_24dp)
        playerToolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }
        playerToolbar.setOnMenuItemClickListener(this)
    }
    abstract fun getImplToolbar(): Toolbar

    protected abstract fun implementRecyclerView()

    private fun initRecyclerView() {
        layoutManager = LinearLayoutManager(requireActivity())
        playingQueueAdapter = PlayingQueueAdapter(
            (requireActivity() as AppCompatActivity),
            MusicPlayerRemote.getPlayingQueue(),
            MusicPlayerRemote.getPosition(),
            R.layout.item_list,
            false,
            null
        )
        recyclerViewDragDropManager = RecyclerViewDragDropManager()
        wrappedAdapter = recyclerViewDragDropManager.createWrappedAdapter(playingQueueAdapter)
    }

    override fun onDestroy() {
        super.onDestroy()
        recyclerViewDragDropManager.release()
        WrapperAdapterUtils.releaseAll(wrappedAdapter)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {

        // toolbar
        when (item.itemId) {
            R.id.action_show_lyrics -> {
                val lyricsPack = viewModel.lyricsPack
                if (lyricsPack != null) {
                    LyricsDialog.create(lyricsPack, MusicPlayerRemote.getCurrentSong())
                        .show(requireActivity().supportFragmentManager, "LYRICS")
                }
                return true
            }
            R.id.action_toggle_favorite -> {
                toggleFavorite(MusicPlayerRemote.getCurrentSong())
                return true
            }
            R.id.action_clear_playing_queue -> {
                MusicPlayerRemote.clearQueue()
                return true
            }
            R.id.action_save_playing_queue -> {
                CreatePlaylistDialog.create(MusicPlayerRemote.getPlayingQueue())
                    .show(childFragmentManager, "ADD_TO_PLAYLIST")
                return true
            }
            R.id.action_sleep_timer -> {
                SleepTimerDialog().show(childFragmentManager, "SET_SLEEP_TIMER")
                return true
            }
            R.id.action_equalizer -> {
                openEqualizer(requireActivity())
                return true
            }
        }

        // current song
        val song = MusicPlayerRemote.getCurrentSong()
        when (item.itemId) {
            R.id.action_add_to_playlist -> {
                AddToPlaylistDialog.create(List(1) { song })
                    .show(childFragmentManager, "ADD_PLAYLIST")
                return true
            }
            R.id.action_details -> {
                SongDetailDialog.create(song)
                    .show(childFragmentManager, "SONG_DETAIL")
                return true
            }
            R.id.action_go_to_album -> {
                goToAlbum(requireActivity(), song.albumId)
                return true
            }
            R.id.action_go_to_artist -> {
                goToArtist(requireActivity(), song.artistId)
                return true
            }
            R.id.action_tag_editor -> {
                startActivity(
                    Intent(activity, SongTagEditorActivity::class.java)
                        .apply { putExtra(AbsTagEditorActivity.EXTRA_ID, song.id) }
                )
                return true
            }
            R.id.action_share -> {
                SongShareDialog.create(song)
                    .show(childFragmentManager, "SHARE_SONG")
                return true
            }
            else -> SongMenuHelper.handleMenuClick(requireActivity(), song, item.itemId)
        }
        return false
    }

    protected val backgroundCoroutine: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    protected val exceptionHandler by lazy {
        CoroutineExceptionHandler { _, throwable ->
            Log.w("LyricsFetcher", "Exception while fetching lyrics!\n${throwable.message}")
        }
    }

    protected fun updateLyrics(lyrics: AbsLyrics) = runBlocking(Dispatchers.Main) {
        playerAlbumCoverFragment.setLyrics(lyrics)
        showLyricsMenuItem()
    }
    protected fun clearLyrics() = backgroundCoroutine.launch(Dispatchers.Main) {
        playerAlbumCoverFragment.setLyrics(null)
        hideLyricsMenuItem()
    }
    protected fun loadAndRefreshLyrics(song: Song) {
        if (song == viewModel.songLocked) {
            // do not load
            return
        }

        viewModel.loadLyrics(song)
        clearLyrics()
        backgroundCoroutine.launch {
            // wait
            withTimeout(6000) {
                while (viewModel.lyricsPack == null) delay(120)
            }
            delay(100)
            // refresh anyway
            viewModel.currentLyrics?.let { updateLyrics(it) }
        }
    }

    protected abstract fun hideLyricsMenuItem()
    protected abstract fun showLyricsMenuItem()

    protected open fun toggleFavorite(song: Song) = toggleFavorite(requireActivity(), song)

    protected fun updateFavoriteState(song: Song) {
        backgroundCoroutine.launch(exceptionHandler) {
            val state = FavoriteUtil.isFavorite(this@AbsPlayerFragment.requireContext(), song)
            withContext(Dispatchers.Main) { updateFavoriteIcon(state) }
        }
    }
    protected abstract fun updateFavoriteIcon(isFavorite: Boolean)

    protected var isToolbarShown: Boolean
        get() = Companion.isToolbarShown
        set(toolbarShown) {
            Companion.isToolbarShown = toolbarShown
        }

    protected fun showToolbar(toolbar: View?) {
        if (toolbar == null) return
        isToolbarShown = true
        toolbar.visibility = View.VISIBLE
        toolbar.animate().alpha(1f).duration = PlayerAlbumCoverFragment.VISIBILITY_ANIM_DURATION.toLong()
    }

    protected fun hideToolbar(toolbar: View?) {
        if (toolbar == null) return
        isToolbarShown = false
        toolbar.animate().alpha(0f).setDuration(PlayerAlbumCoverFragment.VISIBILITY_ANIM_DURATION.toLong())
            .withEndAction { toolbar.visibility = View.GONE }
    }

    protected fun toggleToolbar(toolbar: View?) {
        if (isToolbarShown) {
            hideToolbar(toolbar)
        } else {
            showToolbar(toolbar)
        }
    }

    protected fun checkToggleToolbar(toolbar: View?) {
        if (toolbar != null && !isToolbarShown && toolbar.visibility != View.GONE) {
            hideToolbar(toolbar)
        } else if (toolbar != null && isToolbarShown && toolbar.visibility != View.VISIBLE) {
            showToolbar(toolbar)
        }
    }

    protected val upNextAndQueueTime: String
        get() {
            val duration = MusicPlayerRemote.getQueueDurationMillis(MusicPlayerRemote.getPosition())
            return MusicUtil.buildInfoString(
                resources.getString(R.string.up_next), MusicUtil.getReadableDurationString(duration)
            )
        }

    abstract fun onShow()
    abstract fun onHide()
    abstract fun onBackPressed(): Boolean

    interface Callbacks {
        fun onPaletteColorChanged()
    }

    companion object {
        private var isToolbarShown = true

        const val UPDATE_LYRICS = 1001
        const val LYRICS = "lyrics"
    }

    internal interface Impl {
        fun init()
        fun updateCurrentSong(song: Song)
        fun animateColorChange(newColor: Int)
        fun setUpPanelAndAlbumCoverHeight()
    }
}