package com.example.appfall.data.models

data class Detail(
    val active: Int, //total dans la semaine
    val `false`: Int,
    val rescued: Int,
    val week: Int //number of week in a month (n'amène pas toutes les semaines si la semaine == 0)
)