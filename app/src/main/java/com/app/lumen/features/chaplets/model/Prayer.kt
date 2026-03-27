package com.app.lumen.features.chaplets.model

import kotlinx.serialization.Serializable

@Serializable
data class Prayer(
    val title: String,
    val text: String,
)
