package com.example.wastesamaritanassignment.ui.images.ui.screens

//@file:OptIn(ExperimentalMaterial3Api::class)

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.wastesamaritanassignment.R
import com.example.wastesamaritanassignment.ui.images.CameraViewModel
import com.example.wastesamaritanassignment.ui.images.MainViewModelFactory

class CameraScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraScreenApp()
        }
    }

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
            MainViewModelFactory(applicationContext,noteID)
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
        Scaffold(
            // ... rest of your Scaffold parameters
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
                    FloatingActionButton(onClick = {
                       // controller.unbind()
                        this@CameraScreen.finish()
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    if (!isButtonVisible) {
                        Text(
                            text = "Wait! Saving Image...",
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

  /*  override fun onStop() {
        super.onStop()
        controller.unbind()

    }
    override fun onDestroy() {
        super.onDestroy()
        // Release CameraX resources when the activity is destroyed
        controller.unbind()
    }*/
}

