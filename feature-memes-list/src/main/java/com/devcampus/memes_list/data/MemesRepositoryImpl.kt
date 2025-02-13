package com.devcampus.memes_list.data

import android.content.Context
import com.devcampus.memes_list.domain.MemesRepository
import com.devcampus.memes_list.domain.model.MemeFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

internal class MemesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MemesRepository {

    private companion object {
        private const val MEMES_FOLDER = "memes"

        // Supported meme image extensions
        private val MEMES_IMAGE_EXT = listOf(".jpg", ".png", ".webp")
    }

    /**
     * Use observer to get updates on the content of the memes folder.
     * Return a list of actual meme files on each update.
     */
    override fun getMemes(): Flow<List<MemeFile>> =
        callbackFlow {

            val observer = withContext(Dispatchers.IO) {

                val memesDirectory = File(context.getExternalFilesDir(null), MEMES_FOLDER)

                if (!memesDirectory.exists()) {
                    memesDirectory.mkdir()
                }

                // Start with sending actual file list
                send(memesDirectory.allMemeFiles())

                memesDirectory.startWatching {
                    trySend(memesDirectory.allMemeFiles())
                }
            }

            awaitClose { observer.stopWatching() }
        }

    override suspend fun delete(memes: List<MemeFile>): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            memes.map { File(it.path) }.onEach { file ->
                file.delete()
            }
        }
    }

    private fun File.allMemeFiles() =
        listFiles { _, path -> path?.isImagePath() ?: false }
            ?.map {
                MemeFile(
                    path = it.absolutePath,
                    lastModified = it.lastModified()
                )
            } ?: emptyList()

    private fun String.isImagePath(): Boolean =
        MEMES_IMAGE_EXT.any { endsWith(it) }
}

