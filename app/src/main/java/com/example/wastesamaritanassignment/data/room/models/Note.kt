package com.example.wastesamaritanassignment.data.room.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Note(
    val title: String,
    val quantity: String,
    val remarks: String,
    val timestamp: Long,
    val rating: Float,
    val imageId: String,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
)
