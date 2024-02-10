package com.example.wastesamaritanassignment.ui.viewmodel

import android.graphics.Bitmap
import com.example.wastesamaritanassignment.data.room.models.Note

sealed interface NotesEvent {
   // object SortNotes : NotesEvent
    data class DeleteNote(
        val note: Note
    ) : NotesEvent

    data class SaveNote(
        val title: String,
        val content: String,
        val remarks: String,
        val rating: Float,
        val ImageId: String,
    ) : NotesEvent

    data class UpdateNote(
        val noteId: String,
        val updatedTitle: String,
        val updatedContent: String,
        val updatedRemarks: String,
        val updatedRating: Float,
    ) : NotesEvent

}
