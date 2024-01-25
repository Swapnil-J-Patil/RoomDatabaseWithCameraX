package com.example.wastesamaritanassignment.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.wastesamaritanassignment.data.room.models.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Upsert
    suspend fun upsertNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Update
    suspend fun update(note: Note)

    @Query("SELECT * FROM Note ORDER BY timestamp")
    fun getNotesOrderedByDateAdded(): Flow<List<Note>>

    @Query("SELECT * FROM Note ORDER BY title ASC")
    fun getNotesOrderedByTitle(): Flow<List<Note>>

    @Query("SELECT * FROM Note WHERE id = :noteId")
    fun getNoteById(noteId: String): Flow<Note?>

}