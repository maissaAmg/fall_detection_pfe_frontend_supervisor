package com.example.appfall.data.models

data class DailyFallsResponse(
    val data: List<Day>,
    val status: String
)