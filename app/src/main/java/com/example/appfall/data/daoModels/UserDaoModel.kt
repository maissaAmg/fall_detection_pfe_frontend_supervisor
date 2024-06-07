package com.example.appfall.data.daoModels

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserDaoModel(
    val name: String,
    @PrimaryKey
    val phone: String,
    val token: String

)