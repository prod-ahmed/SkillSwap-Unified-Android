package com.skillswap.model

/**
 * Represents a binary media payload selected locally before upload.
 */
data class MediaPayload(
    val bytes: ByteArray,
    val filename: String,
    val mimeType: String = "image/jpeg"
)
