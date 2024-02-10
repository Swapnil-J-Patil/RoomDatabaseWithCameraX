package com.example.wastesamaritanassignment.ui.detail

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.botsheloramela.noteapp.ui.viewmodel.NotesViewModel
import com.example.wastesamaritanassignment.R
import com.example.wastesamaritanassignment.ui.images.ui.screens.CameraScreen
import com.example.wastesamaritanassignment.ui.speechrecognition.SpeechRecognitionContract
import com.example.wastesamaritanassignment.ui.speechrecognition.SpeechRecognizerContract
import com.example.wastesamaritanassignment.ui.theme.RatingBar
import com.example.wastesamaritanassignment.ui.viewmodel.NotesEvent
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.firstOrNull
import java.util.Locale
import kotlin.math.sqrt

private const val THRESHOLD = 30.0 // You may need to adjust this value based on testing
private const val STABLE_DELAY = 2000L // 5 seconds
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NoteDetailsScreen(
    navController: NavController,
    eventHandler: (NotesEvent) -> Unit,
    viewModel: NotesViewModel,
    noteId: String? = null,
    tempId: String?=null,
    addNote:Boolean
) {
    // Use state for note input
    val titleState = remember { mutableStateOf("") }
    val quantityState = remember { mutableStateOf("") }
    val remarksState= remember { mutableStateOf("")  }
    val ratingState = remember { mutableStateOf(0f) }
    val context = LocalContext.current
    val bitmaps by viewModel.getBitmapsStateFlow().collectAsState()
    val isBottomSheetVisible = remember { mutableStateOf(false) }
    var imageId: String by remember {
        mutableStateOf("")
    }
    var isErrorForTitle by rememberSaveable { mutableStateOf(false) }
    var isErrorForQuantity by rememberSaveable { mutableStateOf(false) }
    var isCameraScreenLaunched by remember { mutableStateOf(false) }
    // LaunchedEffect to fetch existing note details if noteId is not null
    LaunchedEffect(noteId) {
        noteId?.let {
            val existingNoteDetails = viewModel.getNoteDetails(it).firstOrNull()
            if (existingNoteDetails != null) {
                titleState.value = existingNoteDetails.title
                quantityState.value = existingNoteDetails.quantity
                remarksState.value = existingNoteDetails.remarks
                ratingState.value=existingNoteDetails.rating
                imageId=existingNoteDetails.imageId
            }
        }
        val idToLoad = if (tempId != null && imageId == "") tempId else imageId
        viewModel.loadImages(idToLoad) { updatedImages ->
            viewModel.updateBitmaps(updatedImages)
        }
    }

    // Create an ActivityResultLauncher to start CameraScreen
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle the result from CameraScreen here
        if (result.resultCode == Activity.RESULT_OK) {
            isCameraScreenLaunched = false
            // Trigger image loading based on the result
            val data = result.data
            val imageId = data?.getStringExtra("NOTE_ID")
            viewModel.loadImages(imageId!!) { updatedImages ->
                viewModel.updateBitmaps(updatedImages)
            }
        }
    }

    //Motion detection
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

                //for back button
                if (recognizedText.contains("ಹಿಂದೆ")) {
                    navController.popBackStack()
                }
                //for saving note
                if (recognizedText.contains("ಉಳಿಸಿ")) {
                    Log.d("noteID", "This is note id: $noteId")
                    Log.d("noteID", "This is temp id: $tempId")
                    isErrorForTitle = if(titleState.value=="") {
                        true
                    } else {
                        false

                    }
                    isErrorForQuantity = if(quantityState.value=="") {
                        true
                    } else {
                        false
                    }
                    if(ratingState.value==0f)
                    {
                        Toast.makeText(context, "Give Rating to Item", Toast.LENGTH_LONG).show()
                    }
                    if (noteId != "null" && !isErrorForTitle && !isErrorForQuantity && ratingState.value!=0f) {
                        Log.d("noteID", "Condition 1 - Save is executed")
                        eventHandler(
                            NotesEvent.UpdateNote(
                                noteId = noteId!!,
                                updatedTitle = titleState.value,
                                updatedContent = quantityState.value,
                                updatedRemarks = remarksState.value,
                                updatedRating = ratingState.value,
                            )
                        )
                        navController.popBackStack()

                    }
                    else if(noteId=="null" && !isErrorForTitle && !isErrorForQuantity && ratingState.value!=0f) {
                        Log.d("noteID", "Condition 2 - Update is executed")
                        eventHandler(
                            NotesEvent.SaveNote(
                                title = titleState.value,
                                content = quantityState.value,
                                rating = ratingState.value,
                                remarks = remarksState.value,
                                ImageId = tempId ?: ""
                            )
                        )
                        navController.popBackStack()
                    }
                }
                //for navigating to camera screen
                if (recognizedText.contains("ಫೋಟೋ")) {
                    if (noteId != "null") {
                        // Start CameraScreen for result
                        cameraLauncher.launch(
                            Intent(context, CameraScreen::class.java)
                                .putExtra("NOTE_ID", imageId)
                        )
                        isBottomSheetVisible.value = false
                        isCameraScreenLaunched = true // Set the flag
                    } else {
                        // Start CameraScreen for result
                        cameraLauncher.launch(
                            Intent(context, CameraScreen::class.java)
                                .putExtra("NOTE_ID", tempId)
                        )
                        isBottomSheetVisible.value = false
                        isCameraScreenLaunched = true // Set the flag
                    }
                }

                //for title
                if (recognizedText.contains("ಹೆಸರು")) {
                    // Extract the quantity amount from the recognized text
                    val titleName = extractWords(recognizedText)

                    // Update the titleState with the extracted title name
                    titleState.value = titleName
                }

                //for Remarks
                if (recognizedText.contains("ಟೀಕೆಗಳು")) {
                    // Extract the quantity amount from the recognized text
                    val remarks = extractWords(recognizedText)

                    // Update the titleState with the extracted title name
                    remarksState.value = remarks
                }
            }
        }
    )
    val speechRecognizerLauncherEnglish = rememberLauncherForActivityResult(
        contract = SpeechRecognizerContract(),
        onResult = { result ->
            result?.let {
                val recognizedText = it[0].toLowerCase(Locale.getDefault())

                //for Rating
                if (recognizedText.contains("rating")) {
                    // Extract the quantity amount from the recognized text
                    val rating = extractWords(recognizedText)

                    // Update the titleState with the extracted title name
                    if(rating!="") {
                        ratingState.value = rating.toFloat()
                    }
                }

                //for quantity
                if (recognizedText.contains("quantity")) {
                    // Extract the quantity amount from the recognized text
                    val quantityAmount = extractWords(recognizedText)

                    // Update the quantityState with the extracted quantity amount
                    if(quantityAmount!=null) {
                        quantityState.value = quantityAmount
                    }
                }

                // to delete images
                if (recognizedText.contains("delete")) {
                    val index = extractWords(recognizedText)
                    val i= extractNumber(index).toIntOrNull()
                    Log.d("image", "This is image index:$i")
                    // If a valid index is extracted, navigate to the details screen of the corresponding note
                    if (i != null && i >= 0 && i < bitmaps.size) {
                        if(tempId!="null") {
                            viewModel.deleteImage(tempId!!, bitmaps[i])
                        }
                        else {
                            viewModel.deleteImage(imageId, bitmaps[i])
                        }
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
                            if (!isCameraScreenLaunched) {
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
            if (!isCameraScreenLaunched) {
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
                            if (!isCameraScreenLaunched) {
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
            if (!isCameraScreenLaunched) {
            sensorManager?.unregisterListener(proximityListener)}
        }
    }
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    Log.d("noteID", "This is note id: $noteId")
                    Log.d("noteID", "This is temp id: $tempId")
                    isErrorForTitle = if(titleState.value=="") {
                        true
                    } else {
                        false

                    }
                    isErrorForQuantity = if(quantityState.value=="") {
                        true
                    } else {
                        false
                    }
                    if(ratingState.value==0f)
                    {
                        Toast.makeText(context, "Give Rating to Item", Toast.LENGTH_LONG).show()
                    }
                    if (noteId != "null" && !isErrorForTitle && !isErrorForQuantity && ratingState.value!=0f) {
                        Log.d("noteID", "Condition 1 - Save is executed")
                        eventHandler(
                            NotesEvent.UpdateNote(
                                noteId = noteId!!,
                                updatedTitle = titleState.value,
                                updatedContent = quantityState.value,
                                updatedRemarks = remarksState.value,
                                updatedRating = ratingState.value,
                            )
                        )
                        navController.popBackStack()

                    }
                    else if(noteId=="null" && !isErrorForTitle && !isErrorForQuantity && ratingState.value!=0f) {
                        Log.d("noteID", "Condition 2 - Update is executed")
                        eventHandler(
                            NotesEvent.SaveNote(
                                title = titleState.value,
                                content = quantityState.value,
                                rating = ratingState.value,
                                remarks = remarksState.value,
                                ImageId = tempId ?: ""
                            )
                        )
                        navController.popBackStack()
                    }
                },
                containerColor = colorResource(id = R.color.darkGreen)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Save,
                    contentDescription = "Save Note",
                    modifier = Modifier.size(35.dp),
                    tint = colorResource(id = R.color.black)
                )
            }
        }
    ){ paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)

        ) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 16.dp, 0.dp, 8.dp),
                horizontalArrangement = Arrangement.Center) {
                FloatingActionButton(
                    onClick = {
                        if (noteId != "null") {
                            // Start CameraScreen for result
                            cameraLauncher.launch(
                                Intent(context, CameraScreen::class.java)
                                    .putExtra("NOTE_ID", imageId)
                            )
                            isBottomSheetVisible.value = false
                            isCameraScreenLaunched = true // Set the flag

                        } else {
                            // Start CameraScreen for result
                            cameraLauncher.launch(
                                Intent(context, CameraScreen::class.java)
                                    .putExtra("NOTE_ID", tempId)
                            )
                            isBottomSheetVisible.value = false
                            isCameraScreenLaunched = true // Set the flag

                        }
                    },
                    modifier = Modifier
                        .size(100.dp),
                    containerColor = colorResource(id = R.color.darkGreen),
                    shape = CircleShape

                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img),
                        contentDescription = "Photo",
                        colorFilter = ColorFilter.tint(Color.Black),
                        contentScale = ContentScale.FillBounds
                    )
                }
            }

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                label = { Text(text = "Item Name",
                    color = Color.Black) },
                value = titleState.value,
                onValueChange = {
                    if (it.length <= 15) {
                        titleState.value = it
                    }
                },
                isError = isErrorForTitle,
                supportingText = {
                    if (isErrorForTitle) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "This field cannot be empty",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                trailingIcon = {
                    if (isErrorForTitle)
                        Icon(Icons.Filled.Error,"error", tint = MaterialTheme.colorScheme.error)},
                textStyle = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Color.Black),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = colorResource(id = R.color.darkGreen),
                    unfocusedBorderColor = colorResource(id = R.color.darkGreen),
                    focusedLabelColor = colorResource(id = R.color.darkGreen),
                    cursorColor = colorResource(id = R.color.darkGreen)
                ),
                visualTransformation = if (titleState.value.length > 15) {
                    VisualTransformation.None
                } else {
                    VisualTransformation.None
                },
                singleLine = true
            )

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                label = { Text("Quantity",color = Color.Black) },
                value = quantityState.value,
                onValueChange = {
                    quantityState.value = it
                },
                isError = isErrorForQuantity,
                supportingText = {
                    if (isErrorForQuantity) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "This field cannot be empty",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                trailingIcon = {
                    if (isErrorForQuantity)
                        Icon(Icons.Filled.Error,"error", tint = MaterialTheme.colorScheme.error)},
                textStyle = TextStyle(fontWeight = FontWeight.Medium, fontSize = 17.sp, color = Color.Black),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = colorResource(id = R.color.darkGreen),
                    unfocusedBorderColor = colorResource(id = R.color.darkGreen),
                    focusedLabelColor = colorResource(id = R.color.darkGreen),
                    cursorColor = colorResource(id = R.color.darkGreen)
                ),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                label = { Text("Remarks",color = Color.Black) },
                value = remarksState.value,
                onValueChange = {
                    if (it.length <= 200) {
                        remarksState.value = it
                    }
                },
                textStyle = TextStyle( fontSize = 17.sp, color = Color.Black),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = colorResource(id = R.color.darkGreen),
                    unfocusedBorderColor = colorResource(id = R.color.darkGreen),
                    focusedLabelColor = colorResource(id = R.color.darkGreen),
                    cursorColor = colorResource(id = R.color.darkGreen)
                ),
                visualTransformation = if (remarksState.value.length > 200) {
                    VisualTransformation.None
                } else {
                    VisualTransformation.None
                },
                maxLines = 3
            )
            Column {
                Text(text = "Rating: ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(12.dp))
                RatingBar(
                    rating = ratingState.value,
                    onRatingChanged = {
                        ratingState.value = it.toFloat()
                    },
                    modifier = Modifier.padding(12.dp,0.dp,0.dp,8.dp)
                )
                if(tempId!="null") {
                    PhotoBottomSheetContent(
                        bitmaps = bitmaps,
                        onDeleteImage = { deletedBitmap ->
                            viewModel.deleteImage(tempId!!, deletedBitmap)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .size(220.dp)
                    )
                }
                else
                {
                    PhotoBottomSheetContent(
                        bitmaps = bitmaps,
                        onDeleteImage = { deletedBitmap ->
                            viewModel.deleteImage(imageId, deletedBitmap)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .size(220.dp)
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
fun PhotoBottomSheetContent(
    bitmaps: List<Bitmap>,
    onDeleteImage: (Bitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    if(bitmaps.isEmpty()) {
         Box(
             modifier = modifier
                 .padding(16.dp),
             contentAlignment = Alignment.Center
         ) {
             Text(text = "No images taken.",
                 fontWeight = FontWeight.Bold,
                 fontSize = 20.sp)
         }
     } else {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier
    ) {
        itemsIndexed(bitmaps) { index,bitmap ->
                Box() {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Text(
                        text = "Image: ${index}",
                        fontSize = 20.sp,
                        color = Color.White,
                        modifier = Modifier.padding(5.dp)
                            .align(Alignment.TopCenter)
                    )
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Take photo",
                        modifier = Modifier
                            .size(35.dp)
                            .align(Alignment.BottomCenter)
                            .clickable {
                                onDeleteImage(bitmap)
                            },
                        tint = Color.Red
                    )
                }
            }
        }
    }
}


