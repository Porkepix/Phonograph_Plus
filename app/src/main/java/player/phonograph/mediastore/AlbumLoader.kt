/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mediastore

import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns
import android.util.ArrayMap
import player.phonograph.mediastore.SongLoader.getSongs
import player.phonograph.mediastore.SongLoader.makeSongCursor
import player.phonograph.mediastore.sort.SortRef
import player.phonograph.model.Album
import player.phonograph.model.Song
import player.phonograph.settings.Setting

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object AlbumLoader {

    fun getAllAlbums(context: Context): List<Album> {
        val songs = getSongs(
            makeSongCursor(context, null, null, null)
        )
        return splitIntoAlbums(songs)
    }

    fun getAlbums(context: Context, query: String): List<Album> {
        val songs = getSongs(
            makeSongCursor(context, "${AudioColumns.ALBUM} LIKE ?", arrayOf("%$query%"), null)
        )
        return splitIntoAlbums(songs)
    }

    fun getAlbum(context: Context, albumId: Long): Album {
        val songs = getSongs(
            makeSongCursor(context, "${AudioColumns.ALBUM_ID}=?", arrayOf(albumId.toString()), null)
        )
        return Album(albumId, getAlbumTitle(songs), songs.toMutableList().sortedBy { it.trackNumber })
    }

    fun splitIntoAlbums(songs: List<Song>?): List<Album> {
        if (songs == null) return ArrayList()

        // AlbumID <-> List of song
        val idMap: MutableMap<Long, MutableList<Song>> = ArrayMap()
        for (song in songs) {
            if (idMap[song.albumId] == null) {
                // create new
                idMap[song.albumId] = ArrayList<Song>(1).apply { add(song) }
            } else {
                // add to existed
                idMap[song.albumId]!!.add(song)
            }
        }

        // map to list
        return idMap.map { entry ->
            // create album from songs
            Album(
                id = entry.key,
                // list of song
                title = getAlbumTitle(entry.value),
                songs = entry.value.apply {
                    sortBy { it.trackNumber } // sort songs before create album
                }
            )
        }.sortAll()
    }

    private fun getAlbumTitle(list: List<Song>): String? {
        if (list.isEmpty()) return null
        return list[0].albumName
    }

    private fun List<Album>.sortAll(): List<Album> {
        val revert = Setting.instance.albumSortMode.revert
        return when (Setting.instance.albumSortMode.sortRef) {
            SortRef.ALBUM_NAME -> this.sort(revert) { it.title }
            SortRef.ARTIST_NAME -> this.sort(revert) { it.artistName }
            SortRef.YEAR -> this.sort(revert) { it.year }
            SortRef.SONG_COUNT -> this.sort(revert) { it.songCount }
            else -> this
        }
    }

    private inline fun List<Album>.sort(
        revert: Boolean,
        crossinline selector: (Album) -> Comparable<*>?
    ): List<Album> {
        return if (revert) this.sortedWith(compareByDescending(selector))
        else this.sortedWith(compareBy(selector))
    }
}
