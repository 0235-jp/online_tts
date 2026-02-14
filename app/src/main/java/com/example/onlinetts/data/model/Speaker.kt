package com.example.onlinetts.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Speaker(
    val name: String,
    val speakerUuid: String = "",
    val styleId: Int,
    val styleName: String = "",
)
