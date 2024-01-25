package com.example.wastesamaritanassignment.ui.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class CameraViewModel(private val context: Context,private val noteID: String?) : ViewModel() {

    // Use this context throughout the ViewModel
    //private val applicationContext: Context = context.applicationContext

    private val _bitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
   val bitmaps = _bitmaps.asStateFlow()
   private val _isSavingImage = MutableStateFlow(false)
    val isSavingImage = _isSavingImage.asStateFlow()

    fun onTakePhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            _bitmaps.value += bitmap
            saveImage(bitmap)
        }
    }

    private fun saveImage(bitmap: Bitmap) {
        try {
            val noteId=noteID
            viewModelScope.launch {
                _isSavingImage.value = true // Set the state to true when saving starts
                val file = File(context.filesDir, "captured_image_${noteId}_${System.currentTimeMillis()}.png")
                val stream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 50, stream)
                stream.flush()
                stream.close()
                Log.d("SaveImage", "Saved image with file name: $file")
                _isSavingImage.value = false // Set the state to false when saving is complete
            }

        } catch (e: IOException) {
            e.printStackTrace()
            _isSavingImage.value = false // Set the state to false when saving is complete
        }
    }

}
class MainViewModelFactory(private val context: Context, private val noteID: String?) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CameraViewModel(context, noteID) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
