/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package player.phonograph.util

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Playlists
import android.provider.MediaStore.Audio.PlaylistsColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import player.phonograph.model.BasePlaylist
import player.phonograph.provider.BlacklistStore

object PlaylistsUtil {
    private const val TAG: String = "PlaylistUtil"

    fun getAllPlaylists(context: Context): List<BasePlaylist> {
        return getAllPlaylists(
            queryPlaylists(context, null, null)
        )
    }
    fun getAllPlaylists(cursor: Cursor?): List<BasePlaylist> {
        val basePlaylists: MutableList<BasePlaylist> = java.util.ArrayList()
        if (cursor != null && cursor.moveToFirst()) {
            do {
                basePlaylists.add(
                    BasePlaylist(cursor.getLong(0), cursor.getString(1))
                )
            } while (cursor.moveToNext())
        }
        cursor?.close()
        return basePlaylists
    }

    fun getPlaylist(context: Context, playlistId: Long): BasePlaylist {
        return getPlaylist(
            queryPlaylists(
                context, BaseColumns._ID + "=?", arrayOf(playlistId.toString())
            )
        )
    }
    fun getPlaylist(context: Context, playlistName: String): BasePlaylist {
        return getPlaylist(
            queryPlaylists(
                context, PlaylistsColumns.NAME + "=?", arrayOf(playlistName)
            )
        )
    }
    fun getPlaylist(cursor: Cursor?): BasePlaylist {
        var playlist = BasePlaylist()
        if (cursor != null && cursor.moveToFirst()) {
            playlist = BasePlaylist(cursor.getLong(0), cursor.getString(1))
        }
        cursor?.close()
        return playlist
    }

    /**
     * query playlist file via MediaStore
     */
    @SuppressLint("Recycle")
    fun queryPlaylists(
        context: Context,
        selection: String?,
        selectionValues: Array<String>?
    ): Cursor? {
        var realSelection =
            if (selection != null && selection.trim { it <= ' ' } != "") {
                "${MediaStoreUtil.SongConst.BASE_PLAYLIST_SELECTION} AND $selection "
            } else {
                MediaStoreUtil.SongConst.BASE_PLAYLIST_SELECTION
            }
        var realSelectionValues = selectionValues

        // Blacklist
        val paths: List<String> = BlacklistStore.getInstance(context).paths
        if (paths.isNotEmpty()) {

            realSelection += " AND ${MediaStore.MediaColumns.DATA} NOT LIKE ?"
            for (i in 0 until paths.size - 1) {
                realSelection += " AND ${MediaStore.MediaColumns.DATA} NOT LIKE ?"
            }
            Log.i(TAG, "playlist selection: $realSelection")

            realSelectionValues =
                Array<String>((selectionValues?.size ?: 0) + paths.size) { index ->
                    // Todo: Check
                    if (index < (selectionValues?.size ?: 0)) selectionValues?.get(index) ?: ""
                    else paths[index - (selectionValues?.size ?: 0)] + "%"
                }
        }

        val cursor: Cursor? = try {
            context.contentResolver.query(
                Playlists.EXTERNAL_CONTENT_URI,
                arrayOf(
                    BaseColumns._ID, /* 0 */
                    PlaylistsColumns.NAME /* 1 */
                ),
                realSelection, realSelectionValues, Playlists.DEFAULT_SORT_ORDER
            )
        } catch (e: SecurityException) {
            null
        }
        return cursor
    }

    fun getPlaylistPath(context: Context, basePlaylist: BasePlaylist): String {
        val cursor = context.contentResolver.query(
            Playlists.EXTERNAL_CONTENT_URI,
            arrayOf(
                BaseColumns._ID /* 0 */,
                PlaylistsColumns.NAME /* 1 */,
                PlaylistsColumns.DATA /* 2 */
            ),
            "${BaseColumns._ID} = ? AND ${PlaylistsColumns.NAME} = ?",
            arrayOf(basePlaylist.id.toString(), basePlaylist.name), null
        )
        var path: String = "-"
        cursor?.let {
            it.moveToFirst()
            path = it.getString(2)
            it.close()
        }
        return path
    }

    fun doesPlaylistExist(context: Context, name: String): Boolean {
        return doesPlaylistExistImp(context, PlaylistsColumns.NAME + "=?", arrayOf(name))
    }
    fun doesPlaylistExist(context: Context, playlistId: Long): Boolean =
        playlistId != -1L && doesPlaylistExistImp(context, Playlists._ID + "=?", arrayOf(playlistId.toString()))

    private fun doesPlaylistExistImp(context: Context, selection: String, values: Array<String>): Boolean {
        val cursor = context.contentResolver
            .query(Playlists.EXTERNAL_CONTENT_URI, arrayOf(), selection, values, null)
        var exists = false
        if (cursor != null) {
            exists = cursor.count != 0
            cursor.close()
        }
        return exists
    }

    fun getNameForPlaylist(context: Context, id: Long): String {
        try {
            val cursor = context.contentResolver.query(
                ContentUris.withAppendedId(Playlists.EXTERNAL_CONTENT_URI, id),
                arrayOf(PlaylistsColumns.NAME), null, null, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                cursor.use { return it.getString(0).orEmpty() }
            }
        } catch (ignored: SecurityException) { }
        return ""
    }

    /**
     * WARNING: random order (perhaps)
     */
    fun getPlaylistFileNames(context: Context, basePlaylists: List<BasePlaylist>): List<String> {

        val ids: List<Long> = List(basePlaylists.size) { basePlaylists[it].id }

        val cursor = context.contentResolver.query(
            Playlists.EXTERNAL_CONTENT_URI,
            arrayOf(
                BaseColumns._ID /* 0 */,
                MediaStore.MediaColumns.DISPLAY_NAME /* 1 */
            ),
            null, null, null
        )

        val displayNames: MutableList<String> = ArrayList()
        if (cursor != null && cursor.moveToFirst()) {
            do {
                ids.forEach { id ->
                    if (cursor.getLong(0) == id) {
                        displayNames.add(cursor.getString(1))
                    }
                }
            } while (cursor.moveToNext())
            cursor.close()
        }

        return if (displayNames.isNotEmpty()) displayNames else ArrayList()
    }

    fun getPlaylistUris(basePlaylist: BasePlaylist): Uri = getPlaylistUris(basePlaylist.id)

    fun getPlaylistUris(playlistId: Long): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            Playlists.Members.getContentUri(MediaStore.VOLUME_EXTERNAL, playlistId)
        else {
            Playlists.Members.getContentUri("external", playlistId)
        }

    fun doesPlaylistContain(context: Context, playlistId: Long, songId: Long): Boolean {
        if (playlistId != -1L) {
            try {
                val c = context.contentResolver.query(
                    getPlaylistUris(playlistId),
                    arrayOf(Playlists.Members.AUDIO_ID),
                    Playlists.Members.AUDIO_ID + "=?", arrayOf(songId.toString()), null
                )
                var count = 0
                if (c != null) {
                    count = c.count
                    c.close()
                }
                return count > 0
            } catch (ignored: SecurityException) {
            }
        }
        return false
    }

    fun searchPlaylist(context: Context, dir: DocumentFile, basePlaylists: List<BasePlaylist>): List<DocumentFile> {
        val fileNames = getPlaylistFileNames(context, basePlaylists)
        if (fileNames.isEmpty()) {
            Log.w(TAG, "No playlist display name?")
            return ArrayList()
        }
        return searchFiles(context, dir, fileNames)
    }

    fun searchFiles(context: Context, dir: DocumentFile, filenames: List<String>): List<DocumentFile> {
        if (dir.isFile or dir.isVirtual) return ArrayList()
        val result: MutableList<DocumentFile> = ArrayList(1)
        if (dir.isDirectory) {
            dir.listFiles().forEach { sub ->
                if (sub.isFile) {
                    filenames.forEach { n ->
                        if (n == sub.name.orEmpty()) result.add(sub)
                    }
                }
                if (sub.isDirectory) {
                    result.addAll(
                        searchFiles(context, sub, filenames)
                    )
                }
            }
        }
        return result
    }
}
