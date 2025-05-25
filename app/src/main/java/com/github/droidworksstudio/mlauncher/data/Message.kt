package com.github.droidworksstudio.mlauncher.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Message(
    var text: String,
    var timestamp: String,
    var category: String,
    var priority: String
)