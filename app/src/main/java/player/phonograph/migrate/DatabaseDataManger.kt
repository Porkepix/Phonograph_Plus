/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.migrate

import player.phonograph.provider.DatabaseConstants.FAVORITE_DB
import player.phonograph.provider.DatabaseConstants.HISTORY_DB
import player.phonograph.provider.DatabaseConstants.MUSIC_PLAYBACK_STATE_DB
import player.phonograph.provider.DatabaseConstants.PATH_FILTER
import player.phonograph.provider.DatabaseConstants.SONG_PLAY_COUNT_DB
import player.phonograph.util.TimeUtil.currentTimestamp
import player.phonograph.util.zip.ZipUtil.addToZipFile
import player.phonograph.util.zip.ZipUtil.extractZipFile
import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DatabaseDataManger {

    fun exportDatabases(uri: Uri, context: Context): Boolean {
        return context.contentResolver.openFileDescriptor(uri, "w")?.use { parcel ->
            parcel.fileDescriptor?.let { fileDescriptor ->
                FileOutputStream(fileDescriptor).use {
                    exportDatabasesImpl(it, context)
                }
            }
        } ?: false
    }

    fun exportDatabases(folder: String = "exported", context: Context) {
        context.getExternalFilesDir(folder)?.let {
            val name = "phonograph_plus_databases_${currentTimestamp()}.zip"
            FileOutputStream(File(it, name)).use { fileOutputStream ->
                exportDatabasesImpl(fileOutputStream, context)
            }
        }
    }

    private fun exportDatabasesImpl(fileOutputStream: FileOutputStream, context: Context): Boolean {
        ZipOutputStream(fileOutputStream).use { zipOut ->
            addToZipFile(zipOut, context.getDatabasePath(FAVORITE_DB), FAVORITE_DB)
            addToZipFile(zipOut, context.getDatabasePath(PATH_FILTER), PATH_FILTER)
            addToZipFile(zipOut, context.getDatabasePath(HISTORY_DB), HISTORY_DB)
            addToZipFile(zipOut, context.getDatabasePath(SONG_PLAY_COUNT_DB), SONG_PLAY_COUNT_DB)
            addToZipFile(zipOut, context.getDatabasePath(MUSIC_PLAYBACK_STATE_DB), MUSIC_PLAYBACK_STATE_DB)
        }
        return true // todo
    }

    fun importDatabases(uri: Uri, context: Context): Boolean {
        return context.contentResolver.openFileDescriptor(uri, "r")?.use { parcel ->
            parcel.fileDescriptor?.let { fd ->
                FileInputStream(fd).use {
                    importDatabaseImpl(it, context, context.cacheDir)
                }
            }
        } ?: false
    }

    private fun importDatabaseImpl(
        fileInputStream: FileInputStream,
        context: Context,
        cacheDir: File
    ): Boolean {
        if (!cacheDir.exists() && !cacheDir.isDirectory && !cacheDir.canWrite())
            throw FileNotFoundException("Output dirs unavailable!")
        ZipInputStream(fileInputStream).use { zipIn ->
            extractZipFile(zipIn, cacheDir)
        }
        if (cacheDir.exists()) {
            replaceDatabaseFile(cacheDir, context)
        }
        return true // todo
    }

    private fun replaceDatabaseFile(sourceDir: File, context: Context) {
        if (sourceDir.exists() && sourceDir.isDirectory) {
            moveFile(from = File(sourceDir, FAVORITE_DB), to = context.getDatabasePath(FAVORITE_DB))
            moveFile(from = File(sourceDir, PATH_FILTER), to = context.getDatabasePath(PATH_FILTER))
            moveFile(from = File(sourceDir, HISTORY_DB), to = context.getDatabasePath(HISTORY_DB))
            moveFile(from = File(sourceDir, SONG_PLAY_COUNT_DB), to = context.getDatabasePath(SONG_PLAY_COUNT_DB))
            moveFile(from = File(sourceDir, MUSIC_PLAYBACK_STATE_DB), to = context.getDatabasePath(MUSIC_PLAYBACK_STATE_DB))
        }
    }

    private fun moveFile(from: File, to: File) {
        if (from.isDirectory || to.isDirectory) throw IllegalArgumentException("move dirs")
        if (from.exists() && from.canWrite()) {
            if (to.exists()) {
                Log.e(TAG, "deleting ${to.path}")
                to.delete().also { require(it) { "Can't delete ${to.path}" } }
            }
            from.renameTo(to).also { require(it) { "Restore ${from.path} failed!" } }
        }
    }

    private const val TAG = "DatabaseManger"
}
