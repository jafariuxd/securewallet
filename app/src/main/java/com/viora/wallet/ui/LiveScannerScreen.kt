package com.viora.wallet.ui

import android.content.Context
import com.viora.wallet.data.WalletCard
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun LiveScannerScreen(viewModel: WalletViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isProcessing by remember { mutableStateOf(false) }
    var isCardDetected by remember { mutableStateOf(false) }
    var detectionStartTime by remember { mutableStateOf<Long?>(null) }
    var lastDetectionTime by remember { mutableStateOf<Long?>(null) }
    var latestValidExtractedInfo by remember { mutableStateOf<ExtractedCardInfo?>(null) }

    BackHandler {
        val targetScreen = if ((viewModel.selectedCard.value?.id ?: 0) == 0) AppScreen.ADD_EDIT_CARD else AppScreen.EDIT_CARD
        viewModel.navigateTo(targetScreen)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                val executor = ContextCompat.getMainExecutor(ctx)
                
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    
                    val analyzerExecutor = Executors.newSingleThreadExecutor()
                    
                    imageAnalyzer.setAnalyzer(analyzerExecutor) { imageProxy ->
                        if (isProcessing) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                            
                            textRecognizer.process(inputImage)
                                .addOnSuccessListener { visionText ->
                                    if (isProcessing) return@addOnSuccessListener
                                    
                                    val allText = visionText.text
                                    val cardType = viewModel.selectedCard.value?.cardType ?: "BANK_CARD"
                                    val extractedInfo = parseCardInfoFromText(allText, cardType)
                                    val isValid = isValidExtraction(extractedInfo, cardType)
                                    
                                    var capturedImageBase64: String? = null
                                    if (isValid) {
                                        try {
                                            val bitmap = imageProxy.toBitmap()
                                            val outputStream = java.io.ByteArrayOutputStream()
                                            val scaledBitmap = if (bitmap.width > 1200 || bitmap.height > 1200) {
                                                val scale = 1200f / maxOf(bitmap.width, bitmap.height)
                                                Bitmap.createScaledBitmap(
                                                    bitmap,
                                                    (bitmap.width * scale).toInt(),
                                                    (bitmap.height * scale).toInt(),
                                                    true
                                                )
                                            } else {
                                                bitmap
                                            }
                                            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                                            val compressedBytes = outputStream.toByteArray()
                                            capturedImageBase64 = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.DEFAULT)
                                        } catch (e: Exception) {
                                            Log.e("LiveScanner", "Failed to capture bitmap", e)
                                        }
                                    }
                                    
                                    CoroutineScope(Dispatchers.Main).launch {
                                        if (isProcessing) return@launch
                                        
                                        if (isValid) {
                                            isProcessing = true
                                            isCardDetected = true
                                            val currentCard = viewModel.selectedCard.value
                                            
                                            val updatedCard = currentCard?.copy(
                                                title = if (!extractedInfo.title.isNullOrEmpty() && currentCard.title.isBlank()) extractedInfo.title else currentCard.title,
                                                cardNumber = if (extractedInfo.cardNumber?.isNotEmpty() == true) extractedInfo.cardNumber else currentCard.cardNumber,
                                                secondNumber = if (extractedInfo.secondNumber?.isNotEmpty() == true) extractedInfo.secondNumber else currentCard.secondNumber,
                                                expiryDate = if (extractedInfo.expiryDate?.isNotEmpty() == true) extractedInfo.expiryDate else currentCard.expiryDate,
                                                shebaNumber = if (extractedInfo.shebaNumber?.isNotEmpty() == true) extractedInfo.shebaNumber else currentCard.shebaNumber,
                                                frontImageBase64 = capturedImageBase64 ?: currentCard.frontImageBase64
                                            ) ?: WalletCard(
                                                title = extractedInfo.title ?: "", cardType = cardType, ownerName = "", 
                                                cardNumber = extractedInfo.cardNumber, 
                                                secondNumber = extractedInfo.secondNumber, 
                                                expiryDate = extractedInfo.expiryDate,
                                                shebaNumber = extractedInfo.shebaNumber, accountNumber = "",
                                                frontImageBase64 = capturedImageBase64, backImageBase64 = null, timestamp = 0L
                                            )
                                            
                                            val targetScreen = if ((updatedCard?.id ?: 0) == 0) AppScreen.ADD_EDIT_CARD else AppScreen.EDIT_CARD
                                            viewModel.selectCard(updatedCard)
                                            viewModel.navigateTo(targetScreen)
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview, imageAnalyzer
                        )
                    } catch (e: Exception) {
                        Log.e("CameraX", "Use case binding failed", e)
                    }
                }, executor)
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Guide
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.99f }) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val rectWidth = canvasWidth * 0.9f
            val rectHeight = canvasHeight * 0.30f
            val left = (canvasWidth - rectWidth) / 2
            val top = (canvasHeight - rectHeight) / 2

            drawRect(color = Color.Black.copy(alpha = 0.75f))
            
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(16.dp.toPx()),
                blendMode = BlendMode.Clear
            )
            
            // Draw border for the cutout: bright white when detected, very faint when not
            val borderColor = if (isCardDetected) Color.White else Color.White.copy(alpha = 0.15f)
            val strokeWidth = if (isCardDetected) 4.dp.toPx() else 1.5.dp.toPx()
            
            drawRoundRect(
                color = borderColor,
                topLeft = Offset(left, top),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { 
                    val targetScreen = if ((viewModel.selectedCard.value?.id ?: 0) == 0) AppScreen.ADD_EDIT_CARD else AppScreen.EDIT_CARD
                    viewModel.navigateTo(targetScreen) 
                },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            Text(
                text = "اسکن خودکار",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Text(
            text = "کارت را درون کادر قرار دهید.\nاطلاعات کارت به محض تشخیص استخراج خواهد شد.",
            color = Color.White,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .background(Color.Black.copy(alpha = 0.7f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .padding(16.dp)
        )
    }
}

fun isValidExtraction(info: ExtractedCardInfo, cardType: String): Boolean {
    return if (cardType == "BANK_CARD") {
        info.cardNumber != null && info.cardNumber.length == 16
    } else {
        info.cardNumber != null && info.cardNumber.length == 10
    }
}
