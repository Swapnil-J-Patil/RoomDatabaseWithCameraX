package com.botsheloramela.noteapp.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wastesamaritanassignment.data.NoteDao
import com.example.wastesamaritanassignment.data.room.models.Note
import com.example.wastesamaritanassignment.ui.speechrecognition.TextState
import com.example.wastesamaritanassignment.ui.viewmodel.NotesEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.IOException

class NotesViewModel(
    private val dao: NoteDao,
    private val context: Context
): ViewModel() {

    private val _notes: MutableState<List<Note>> = mutableStateOf(emptyList())
    val notes: State<List<Note>> by mutableStateOf(_notes)

    private val _bitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val bitmaps = _bitmaps.asStateFlow()

  /*  init {
        // Load previously saved images when the ViewModel is created
        loadImages()
    }*/
    fun getBitmapsStateFlow(): StateFlow<List<Bitmap>> = bitmaps
    fun updateBitmaps(updatedImages: List<Bitmap>) {
        _bitmaps.value = updatedImages
    }
    var state by mutableStateOf(TextState())
        private set

    // State for note input
    private val _titleState: MutableState<String> = mutableStateOf("")

    private val _contentState: MutableState<String> = mutableStateOf("")

    private val _remarkState: MutableState<String> = mutableStateOf("")

    private val _ratingState: MutableState<Float> = mutableStateOf(0f)

    private val isSortedByDateAdded: MutableState<Boolean> = mutableStateOf(true)

    // Event handling
    fun onEvent(event: NotesEvent) {
        when (event) {
            is NotesEvent.DeleteNote -> deleteNote(event.note)

            is NotesEvent.SaveNote -> saveNote(
                event.title,
                event.content,
                event.remarks,
                event.rating,
                event.ImageId)

            is NotesEvent.UpdateNote -> updateNote(
                event.noteId,
                event.updatedTitle,
                event.updatedContent,
                event.updatedRemarks,
                event.updatedRating
            )
           // NotesEvent.SortNotes -> isSortedByDateAdded.value = !isSortedByDateAdded.value
        }
    }

    private fun deleteNote(note: Note) {
        viewModelScope.launch {
            dao.deleteNote(note)
            deleteImages(note.imageId)
        }
    }
    fun getNoteDetails(noteId: String): Flow<Note?> {
        return dao.getNoteById(noteId)
    }

    private fun updateNote(
        noteId: String,
        updatedTitle: String,
        updatedContent: String,
        updatedRemarks: String,
        updatedRating: Float
    ) {
        viewModelScope.launch {
            if (!noteId.isNullOrBlank()) {
                val existingNote = dao.getNoteById(noteId).firstOrNull()

                existingNote?.let { nonNullExistingNote ->
                    val updatedNote = nonNullExistingNote.copy(
                        title = updatedTitle,
                        quantity = updatedContent,
                        remarks = updatedRemarks,
                        rating = updatedRating,
                        timestamp = System.currentTimeMillis()
                    )
                    dao.update(updatedNote)
                }
            }
        }
    }


    private fun saveNote(title: String, content: String,remarks:String,rating: Float,imageId: String) {
        val note = Note(
            title = title,
            quantity = content,
            remarks=remarks,
            rating=rating,
            timestamp = System.currentTimeMillis(),
            imageId = imageId
        )

        viewModelScope.launch {
            dao.upsertNote(note)
            // Clear input fields after saving
            _titleState.value = ""
            _contentState.value = ""
            _remarkState.value =""
            _ratingState.value=0f
        }
    }

    // Load notes based on sorting
    fun loadNotes() {
        viewModelScope.launch {
            val notesFlow = if (isSortedByDateAdded.value) {
                dao.getNotesOrderedByDateAdded()
            } else {
                dao.getNotesOrderedByTitle()
            }

            // Collect the Flow to get the List<Note>
            notesFlow.collect { notesList ->
                _notes.value = notesList
            }
        }
    }
    private fun deleteImages(noteId: String) {
        viewModelScope.launch {
            val files = context.filesDir.listFiles()

            files?.forEach { file ->
                // Check if the file name is associated with the specified note
                if (file.name.contains(noteId)) {
                    file.delete() // Delete the file
                }
            }
        }

    }
    fun deleteImage(noteId: String, deletedBitmap: Bitmap) {
        viewModelScope.launch {
            val files = context.filesDir.listFiles()
            files?.forEach { file ->
                // Check if the file name is associated with the specified note
                if (file.name.contains(noteId)) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null && compareBitmaps(bitmap, deletedBitmap)) {
                        file.delete() // Delete the file
                    }
                }
            }

            // Load updated images
            loadImages(noteId) { updatedImages ->
                // Update the state with the new list of images
                _bitmaps.value = updatedImages
            }
        }
    }
    private fun compareBitmaps(bitmap1: Bitmap, bitmap2: Bitmap): Boolean {
        // Implement bitmap comparison logic here
        // For example, you can compare their byte arrays or use other methods
        return bitmap1.sameAs(bitmap2)
    }
    fun loadImages(noteID: String, onImagesLoaded: (List<Bitmap>) -> Unit) {
        val images = mutableListOf<Bitmap>()
        viewModelScope.launch{
            val files = context.filesDir.listFiles()
            var noteId=noteID
            files?.forEach { file ->
                // Check if the file name is associated with the specified note
                if (file.name.contains(noteId)) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        images.add(bitmap)
                    }
                }
            }
        }

        onImagesLoaded(images)
    }

}
