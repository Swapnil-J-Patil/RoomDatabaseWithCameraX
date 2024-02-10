package com.example.wastesamaritanassignment.ui.home

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.wastesamaritanassignment.R
import com.example.wastesamaritanassignment.data.room.converters.DateTimeUtil
import com.example.wastesamaritanassignment.data.room.models.Note
import com.example.wastesamaritanassignment.ui.speechrecognition.SpeechRecognitionContract
import com.example.wastesamaritanassignment.ui.speechrecognition.SpeechRecognizerContract
import com.example.wastesamaritanassignment.ui.viewmodel.NotesEvent
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import java.util.Locale
import kotlin.math.sqrt

private const val THRESHOLD = 30.0 // You may need to adjust this value based on testing
private const val STABLE_DELAY = 2000L // 5 seconds
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NotesScreen(
    notes: List<Note>,
    navController: NavController,
    eventHandler: (NotesEvent) -> Unit
) {
    var isDetailsScreenLaunched by remember { mutableStateOf(false) }
    //Motion detection
    val context: Context = LocalContext.current
    val permissionState = rememberPermissionState(
        permission = Manifest.permission.RECORD_AUDIO
    )
    SideEffect {
        permissionState.launchPermissionRequest()
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = SpeechRecognitionContract(),
        onResult = { result ->
            result?.let {
                val recognizedText = it[0].toLowerCase(Locale.getDefault())
                if (recognizedText.contains("ಹೊಸ")) {
                    isDetailsScreenLaunched=true
                    val add=true
                    navController.navigate("NoteDetailsScreen/${null}/${"TempID"+System.currentTimeMillis()}/${add}") }
            }
        }
    )
    val speechRecognizerLauncherEnglish = rememberLauncherForActivityResult(
        contract = SpeechRecognizerContract(),
        onResult = { result ->
            result?.let {
                val recognizedText = it[0].toLowerCase(Locale.getDefault())

                //to open a note
                if (recognizedText.contains("note")) {
                    val index = extractWords(recognizedText)
                    val i= extractNumber(index).toIntOrNull()
                    // If a valid index is extracted, navigate to the details screen of the corresponding note
                    if (i != null && i >= 0 && i < notes.size) {
                        val add = false
                        navController.navigate("NoteDetailsScreen/${notes[i].id}/${null}/${add}") {
                            launchSingleTop = true
                        }
                    }
                }

                //to delete a note
                if (recognizedText.contains("delete")) {
                    val index = extractWords(recognizedText)
                    val i= extractNumber(index).toIntOrNull()
                    Log.d("noteIndex", "This is note index:$i")
                    // If a valid index is extracted, navigate to the details screen of the corresponding note
                    if (i != null && i >= 0 && i < notes.size) {
                        eventHandler(NotesEvent.DeleteNote(notes[i]))
                    }
                }
            }
        }
    )
    DisposableEffect(context) {
        var lastShakeTime = 0L
        val sensorManager = ContextCompat.getSystemService(context, SensorManager::class.java)
        val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]

                    val acceleration = sqrt(x * x + y * y + z * z)

                    if (acceleration > THRESHOLD) {
                        // Phone is shaking
                        if (System.currentTimeMillis() - lastShakeTime > STABLE_DELAY) {
                            if(!isDetailsScreenLaunched) {
                                speechRecognizerLauncher.launch(Unit)
                            }
                        }
                        lastShakeTime = System.currentTimeMillis()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not needed for this example
            }
        }

        sensorManager?.registerListener(listener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)

        onDispose {
            // Unregister the sensor listener when the composable is disposed
            if(!isDetailsScreenLaunched) {
                sensorManager?.unregisterListener(listener)
            }
        }
    }
    DisposableEffect(context) {
        var lastProximityState = false
        val sensorManager = ContextCompat.getSystemService(context, SensorManager::class.java)
        val proximityThreshold = 5.0 // Adjust this value based on testing
        val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        val proximityListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                // Proximity sensor logic
                event?.let {
                    val distance = it.values[0]

                    if (distance < proximityThreshold) {
                        // Proximity sensor triggered
                        if (!lastProximityState) {
                            lastProximityState = true
                            // Load English speech recognition
                            if(!isDetailsScreenLaunched) {
                            speechRecognizerLauncherEnglish.launch(Unit)
                            }
                        }
                    } else {
                        lastProximityState = false
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not needed for this example
            }
        }

        sensorManager?.registerListener(
            proximityListener,
            proximitySensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        onDispose {
            // Unregister the sensor listener when the composable is disposed
            if(!isDetailsScreenLaunched) {
            sensorManager?.unregisterListener(proximityListener)
            }
        }
    }
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .background(colorResource(id = R.color.darkGreen))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    modifier = Modifier.weight(1f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )

               /* IconButton(onClick = { eventHandler(NotesEvent.SortNotes) }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sort,
                        contentDescription = "Sort notes",
                        modifier = Modifier.size(35.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }*/
            }
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val add=true
                    navController.navigate("NoteDetailsScreen/${null}/${"TempID"+System.currentTimeMillis()}/${add}") },
                containerColor = colorResource(id = R.color.darkGreen)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add note",
                    modifier = Modifier.size(30.dp),
                    tint = colorResource(id = R.color.black)
                )
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)

        ) {
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp,15.dp,5.dp,15.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(notes.size) { index ->
                    NoteCard(
                        note = notes[index],
                        onEvent = eventHandler,
                        onClick = {
                            // Navigate to the details screen when a note is clicked
                            val add=false
                            navController.navigate("NoteDetailsScreen/${notes[index].id}/${null}/${add}") {
                                launchSingleTop = true
                            }
                        },
                        index=index
                    )
                }
            }
        }
    }
}
private fun extractWords(text: String): String {
    val firstSpaceIndex = text.indexOf(' ')

    // Return the substring starting from the character after the first space
    return if (firstSpaceIndex != -1) {
        text.substring(firstSpaceIndex + 1).trim()
    } else {
        // Return empty string if there is no space (only one word)
        ""
    }
}
private fun extractNumber(text: String): String {
    Log.d("noteIndex", "This is note index:$text")

    val wordVariations = hashMapOf(
        "zero" to "0",
        "0" to "0",
        "one" to "1",
        "1" to "1",
        "to" to "2",
        "too" to "2",
        "two" to "2",
        "three" to "3",
        "3" to "3",
        "four" to "4",
        "for" to "4",
        "five" to "5",
        "file" to "5",
        "5" to "5",
        // Add more variations as needed
    )

    // Check if the text is present in the map, if not, return the original text
    val normalizedText = text.trim().toLowerCase()
    return wordVariations[normalizedText] ?: normalizedText
}

@Composable
fun NoteCard(
    note: Note,
    onEvent: (NotesEvent) -> Unit,
    onClick: () -> Unit,
    index: Int,
) {

    val formattedDate = remember(note.timestamp) {
        val localDateTime = DateTimeUtil.millisToLocalDateTime(note.timestamp)
        DateTimeUtil.formatNoteDate(localDateTime)
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(colorResource(id = R.color.darkGreen))
            .padding(16.dp)
            .clickable { onClick.invoke() },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Item: "+ note.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.background
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete note",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .clickable(MutableInteractionSource(), null) {
                        Log.d("noteIndex", "This is note index:$note")
                        onEvent(NotesEvent.DeleteNote(note))
                    }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Rating: " + note.rating.toString(),
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.background
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text ="Quantity: " + note.quantity,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.background
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text ="Remarks: " + note.remarks ,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.background
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Note Index: $index", // Display the index dynamically
            modifier = Modifier.align(Alignment.End),
            color = MaterialTheme.colorScheme.background
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = formattedDate,
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.align(Alignment.End)
        )
    }
}