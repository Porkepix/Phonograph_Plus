/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.playlist.saf

import lib.phonograph.storage.getAbsolutePath
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.util.PlaylistsUtil
import player.phonograph.util.Util
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import java.io.FileNotFoundException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@SuppressLint("Recycle")
internal fun openOutputStreamSafe(context: Context, uri: Uri, mode: String): OutputStream? =
    try {
        val outputStream = context.contentResolver.openOutputStream(uri, mode)
        if (outputStream == null) Util.warning(TAG, "Failed!")
        outputStream
    } catch (e: FileNotFoundException) {
        Util.reportError(e, TAG, "Not found $uri")
        null
    }


internal fun checkUri(context: Context, target: FilePlaylist, uri: Uri): Boolean =
    uri.getAbsolutePath(context) == PlaylistsUtil.getPlaylistPath(context, target)

/**
 * common path root of a list of paths
 * @param paths list of path separating by '/'
 * @return common root path of these [paths], **empty** if no common
 */
internal fun commonPathRoot(paths: Collection<String>): String {

    val fragments = paths.map { path -> path.split('/').filter { it.isNotEmpty() } }
    val result = mutableListOf<String>()

    var index = 0
    while (true) {
        val col = fragments.mapNotNull { it.getOrNull(index) }.toSet()
        if (col.size == 1) {
            result.add(col.first())
            index++
        } else {// size > 1 or size == 0
            break
        }
    }

    return result.fold("") { acc, s -> "$acc/$s" }
}

fun appendTimestampSuffix(string: String): String {
    val suffix = SimpleDateFormat("_yy-MM-dd_HH-mm", Locale.ENGLISH).format(Calendar.getInstance().time)
    return string + suffix
}

private const val TAG = "Playlist"