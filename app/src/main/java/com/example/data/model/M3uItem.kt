package com.example.data.model

data class M3uItem(
    val title: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String = "Genel",
    val type: String = "LIVE",
    val tvgId: String? = null
)
