/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.audiofile

import coil.ImageLoader
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Size
import player.phonograph.coil.retriever.ImageRetriever
import player.phonograph.coil.retriever.retrieverFromConfig
import player.phonograph.util.Util.debug
import android.content.Context
import android.util.Log

class AudioFileFetcher private constructor(
    private val audioFile: AudioFile,
    private val context: Context,
    private val size: Size,
) : Fetcher {

    class Factory : Fetcher.Factory<AudioFile> {
        override fun create(data: AudioFile, options: Options, imageLoader: ImageLoader): Fetcher =
            AudioFileFetcher(data, options.context, options.size)
    }

    override suspend fun fetch(): FetchResult? =
        retrieve(retriever, audioFile, context, size)

    private fun retrieve(
        retrievers: List<ImageRetriever>,
        audioFile: AudioFile,
        context: Context,
        size: Size
    ): FetchResult? {
        for (retriever in retrievers) {
            val result = retriever.retrieve(audioFile.path, audioFile.albumId, context, size)
            if (result == null) {
                debug {
                    Log.v(TAG, "Image not available from ${retriever.name} for $audioFile")
                }
                continue
            } else {
                return result
            }
        }
        debug {
            Log.v(TAG, "No any cover for $audioFile")
        }
        return null
    }

    companion object {
        val retriever = retrieverFromConfig
        private const val TAG = "ImageRetriever"
    }
}
