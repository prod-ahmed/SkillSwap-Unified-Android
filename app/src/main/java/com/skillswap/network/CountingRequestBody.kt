package com.skillswap.network

import java.io.IOException
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

/**
 * Simple RequestBody wrapper to report upload progress for in-memory bytes.
 */
class CountingRequestBody(
    private val bytes: ByteArray,
    private val contentType: MediaType,
    private val onProgress: (Int) -> Unit
) : RequestBody() {
    override fun contentType(): MediaType = contentType
    override fun contentLength(): Long = bytes.size.toLong()

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val total = contentLength().coerceAtLeast(1L)
        bytes.inputStream().use { input ->
            val source = input.source()
            var read: Long
            var uploaded: Long = 0
            val buffer = okio.Buffer()
            val bufferSize = 8_192L
            while (source.read(buffer, bufferSize).also { read = it } != -1L) {
                sink.write(buffer, read)
                uploaded += read
                val percent = ((uploaded * 100) / total).toInt().coerceIn(0, 100)
                onProgress(percent)
            }
        }
        onProgress(100)
    }
}
