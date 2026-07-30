package com.viora.wallet.ui

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viora.wallet.ui.theme.SophisticatedDarkBg
import com.viora.wallet.ui.theme.SophisticatedSurface
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun DocumentPickerApp(viewModel: WalletViewModel, onImagePicked: (Uri) -> Unit, onCancel: () -> Unit) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (!isAuthenticated) {
                val context = LocalContext.current
                val activity = context as? androidx.fragment.app.FragmentActivity
                LoginScreen(viewModel = viewModel, activity = activity)
                BackHandler { onCancel() }
            } else {
                DocumentPickerScreen(viewModel = viewModel, onImagePicked = onImagePicked, onCancel = onCancel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentPickerScreen(viewModel: WalletViewModel, onImagePicked: (Uri) -> Unit, onCancel: () -> Unit) {
    val cards by viewModel.allCards.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    BackHandler { onCancel() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("انتخاب تصویر مدرک", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SophisticatedDarkBg)
            )
        },
        containerColor = SophisticatedDarkBg
    ) { innerPadding ->
        if (cards.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("هیچ مدرکی یافت نشد", color = Color.Gray, fontSize = 18.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(cards) { card ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = card.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Front Image
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("تصویر روی مدرک", color = Color.Gray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (card.frontImageBase64 != null) {
                                        val frontBitmap = remember(card.frontImageBase64) { base64ToBitmap(card.frontImageBase64) }
                                        if (frontBitmap != null) {
                                            Image(
                                                bitmap = frontBitmap.asImageBitmap(),
                                                contentDescription = "Front",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(100.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        coroutineScope.launch {
                                                            try {
                                                                val file = File(context.cacheDir, "shared_front_${card.id}.jpg")
                                                                val decodedBytes = Base64.decode(card.frontImageBase64, Base64.DEFAULT)
                                                                FileOutputStream(file).use { it.write(decodedBytes) }
                                                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                                onImagePicked(uri)
                                                            } catch (e: Exception) {
                                                                android.widget.Toast.makeText(context, "خطا در آماده‌سازی تصویر", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(100.dp).background(Color.DarkGray.copy(alpha=0.3f), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("ندارد", color = Color.Gray, fontSize = 12.sp)
                                        }
                                    }
                                }

                                // Back Image
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("تصویر پشت مدرک", color = Color.Gray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (card.backImageBase64 != null) {
                                        val backBitmap = remember(card.backImageBase64) { base64ToBitmap(card.backImageBase64) }
                                        if (backBitmap != null) {
                                            Image(
                                                bitmap = backBitmap.asImageBitmap(),
                                                contentDescription = "Back",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(100.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        coroutineScope.launch {
                                                            try {
                                                                val file = File(context.cacheDir, "shared_back_${card.id}.jpg")
                                                                val decodedBytes = Base64.decode(card.backImageBase64, Base64.DEFAULT)
                                                                FileOutputStream(file).use { it.write(decodedBytes) }
                                                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                                onImagePicked(uri)
                                                            } catch (e: Exception) {
                                                                android.widget.Toast.makeText(context, "خطا در آماده‌سازی تصویر", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(100.dp).background(Color.DarkGray.copy(alpha=0.3f), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("ندارد", color = Color.Gray, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
