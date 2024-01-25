package com.example.wastesamaritanassignment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.botsheloramela.noteapp.ui.viewmodel.NotesViewModel
import com.example.wastesamaritanassignment.data.NotesDatabase
import com.example.wastesamaritanassignment.data.room.models.Note
import com.example.wastesamaritanassignment.ui.detail.NoteDetailsScreen
import com.example.wastesamaritanassignment.ui.home.NotesScreen
import com.example.wastesamaritanassignment.ui.images.ui.screens.CameraScreen
import com.example.wastesamaritanassignment.ui.theme.WasteSamaritanAssignmentTheme

class MainActivity : ComponentActivity() {

    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            NotesDatabase::class.java,
            "notes.db"
        ).build()
    }

    private val viewModel: NotesViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NotesViewModel(database.noteDao,applicationContext) as T
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(
                this, CAMERAX_PERMISSIONS, 0
            )
        }
        setContent {
            WasteSamaritanAssignmentTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App()
                }
            }
        }
    }

    @Composable
    fun App() {

        viewModel.loadNotes()

        val notes: List<Note> by viewModel.notes
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "NotesScreen") {
            composable("NotesScreen") {
                NotesScreen(
                    notes = notes,
                    navController = navController,
                    eventHandler = viewModel::onEvent
                )
            }
            composable("NoteDetailsScreen/{noteId}/{tempId}/{add}") { backStackEntry ->
                val arguments = requireNotNull(backStackEntry.arguments)
                val noteId = arguments.getString("noteId")
                val tempId = arguments.getString("tempId")
                val add=arguments.getBoolean("add")
                NoteDetailsScreen(
                    navController = navController,
                    eventHandler = viewModel::onEvent,
                    viewModel = viewModel,
                    noteId = noteId,
                    tempId = tempId,
                    addNote = add
                )
            }
            composable("CameraScreen") {
                CameraScreen()
            }
        }

    }
    private fun hasRequiredPermissions(): Boolean {
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val CAMERAX_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        )
    }

}



