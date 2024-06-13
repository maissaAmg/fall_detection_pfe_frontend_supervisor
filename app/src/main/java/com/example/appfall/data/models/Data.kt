package com.example.appfall.data.models

data class Data(
    val details: List<Detail>,
    val total: Int, //total des chutes en entier
    val users: List<UserStats>
)