package com.example.android_cnn_cv

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.android_cnn_cv.ui.theme.Android_CNN_CVTheme
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.ImageDecoder
import android.os.Build
import androidx.annotation.RequiresApi

lateinit var module: Module

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Android_CNN_CVTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CameraScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }

        // Load the PyTorch model
        try {
            val modelPath = assetFilePath("mobile_classification_model.ptl")
            Log.d("Model", "Model path: $modelPath")
            module = Module.load(modelPath)
            Log.d("PyTorch", "Model loaded successfully")
        } catch (e: IOException) {
            Log.e("PyTorch", "Error loading model", e)
        } catch (e: Exception) {
            Log.e("PyTorch", "Unexpected error loading model", e)
        }
    }

    private fun assetFilePath(assetName: String): String {
        val file = File(filesDir, assetName)
        assets.open(assetName).use { inputStream ->
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return file.absolutePath
    }

    @Composable
    fun CameraScreen(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        var previewUseCase by remember { mutableStateOf<androidx.camera.core.Preview?>(null) }
        var previewView by remember { mutableStateOf<PreviewView?>(null) }
        var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }
        val executor = remember { Executors.newSingleThreadExecutor() }
        var debugText by remember { mutableStateOf("Press the button to capture an image") }
        var predictionLabel by remember { mutableStateOf("Esperando la prediccion...") }
        var showCamera by remember { mutableStateOf(true) }
        var showCapturedImage by remember { mutableStateOf(false) }
        var capturedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var captureButtonText by remember { mutableStateOf("Capture Image") }

        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                Log.d("CameraX", "Camera permission granted")
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    previewView = PreviewView(context)
                    Log.d("CameraX", "PreviewView created")

                    val preview = androidx.camera.core.Preview.Builder().build().also { builder ->
                        builder.surfaceProvider = previewView!!.surfaceProvider
                        Log.d("CameraX", "SurfaceProvider set")
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    imageCaptureUseCase = ImageCapture.Builder().build()
                    Log.d("CameraX", "ImageCapture use case set up")

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            context as ComponentActivity, cameraSelector, preview, imageCaptureUseCase
                        )
                        previewUseCase = preview
                        Log.d("CameraX", "Camera bound to lifecycle")
                    } catch (exc: Exception) {
                        Log.e("CameraX", "Use case binding failed", exc)
                        debugText = "Use case binding failed: ${exc.message}"
                    }
                } catch (exc: Exception) {
                    Log.e("CameraX", "Camera provider initialization failed", exc)
                    debugText = "Camera provider initialization failed: ${exc.message}"
                }
            } else {
                Log.e("CameraX", "Camera permission not granted")
                debugText = "Camera permission not granted"
            }
        }

        LaunchedEffect(Unit) {
            Log.d("CameraX", "Requesting camera permission")
            launcher.launch(Manifest.permission.CAMERA)
        }

        Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (showCamera) {
                if (previewView != null) {
                    AndroidView(factory = { previewView!! }, modifier = Modifier.weight(1f))
                } else {
                    Log.d("CameraX", "PreviewView is null")
                    debugText = "PreviewView is null"
                }
            }

            capturedImageBitmap?.let { bitmap ->
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.weight(1f))
            }

            Text(text = debugText, modifier = Modifier.padding(all = Dp(16.0F)))
            Text(text = predictionLabel, modifier = Modifier.padding(all = Dp(16.0F)))

            if (captureButtonText == "Capture Image") {
                Button(onClick = {
                    Log.d("CameraX", "Capture Image button clicked")
                    debugText = "Button clicked, capturing image..."
                    captureImage(imageCaptureUseCase, executor,
                        updateDebugText = { message -> debugText = message },
                        updatePredictionLabel = { label -> predictionLabel = label },
                        updateCapturedImage = { bitmap ->
                            capturedImageBitmap = bitmap
                            showCamera = false
                            showCapturedImage = true
                            captureButtonText = "Retake"
                        }
                    )
                }) {
                    Text(captureButtonText)
                }
            } else {
                Button(onClick = {
                    showCamera = true
                    showCapturedImage = false
                    capturedImageBitmap = null
                    captureButtonText = "Capture Image"
                    debugText = "Press the button to capture an image"
                    predictionLabel = ""
                }) {
                    Text("Retake")
                }

                Button(onClick = {
                    // Redirect to another view with the predictionLabel
                }) {
                    Text("Go to Prediction View")
                }
            }
        }
    }

    private fun captureImage(
        imageCapture: ImageCapture?,
        executor: java.util.concurrent.Executor,
        updateDebugText: (String) -> Unit,
        updatePredictionLabel: (String) -> Unit,
        updateCapturedImage: (Bitmap) -> Unit
    ) {
        if (imageCapture == null) {
            val errorMessage = "ImageCapture use case is null"
            Log.e("CameraX", errorMessage)
            updateDebugText(errorMessage)
            return
        }

        Log.d("CameraX", "Starting image capture")
        updateDebugText("Starting image capture")
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                Log.d("CameraX", "Image captured successfully")
                updateDebugText("Image captured successfully")
                try {
                    // Convert ImageProxy to Bitmap using ImageDecoder
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        imageProxyToBitmap(image)
                    } else {
                        // Fallback for older versions if needed
                        val buffer: ByteBuffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                    Log.d("CameraX", "Converted ImageProxy to Bitmap")
                    updateDebugText("Converted ImageProxy to Bitmap")
                    updateCapturedImage(bitmap) // Update UI with captured image

                    // Create a mutable copy of the bitmap with ARGB_8888 configuration
                    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    Log.d("CameraX", "Copied Bitmap to mutable bitmap with ARGB_8888 configuration")

                    // Run inference on a background thread
                    executor.execute {
                        try {
                            val resizedBitmap = Bitmap.createScaledBitmap(mutableBitmap, 100, 100, true)
                            Log.d("CameraX", "Resized Bitmap dimensions: ${resizedBitmap.width}x${resizedBitmap.height}")

                            val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                                resizedBitmap,
                                floatArrayOf(0.0f, 0.0f, 0.0f), // Mean normalization set to zero
                                floatArrayOf(1.0f, 1.0f, 1.0f)
                            )
                            Log.d("CameraX", "Input Tensor shape: ${inputTensor.shape().contentToString()}")

                            // Log the first few values to verify content
                            val inputTensorData = inputTensor.dataAsFloatArray
                            Log.d("CameraX", "Input Tensor first 10 values: ${inputTensorData.take(10).joinToString()}")

                            updateDebugText("Converted Bitmap to Tensor")

                            val outputTensor = module.forward(IValue.from(inputTensor)).toTensor()
                            Log.d("CameraX", "Model inference performed")
                            updateDebugText("Model inference performed")

                            val scores = outputTensor.dataAsFloatArray
                            Log.d("CameraX", "Output scores length: ${scores.size}")
                            val maxScoreIndex = scores.indices.maxByOrNull { scores[it] } ?: -1

                            if (maxScoreIndex != -1) {
                                val classes = arrayOf("aguacate", "ajo", "almendra india", "arandano", "arandano azul", "berenjena", "cacahuate", "cacao", "cafe", "calabacin", "calabaza", "calabaza amarga", "calabaza moscada", "camote", "caqui", "carambola", "cebolla", "cereza", "cereza negra", "chile", "ciruela", "ciruela de playa", "coco", "coliflor", "durazno", "durian", "espinaca", "frambuesa", "fresa", "granada", "granadilla", "granadina", "grosella negra", "guanabana", "guayaba", "guisante", "higo", "higo del desierto", "jalapeno", "jengibre", "jitomate", "jocote", "kiwano", "kiwi", "lechuga", "lima", "limon americano", "lychee", "maiz", "mamey", "mandarina", "mango", "manzana", "melon", "melon galia", "mora artica", "nabo", "naranja", "nuez de brazil", "papa", "papaya", "paprika", "pepino", "pera", "pera yali", "pimiento", "pina", "pitahaya", "platano", "pomelo", "rabano", "remolacha", "repollo", "sandia", "soja", "toronja", "tuna", "uva", "vainilla", "zanahoria", "zarzamora")
                                val detectedClass = classes[maxScoreIndex]
                                Log.d("PyTorch", "Detected class: $detectedClass")
                                updatePredictionLabel(detectedClass)
                            }

                            image.close()
                            Log.d("CameraX", "Image processed successfully")
                            updateDebugText("Image processed successfully")
                        } catch (e: Exception) {
                            Log.e("CameraX", "Failed to process image", e)
                            updateDebugText("Failed to process image: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CameraX", "Failed to process image", e)
                    updateDebugText("Failed to process image: ${e.message}")
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                updateDebugText("Photo capture failed: ${exception.message}")
            }
        })
    }


    @RequiresApi(Build.VERSION_CODES.P)
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
        return ImageDecoder.decodeBitmap(source)
    }

}

@ComposePreview(showBackground = true)
@Composable fun CameraScreenPreview() {
    Android_CNN_CVTheme {
        MainActivity().CameraScreen()
    }
}
