/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.changelog

data class ReleaseNoteModel(
    val version: String,
    val versionCode: Int,
    val time: Long,
    val channel: String?,
    val note: Note
) {
    data class Note(
        val en: List<String>,
        val zh: List<String>
    )
}