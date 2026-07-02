package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class Movie(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val posterUrl: String,
    val genre: String,
    val googleDriveFileId: String,
    val mirrorIds: String = "",
    val views: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "advertisements")
data class Advertisement(
    @PrimaryKey val id: String,
    val videoUrl: String,
    val clickThroughUrl: String,
    val title: String = "Sponsored Advertisement",
    val htmlCode: String = "",
    val isBanner: Boolean = false
)
