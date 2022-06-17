/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import player.phonograph.mediastore.AlbumLoader
import player.phonograph.model.Album
import player.phonograph.model.Song

class AlbumDetailActivityViewModel : ViewModel() {

    var isRecyclerViewPrepared: Boolean = false

    var albumId: Long = -1
    private var _album: Album? = null
    val album: Album get() = _album ?: Album()

    fun loadDataSet(
        context: Context,
        callback: (Album, List<Song>) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {

            _album = AlbumLoader.getAlbum(context, albumId)

            val songs: List<Song> = album.songs

            while (!isRecyclerViewPrepared) yield() // wait until ready
            withContext(Dispatchers.Main) {
                if (isRecyclerViewPrepared) {
                    callback(album, songs)
                }
            }
        }
    }

    val paletteColor: MutableLiveData<Int> = MutableLiveData(0)
}