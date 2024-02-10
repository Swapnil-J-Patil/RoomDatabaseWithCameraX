package com.example.wastesamaritanassignment.ui.images.ui.screens

//@file:OptIn(ExperimentalMaterial3Api::class)

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognizerIntent
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.navigation.NavController
import com.example.wastesamaritanassignment.R
import com.example.wastesamaritanassignment.ui.images.CameraViewModel
import com.example.wastesamaritanassignment.ui.images.MainViewModelFactory
import com.example.wastesamaritanassignment.ui.speechrecognition.SpeechRecognitionContract
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

private const val THRESHOLD = 30.0 // You may need to adjust this value based on testing
private const val STABLE_DELAY = 2000L // 5 seconds
class CameraScreen() : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraScreenApp()
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun CameraScreenApp() {
        val intent = intent
// Get the data using getStringExtra (assuming it's a String)
        val noteID = intent.getStringExtra("NOTE_ID")

        val controller = remember {
            LifecycleCameraController(applicationContext).apply {
                setEnabledUseCases(
                    CameraController.IMAGE_CAPTURE or
                            CameraController.VIDEO_CAPTURE
                )
            }
        }
        // Obtain the ViewModel using viewModels
        val viewModel: CameraViewModel by viewModels {
            MainViewModelFactory(applicationContext, noteID)
        }
        // Collect the isSavingImage state as a State<Boolean>
        val isSavingImage by viewModel.isSavingImage.collectAsState()

        var isButtonVisible by remember { mutableStateOf(true) }

        LaunchedEffect(isSavingImage) {
            // If isSavingImage becomes true, hide the button
            if (isSavingImage) {
                isButtonVisible = false
            }

        }

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

                    //For taking photos
                    if (recognizedText.contains("ಫೋಟೋ")) {
                        isButtonVisible = false
                        takePhoto(
                            controller = controller,
                            onPhotoTaken = { bitmap ->
                                viewModel.onTakePhoto(bitmap)
                                // After photo is taken, show the button
                                isButtonVisible = true
                            }
                        )
                    }
                    //For back button
                    if (recognizedText.contains("ಹಿಂದೆ")) {
                        val resultIntent = Intent().apply {
                            // You can put additional data in the intent if needed
                            putExtra("NOTE_ID", noteID)
                        }

                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
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
                                speechRecognizerLauncher.launch(Unit)
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
                sensorManager?.unregisterListener(listener)
            }
        }

        Scaffold(

        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                CameraPreview(
                    controller = controller,
                    modifier = Modifier
                        .fillMaxSize()
                )

                IconButton(
                    onClick = {
                        controller.cameraSelector =
                            if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else CameraSelector.DEFAULT_BACK_CAMERA
                    },
                    modifier = Modifier
                        .align(AbsoluteAlignment.TopRight)
                        .padding(0.dp, 16.dp, 16.dp, 0.dp),

                    ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch camera",
                        tint = Color.White
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    if(isButtonVisible) {
                        FloatingActionButton(
                            onClick = {
// Set the result to be sent back to the calling activity
                                val resultIntent = Intent().apply {
                                    // You can put additional data in the intent if needed
                                    putExtra("NOTE_ID", noteID)
                                }

                                setResult(Activity.RESULT_OK, resultIntent)
                                finish()
                            },
                            containerColor = colorResource(id = R.color.darkGreen)
                        )
                        {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back Button",
                                modifier = Modifier.size(35.dp),
                                tint = MaterialTheme.colorScheme.background
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    if (!isButtonVisible) {
                        Text(
                            text = "Please Wait! Saving Image...",
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    } else if (isButtonVisible) {
                        FloatingActionButton(
                            onClick = {
                                // Hide the button when clicked and trigger the photo capture
                                isButtonVisible = false
                                takePhoto(
                                    controller = controller,
                                    onPhotoTaken = { bitmap ->
                                        viewModel.onTakePhoto(bitmap)
                                        // After photo is taken, show the button
                                        isButtonVisible = true
                                    }
                                )
                            },
                            containerColor = colorResource(id = R.color.darkGreen),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Take photo",
                                modifier = Modifier.size(35.dp),
                                tint = MaterialTheme.colorScheme.background
                            )
                        }
                    }
                }
            }
        }
    }

    private fun takePhoto(
        controller: LifecycleCameraController,
        onPhotoTaken: (Bitmap) -> Unit
    ) {
        controller.takePicture(
            ContextCompat.getMainExecutor(applicationContext),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                    }
                    val rotatedBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )

                    onPhotoTaken(rotatedBitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("Camera", "Couldn't take photo: ", exception)
                }
            }
        )
    }
}
