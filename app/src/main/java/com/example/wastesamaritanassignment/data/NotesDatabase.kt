package com.example.wastesamaritanassignment.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.wastesamaritanassignment.data.room.models.Note

@Database(entities = [Note::class], version = 1)
abstract class NotesDatabase: RoomDatabase() {
    abstract val noteDao: NoteDao
}