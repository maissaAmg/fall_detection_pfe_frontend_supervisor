package com.example.appfall.data.models

import java.util.Date

data class Fall(
    val _id: String,
    val createdAt: String,
    val place: Place,
    val status: String,
    val dateTime: String
)