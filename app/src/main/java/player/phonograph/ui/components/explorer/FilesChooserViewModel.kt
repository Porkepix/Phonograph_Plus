/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.components.explorer

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import java.io.File
import java.util.*

class FilesChooserViewModel : AbsFileViewModel() {

    override fun onLoadFiles(location: Location, context: Context, scope: CoroutineScope?) {
        listFile(location, scope)
    }

    @Synchronized
    private fun listFile(
        location: Location,
        scope: CoroutineScope?,
    ) {
        currentFileList.clear()
        // todo
        val directory = File(location.absolutePath).also { if (!it.isDirectory) return }
        val files = directory.listFiles() ?: return
        val set = TreeSet<FileEntity>()
        for (file in files) {
            val l = Location.fromAbsolutePath(file.absolutePath)
            if (scope?.isActive == false) break
            val item =
                when {
                    file.isDirectory -> {
                        FileEntity.Folder(
                            location = l,
                            name = file.name,
                            dateAdded = file.lastModified(),
                            dateModified = file.lastModified()
                        ).also { it.songCount = 0 }
                    }
                    file.isFile -> {
                        FileEntity.File(
                            location = l,
                            name = file.name,
                            size = file.length(),
                            dateAdded = file.lastModified(),
                            dateModified = file.lastModified()
                        )
                    }
                    else -> null
                }
            item?.let { set.add(it) }
        }
        currentFileList.addAll(set)
    }

}
