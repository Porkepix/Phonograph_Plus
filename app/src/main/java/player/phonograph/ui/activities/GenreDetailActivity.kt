package player.phonograph.ui.activities

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chr_56.MDthemer.core.ThemeColor
import chr_56.MDthemer.core.Themer
import com.afollestad.materialcab.MaterialCab
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import player.phonograph.R
import player.phonograph.adapter.song.SongAdapter
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.interfaces.CabHolder
import player.phonograph.interfaces.LoaderIds
import player.phonograph.loader.GenreLoader
import player.phonograph.misc.WrappedAsyncTaskLoader
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.PhonographColorUtil
import player.phonograph.util.ViewUtil

class GenreDetailActivity :
    AbsSlidingMusicPanelActivity(), CabHolder, LoaderManager.LoaderCallbacks<List<Song>> {

    private lateinit var recyclerView: RecyclerView
    private lateinit var mToolbar: Toolbar
    private lateinit var empty: TextView

    private lateinit var genre: Genre

    private lateinit var adapter: SongAdapter

    private var cab: MaterialCab? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setDrawUnderStatusbar()

        // todo: viewBinding
        recyclerView = findViewById(R.id.recycler_view)
        mToolbar = findViewById(R.id.toolbar)
        empty = findViewById(android.R.id.empty)

        Themer.setActivityToolbarColorAuto(this, mToolbar)

        setStatusbarColorAuto()
        setNavigationbarColorAuto()
        setTaskDescriptionColorAuto()

        genre = intent.extras?.getParcelable(EXTRA_GENRE) ?: throw Exception("No genre in the intent!")

        setUpRecyclerView()
        setUpToolBar()

        LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this)
    }

    override fun createContentView(): View {
        return wrapSlidingMusicPanel(R.layout.activity_genre_detail)
    }

    private fun setUpRecyclerView() {
        ViewUtil.setUpFastScrollRecyclerViewColor(
            this, recyclerView as FastScrollRecyclerView, ThemeColor.accentColor(this)
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SongAdapter(this, ArrayList(), R.layout.item_list, false, this)
        recyclerView.adapter = adapter

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkIsEmpty()
            }
        })
    }

    private fun setUpToolBar() {
        mToolbar.setBackgroundColor(ThemeColor.primaryColor(this))
        setSupportActionBar(mToolbar)
        supportActionBar!!.title = genre.name
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_genre_detail, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_shuffle_genre -> {
                MusicPlayerRemote.openAndShuffleQueue(adapter.dataSet, true)
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun openCab(menu: Int, callback: MaterialCab.Callback): MaterialCab {
        cab?.let { if (it.isActive) it.finish() }
        return MaterialCab(this, R.id.cab_stub)
            .setMenu(menu)
            .setCloseDrawableRes(R.drawable.ic_close_white_24dp)
            .setBackgroundColor(PhonographColorUtil.shiftBackgroundColorForLightText(ThemeColor.primaryColor(this)))
            .start(callback)
            .also { cab = it /* set activity's cab */ }
    }

    override fun onBackPressed() {
        if (cab != null && cab!!.isActive) cab!!.finish() else {
            recyclerView.stopScroll()
            super.onBackPressed()
        }
    }

    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this)
    }

    private fun checkIsEmpty() {
        empty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        recyclerView.adapter = null
        super.onDestroy()
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<Song>> {
        return AsyncGenreSongLoader(this, genre)
    }

    override fun onLoadFinished(loader: Loader<List<Song>>, data: List<Song>) {
        adapter.swapDataSet(data)
    }

    override fun onLoaderReset(loader: Loader<List<Song>>) {
        adapter.swapDataSet(ArrayList())
    }

    private class AsyncGenreSongLoader(context: Context, private val genre: Genre) :
        WrappedAsyncTaskLoader<List<Song>>(context) {
        override fun loadInBackground(): List<Song> {
            return GenreLoader.getSongs(context, genre.id)
        }
    }

    companion object {
        private const val LOADER_ID = LoaderIds.GENRE_DETAIL_ACTIVITY
        const val EXTRA_GENRE = "extra_genre"
    }
}