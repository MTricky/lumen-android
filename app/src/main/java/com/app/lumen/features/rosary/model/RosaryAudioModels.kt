package com.app.lumen.features.rosary.model

import kotlinx.serialization.Serializable

/**
 * Mirrors the Firestore `audio/{lang}` document structure.
 */
@Serializable
data class RosaryAudioConfig(
    val id: String = "",
    val version: Int = 0,
    val prayers: Map<String, List<RosaryAudioFile>> = emptyMap(),
    val mysteries: Map<String, List<RosaryAudioFile?>> = emptyMap(),
    val files: List<RosaryAudioFile> = emptyList(),
    val totalSize: Int = 0,
    val totalFiles: Int = 0,
) {
    /** Full audio requires both prayers and mysteries (announcements for all 4 sets) */
    val hasFullAudio: Boolean get() = mysteries.isNotEmpty()
}

@Serializable
data class RosaryAudioFile(
    val storagePath: String,
    val downloadUrl: String,
    val size: Int,
    val variant: Int? = null,
)
