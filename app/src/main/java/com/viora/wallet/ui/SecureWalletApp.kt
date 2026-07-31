@file:OptIn(ExperimentalMaterial3Api::class)
package com.viora.wallet.ui
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

import androidx.compose.material3.ExperimentalMaterial3Api
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viora.wallet.data.WalletCard
import com.viora.wallet.ui.theme.SophisticatedDarkBg
import com.viora.wallet.ui.theme.SophisticatedSurface
import com.viora.wallet.ui.theme.SophisticatedSurfaceVariant
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

// Convert Base64 string to Bitmap
fun base64ToBitmap(base64Str: String?): Bitmap? {
    if (base64Str == null) return null
    return try {
        val decodedBytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}

// Convert Uri to Compressed Base64 String
fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        if (bytes != null) {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val outputStream = ByteArrayOutputStream()
            // Resize if too large
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
            android.util.Base64.encodeToString(compressedBytes, android.util.Base64.DEFAULT)
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Copy Text Helper
fun copyToClipboard(context: Context, text: String, label: String = "Card Info") {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "کپی شد: $text", Toast.LENGTH_SHORT).show()
}

fun isCardWithFrontBack(cardType: String): Boolean {
    return cardType == "BANK_CARD" || cardType == "NATIONAL_ID" || cardType == "MILITARY_CARD" || cardType == "DRIVERS_LICENSE" || cardType == "STUDENT_ID"
}

fun isMultiImageDocument(cardType: String): Boolean {
    return !isCardWithFrontBack(cardType)
}

fun buildExtraFieldsJson(
    cardType: String,
    fatherName: String = "",
    motherName: String = "",
    birthDate: String = "",
    issueDate: String = "",
    issuePlace: String = "",
    ownerEn: String = "",
    serviceStatus: String = "",
    militaryBranch: String = "",
    licenseType: String = "",
    bloodType: String = "",
    studyField: String = "",
    degreeLevel: String = "",
    cityProvince: String = "",
    fullAddress: String = "",
    notes: String = ""
): String? {
    val map = mutableMapOf<String, String>()
    when (cardType) {
        "NATIONAL_ID" -> {
            if (fatherName.isNotBlank()) map["father_name"] = fatherName
            if (birthDate.isNotBlank()) map["birth_date"] = birthDate
        }
        "SHENASNAMEH" -> {
            if (fatherName.isNotBlank()) map["father_name"] = fatherName
            if (motherName.isNotBlank()) map["mother_name"] = motherName
            if (birthDate.isNotBlank()) map["birth_date"] = birthDate
            if (issuePlace.isNotBlank()) map["issue_place"] = issuePlace
        }
        "PASSPORT" -> {
            if (ownerEn.isNotBlank()) map["owner_en"] = ownerEn
            if (issueDate.isNotBlank()) map["issue_date"] = issueDate
            if (issuePlace.isNotBlank()) map["issue_place"] = issuePlace
        }
        "MILITARY_CARD" -> {
            if (serviceStatus.isNotBlank()) map["service_status"] = serviceStatus
            if (militaryBranch.isNotBlank()) map["military_branch"] = militaryBranch
        }
        "DRIVERS_LICENSE" -> {
            if (licenseType.isNotBlank()) map["license_type"] = licenseType
            if (bloodType.isNotBlank()) map["blood_type"] = bloodType
        }
        "STUDENT_ID" -> {
            if (studyField.isNotBlank()) map["study_field"] = studyField
            if (degreeLevel.isNotBlank()) map["degree_level"] = degreeLevel
        }
        "POSTAL_ADDRESS" -> {
            if (cityProvince.isNotBlank()) map["city_province"] = cityProvince
            if (fullAddress.isNotBlank()) map["full_address"] = fullAddress
        }
        "OTHER" -> {
            if (notes.isNotBlank()) map["notes"] = notes
        }
    }
    if (map.isEmpty()) return null
    val json = org.json.JSONObject()
    map.forEach { (k, v) -> json.put(k, v) }
    return json.toString()
}

fun getDocumentTypeName(cardType: String): String {
    return when (cardType) {
        "BANK_CARD" -> "کارت بانکی"
        "NATIONAL_ID" -> "کارت ملی"
        "SHENASNAMEH" -> "شناسنامه"
        "PASSPORT" -> "پاسپورت / گذرنامه"
        "MILITARY_CARD" -> "کارت پایان خدمت"
        "DRIVERS_LICENSE" -> "گواهینامه رانندگی"
        "STUDENT_ID" -> "کارت دانشجویی"
        "POSTAL_ADDRESS" -> "آدرس پستی"
        else -> "سایر مدارک"
    }
}

fun getDocumentTypeIcon(cardType: String): ImageVector {
    return when (cardType) {
        "BANK_CARD" -> Icons.Default.CreditCard
        "NATIONAL_ID" -> Icons.Default.Badge
        "SHENASNAMEH" -> Icons.Default.Book
        "PASSPORT" -> Icons.Default.Flight
        "MILITARY_CARD" -> Icons.Default.Shield
        "DRIVERS_LICENSE" -> Icons.Default.DirectionsCar
        "STUDENT_ID" -> Icons.Default.School
        "POSTAL_ADDRESS" -> Icons.Default.Place
        else -> Icons.Default.Folder
    }
}

@Composable
fun SecureWalletApp(viewModel: WalletViewModel) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()

    // Force RTL for Persian Interface
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    val initialRank = initialState.ordinal
                    val targetRank = targetState.ordinal
                    val isForward = targetRank > initialRank
                    
                    if (isForward) {
                        // Forward transition in RTL: Slide in from Left, Slide out to Right
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth / 5 },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                        ) + fadeIn(
                            animationSpec = tween(220)
                        ) togetherWith slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth / 10 },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                        ) + fadeOut(
                            animationSpec = tween(180)
                        )
                    } else {
                        // Backward transition in RTL: Slide in from Right, Slide out to Left
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth / 5 },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                        ) + fadeIn(
                            animationSpec = tween(220)
                        ) togetherWith slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth / 10 },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                        ) + fadeOut(
                            animationSpec = tween(180)
                        )
                    }
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    AppScreen.LOGIN -> LoginScreen(viewModel, activity)
                    AppScreen.FIRST_TIME_SETUP -> FirstTimeSetupScreen(viewModel)
                    AppScreen.DASHBOARD -> DashboardScreen(viewModel)
                    AppScreen.CARD_DETAIL -> CardDetailScreen(viewModel)
                    AppScreen.ADD_EDIT_CARD -> AddEditCardScreen(viewModel)
                    AppScreen.EDIT_CARD -> EditCardScreen(viewModel)
                    AppScreen.SETTINGS -> SettingsScreen(viewModel)
                    AppScreen.PRIVACY_POLICY -> PrivacyPolicyScreen(viewModel)
                    AppScreen.TERMS -> TermsScreen(viewModel)
                    AppScreen.LIVE_SCANNER -> LiveScannerScreen(viewModel)
                }
            }
        }
    }
}

// --- SCREEN 1: LOGIN ---
@Composable
fun LoginScreen(viewModel: WalletViewModel, activity: FragmentActivity?) {
    var enteredPin by remember { mutableStateOf("") }
    val pinError by viewModel.pinError.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Trigger biometric on launch if enabled
    LaunchedEffect(Unit) {
        try {
            if (viewModel.isBiometricEnabled() && activity != null && viewModel.isBiometricHardwareAvailable()) {
                viewModel.securityManager.authenticateBiometric(
                    activity = activity,
                    onSuccess = {
                        viewModel.setAuthenticatedDirectly()
                    },
                    onError = { err ->
                        try {
                            Toast.makeText(context, "احراز هویت بیومتریک ناموفق بود", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SophisticatedDarkBg, Color(0xFF000000))
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Logo",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(54.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "کیف مدارک امن",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "اطلاعات شما به صورت کاملا آفلاین ذخیره می‌شوند",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

            // PIN Display Dots
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..4) {
                        val isFilled = enteredPin.length >= i
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                        )
                    }
                }
                if (pinError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = pinError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Numeric Keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("FINGER", "0", "BACK")
                )

                for (row in keys) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (key in row) {
                            if (key == "FINGER") {
                                if (viewModel.isBiometricEnabled() && viewModel.isBiometricHardwareAvailable()) {
                                    IconButton(
                                        onClick = {
                                            if (activity != null) {
                                                viewModel.securityManager.authenticateBiometric(
                                                    activity = activity,
                                                    onSuccess = { viewModel.setAuthenticatedDirectly() },
                                                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .size(68.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surface,
                                                CircleShape
                                            )
                                            .testTag("biometric_login_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Fingerprint Authentication",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(68.dp))
                                }
                            } else if (key == "BACK") {
                                IconButton(
                                    onClick = {
                                        if (enteredPin.isNotEmpty()) {
                                            enteredPin = enteredPin.dropLast(1)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(68.dp)
                                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                                        .testTag("backspace_pin_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Delete Character",
                                        tint = Color.White
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (enteredPin.length < 4) {
                                            enteredPin += key
                                            if (enteredPin.length == 4) {
                                                val success = viewModel.loginWithPin(enteredPin)
                                                if (!success) {
                                                    enteredPin = ""
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = Color.White
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .testTag("pin_btn_$key")
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 2: FIRST TIME SETUP ---
@Composable
fun FirstTimeSetupScreen(viewModel: WalletViewModel) {
    var pin by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentStep by remember { mutableStateOf(1) } // 1: Enter, 2: Confirm

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SophisticatedDarkBg)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security Shield",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (currentStep == 1) "تعریف پین‌کد جدید" else "تایید پین‌کد جدید",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (currentStep == 1) "یک پین‌کد ۴ رقمی برای ورود مجدد به برنامه مشخص کنید"
                    else "پین‌کد را برای تایید مجدداً وارد نمایید",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

            // Dots Display
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val len = if (currentStep == 1) pin.length else pinConfirm.length
                for (i in 1..4) {
                    val isFilled = len >= i
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                    )
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // NumPad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("EMPTY", "0", "BACK")
                )

                for (row in keys) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (key in row) {
                            if (key == "EMPTY") {
                                Spacer(modifier = Modifier.size(68.dp))
                            } else if (key == "BACK") {
                                IconButton(
                                    onClick = {
                                        if (currentStep == 1) {
                                            if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                        } else {
                                            if (pinConfirm.isNotEmpty()) pinConfirm = pinConfirm.dropLast(1)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(68.dp)
                                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Delete Character",
                                        tint = Color.White
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        errorMessage = null
                                        if (currentStep == 1) {
                                            if (pin.length < 4) {
                                                pin += key
                                                if (pin.length == 4) {
                                                    currentStep = 2
                                                }
                                            }
                                        } else {
                                            if (pinConfirm.length < 4) {
                                                pinConfirm += key
                                                if (pinConfirm.length == 4) {
                                                    if (pin == pinConfirm) {
                                                        viewModel.setupPin(pin)
                                                    } else {
                                                        errorMessage = "پین‌کد تایید مطابقت ندارد. دوباره تلاش کنید."
                                                        pinConfirm = ""
                                                        currentStep = 1
                                                        pin = ""
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = Color.White
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .testTag("setup_pin_btn_$key")
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 3: DASHBOARD ---
@Composable
fun DashboardScreen(viewModel: WalletViewModel) {
    val cards by viewModel.allCards.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") } // ALL, BANK_CARD, NATIONAL_ID, SHENASNAMEH, OTHER
    val context = LocalContext.current

    val focusManager = LocalFocusManager.current
    var lastBackPressTime by remember { mutableStateOf(0L) }
    val activity = context as? androidx.activity.ComponentActivity

    BackHandler {
        when {
            searchQuery.isNotEmpty() -> {
                searchQuery = ""
            }
            selectedCategory != "ALL" -> {
                selectedCategory = "ALL"
            }
            else -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < 2000) {
                    activity?.finish()
                } else {
                    lastBackPressTime = currentTime
                    Toast.makeText(context, "برای خروج، دوباره دکمه بازگشت را بزنید", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var showBiometricPrompt by rememberSaveable {
        mutableStateOf(!viewModel.isBiometricEnabled() && viewModel.isBiometricHardwareAvailable())
    }

    if (showBiometricPrompt) {
        AlertDialog(
            onDismissRequest = { showBiometricPrompt = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "فعال‌سازی ورود با اثرانگشت",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "آیا می‌خواهید قابلیت ورود سریع و امن با اثرانگشت (بیومتریک) را فعال کنید؟",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                    Text(
                        text = "با فعال‌سازی این ویژگی، در دفعات بعدی نیازی به وارد کردن پین‌کد نخواهید داشت.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setBiometricEnabled(true)
                        showBiometricPrompt = false
                        Toast.makeText(context, "ورود با اثرانگشت فعال شد", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("بله، فعال‌سازی", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBiometricPrompt = false }
                ) {
                    Text("خیر، بعداً", color = Color.Gray)
                }
            },
            containerColor = SophisticatedSurface,
            textContentColor = Color.White,
            titleContentColor = Color.White
        )
    }

    val filteredCards = cards.filter { card ->
        val matchesSearch = card.title.contains(searchQuery, ignoreCase = true) ||
                (card.ownerName.contains(searchQuery, ignoreCase = true)) ||
                (card.cardNumber?.contains(searchQuery) ?: false)
        val matchesCategory = selectedCategory == "ALL" || card.cardType == selectedCategory
        matchesSearch && matchesCategory
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        val currentTitle = if (selectedCategory == "ALL") {
                            "کیف مدارک امن"
                        } else {
                            getDocumentTypeName(selectedCategory)
                        }
                        val currentSubtitle = if (selectedCategory == "ALL") {
                            "بانک امن اطلاعات کارت و مدارک هویتی شما"
                        } else {
                            "${cards.count { it.cardType == selectedCategory }} مدرک ثبت شده"
                        }
                        Text(
                            text = currentTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = Color.White
                        )
                        Text(
                            text = currentSubtitle,
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                },
                navigationIcon = {
                    if (selectedCategory != "ALL") {
                        IconButton(onClick = { selectedCategory = "ALL" }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to categories",
                                tint = Color.White
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        modifier = Modifier.testTag("settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("logout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = SophisticatedDarkBg,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    viewModel.selectCard(null)
                    viewModel.navigateTo(AppScreen.ADD_EDIT_CARD) 
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(16.dp)
                    .testTag("add_card_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Card",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = SophisticatedDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("جستجو در کارت‌ها و مدارک...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon", tint = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("search_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = SophisticatedSurface,
                    unfocusedContainerColor = SophisticatedSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            AnimatedContent(
                targetState = when {
                    searchQuery.isNotEmpty() -> "SEARCH"
                    selectedCategory == "ALL" -> "CATEGORIES"
                    else -> "CATEGORY_DETAIL"
                },
                transitionSpec = {
                    if (initialState == "CATEGORIES" && targetState == "CATEGORY_DETAIL") {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth / 5 },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                        ) + fadeIn(
                            animationSpec = tween(220)
                        ) togetherWith slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth / 10 },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                        ) + fadeOut(
                            animationSpec = tween(180)
                        )
                    } else if (initialState == "CATEGORY_DETAIL" && targetState == "CATEGORIES") {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth / 5 },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                        ) + fadeIn(
                            animationSpec = tween(220)
                        ) togetherWith slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth / 10 },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                        ) + fadeOut(
                            animationSpec = tween(180)
                        )
                    } else {
                        fadeIn(
                            animationSpec = tween(200)
                        ) togetherWith fadeOut(
                            animationSpec = tween(200)
                        )
                    }
                },
                label = "DashboardContentTransition",
                modifier = Modifier.weight(1f)
            ) { state ->
                when (state) {
                    "SEARCH" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "نتایج جستجو:",
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            if (filteredCards.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.FolderOpen,
                                            contentDescription = "Empty Search",
                                            tint = Color.DarkGray,
                                            modifier = Modifier.size(80.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "موردی با این مشخصات یافت نشد",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.LightGray,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "عبارت جستجوی خود را تغییر دهید",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    items(filteredCards) { card ->
                                        WalletCardItem(card = card, onClick = { viewModel.selectCard(card) }, context = context)
                                    }
                                }
                            }
                        }
                    }
                    "CATEGORIES" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "دسته‌بندی مدارک هویتی و بانکی",
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            val allDocTypes = listOf(
                                "BANK_CARD",
                                "NATIONAL_ID",
                                "SHENASNAMEH",
                                "PASSPORT",
                                "MILITARY_CARD",
                                "DRIVERS_LICENSE",
                                "STUDENT_ID",
                                "POSTAL_ADDRESS",
                                "OTHER"
                            )

                            Column(modifier = Modifier.fillMaxSize()) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(allDocTypes) { type ->
                                        CategoryGridItem(
                                            title = getDocumentTypeName(type),
                                            count = cards.count { it.cardType == type },
                                            icon = getDocumentTypeIcon(type),
                                            onClick = { selectedCategory = type }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "CATEGORY_DETAIL" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (filteredCards.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.FolderOpen,
                                            contentDescription = "Empty Category",
                                            tint = Color.DarkGray,
                                            modifier = Modifier.size(80.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "هنوز هیچ موردی در این دسته‌بندی ثبت نشده است",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.LightGray,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "با زدن دکمه + در گوشه پایین، اولین مورد را اضافه کنید",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    items(filteredCards) { card ->
                                        WalletCardItem(card = card, onClick = { viewModel.selectCard(card) }, context = context)
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

@Composable
fun CategoryGridItem(
    title: String,
    count: Int,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "Alpha"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.94f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "Scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .graphicsLayer(
                alpha = alpha,
                scaleX = scale,
                scaleY = scale
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = SophisticatedSurface
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                // Forward/Left chevron indicating you can navigate deep in RTL
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = if (count > 0) "$count مدرک ثبت شده" else "بدون مدرک",
                    color = if (count > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun WalletCardItem(card: WalletCard, onClick: () -> Unit, context: Context) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "Alpha"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.94f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "Scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .graphicsLayer(
                alpha = alpha,
                scaleX = scale,
                scaleY = scale
            )
            .clickable { onClick() }
            .testTag("wallet_card_item_${card.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = SophisticatedSurface
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val icon = getDocumentTypeIcon(card.cardType)
                        val bankLogoName = if (card.cardType == "BANK_CARD") getBankLogoFromName(card.title) else null
                        
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (bankLogoName != null) Color.White else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bankLogoName != null) {
                                val resId = context.resources.getIdentifier(bankLogoName, "raw", context.packageName)
                                if (resId != 0) {
                                    coil.compose.AsyncImage(
                                        model = coil.request.ImageRequest.Builder(context)
                                            .data(resId)
                                            .decoderFactory(coil.decode.SvgDecoder.Factory())
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = card.title,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = "Card Type Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "Card Type Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = card.title,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Display card type text nicely with high-end neon lime pill
                    val cardTypeName = getDocumentTypeName(card.cardType)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = cardTypeName,
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Card Number display
                if (!card.cardNumber.isNullOrBlank()) {
                    val displayedNumber = if (card.cardType == "BANK_CARD" && card.cardNumber.length >= 16) {
                        card.cardNumber.chunked(4).joinToString("  ")
                    } else {
                        card.cardNumber
                    }
                    Text(
                        text = displayedNumber,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Footer section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (card.cardType != "POSTAL_ADDRESS") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "صاحب مدرک",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = card.ownerName.ifBlank { "ثبت نشده" },
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (card.cardType == "BANK_CARD" && !card.expiryDate.isNullOrBlank()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "انقضا",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = card.expiryDate,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Share & Copy Quick Action Row
                    if (!card.cardNumber.isNullOrBlank()) {
                        IconButton(
                            onClick = { copyToClipboard(context, card.cardNumber, "شماره کارت") },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SophisticatedDarkBg)
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Number",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        // Keep spacing balanced if no copy button
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }
            }
        }
    }
}

// --- SCREEN 4: CARD DETAILS ---
@Composable
fun CardDetailScreen(viewModel: WalletViewModel) {
    val card by viewModel.selectedCard.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler {
        viewModel.navigateTo(AppScreen.DASHBOARD)
    }

    var isFrontExpanded by remember { mutableStateOf(false) }
    var isBackExpanded by remember { mutableStateOf(false) }
    var selectedMultiBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("حذف مدرک", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("آیا از حذف این مدرک اطمینان دارید؟", color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    card?.let { viewModel.deleteCard(it) }
                    Toast.makeText(context, "کارت با موفقیت حذف شد", Toast.LENGTH_SHORT).show()
                }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("انصراف", color = Color.White)
                }
            },
            containerColor = SophisticatedSurface
        )
    }

    if (card == null) return

    val numberLabel = when (card?.cardType) {
        "BANK_CARD" -> "شماره کارت"
        "NATIONAL_ID" -> "کد ملی"
        "SHENASNAMEH" -> "شماره شناسنامه"
        "PASSPORT" -> "شماره گذرنامه"
        "MILITARY_CARD" -> "شماره کارت پایان خدمت"
        "DRIVERS_LICENSE" -> "شماره گواهینامه"
        "STUDENT_ID" -> "شماره دانشجویی"
        "POSTAL_ADDRESS" -> "کد پستی"
        else -> "شماره مدرک"
    }

    val subNumberLabel = when (card?.cardType) {
        "BANK_CARD" -> "کد CVV2"
        "NATIONAL_ID" -> "شماره سریال کارت ملی"
        "SHENASNAMEH" -> "شماره سریال شناسنامه"
        "PASSPORT" -> "کد ملی"
        "MILITARY_CARD" -> "شماره سریال کارت"
        "DRIVERS_LICENSE" -> "کد ملی"
        "STUDENT_ID" -> "کد ملی"
        "POSTAL_ADDRESS" -> "شماره تماس"
        else -> "شناسه دوم"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val bankLogoName = if (card?.cardType == "BANK_CARD") getBankLogoFromName(card?.title ?: "") else null
                        if (bankLogoName != null) {
                            val resId = context.resources.getIdentifier(bankLogoName, "raw", context.packageName)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                if (resId != 0) {
                                    coil.compose.AsyncImage(
                                        model = coil.request.ImageRequest.Builder(context)
                                            .data(resId)
                                            .decoderFactory(coil.decode.SvgDecoder.Factory())
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = card?.title,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = card?.title,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        Text(card?.title ?: "جزئیات مدرک", fontWeight = FontWeight.Bold, color = Color.White) 
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                        modifier = Modifier.testTag("detail_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.EDIT_CARD) },
                        modifier = Modifier.testTag("detail_edit_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Card", tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            showDeleteDialog = true
                        },
                        modifier = Modifier.testTag("detail_delete_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Card", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SophisticatedDarkBg)
            )
        },
        containerColor = SophisticatedDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Visual Card Preview
            card?.let { WalletCardItem(card = it, onClick = {}, context = context) }

            // Fields Table
            Card(
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "اطلاعات متنی مدرک",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (card?.cardType != "POSTAL_ADDRESS") {
                        DetailRowItem(
                            label = if (card?.cardType == "PASSPORT") "نام و نام خانوادگی" else "صاحب مدرک",
                            value = card?.ownerName?.ifBlank { "ثبت نشده" } ?: "ثبت نشده",
                            onCopy = { card?.ownerName?.let { copyToClipboard(context, it, "نام صاحب") } }
                        )
                    }

                    if (!card?.cardNumber.isNullOrBlank()) {
                        DetailRowItem(
                            label = numberLabel,
                            value = card?.cardNumber ?: "",
                            onCopy = { card?.cardNumber?.let { copyToClipboard(context, it, numberLabel) } }
                        )
                    }

                    if (!card?.secondNumber.isNullOrBlank()) {
                        DetailRowItem(
                            label = subNumberLabel,
                            value = card?.secondNumber ?: "",
                            onCopy = { card?.secondNumber?.let { copyToClipboard(context, it, subNumberLabel) } }
                        )
                    }

                    if (card?.cardType == "BANK_CARD" && !card?.expiryDate.isNullOrBlank()) {
                        DetailRowItem(
                            label = "تاریخ انقضا",
                            value = card?.expiryDate ?: "",
                            onCopy = { card?.expiryDate?.let { copyToClipboard(context, it, "تاریخ انقضا") } }
                        )
                    }

                    if (!card?.shebaNumber.isNullOrBlank()) {
                        DetailRowItem(
                            label = "شماره شبا",
                            value = card?.shebaNumber ?: "",
                            onCopy = { card?.shebaNumber?.let { copyToClipboard(context, it, "شماره شبا") } }
                        )
                    }

                    if (!card?.accountNumber.isNullOrBlank()) {
                        DetailRowItem(
                            label = "شماره حساب",
                            value = card?.accountNumber ?: "",
                            onCopy = { card?.accountNumber?.let { copyToClipboard(context, it, "شماره حساب") } }
                        )
                    }

                    // Render dynamic extra fields
                    val extraMap = remember(card?.extraFieldsJson) { card?.getExtraFieldsMap() ?: emptyMap() }
                    val fieldLabelMap = mapOf(
                        "father_name" to "نام پدر",
                        "mother_name" to "نام مادر",
                        "birth_date" to "تاریخ تولد",
                        "issue_date" to "تاریخ صدور",
                        "issue_place" to "محل صدور",
                        "owner_en" to "نام انگلیسی",
                        "service_status" to "وضعیت خدمت نظام وظیفه",
                        "military_branch" to "ارگان خدمت",
                        "license_type" to "پایه گواهینامه",
                        "blood_type" to "گروه خونی",
                        "study_field" to "رشته تحصیلی",
                        "degree_level" to "مقطع تحصیلی",
                        "full_address" to "نشانی کامل پستی",
                        "city_province" to "استان و شهر",
                        "notes" to "توضیحات و ملاحظات"
                    )

                    extraMap.forEach { (key, valueStr) ->
                        if (valueStr.isNotBlank()) {
                            val labelText = fieldLabelMap[key] ?: key
                            DetailRowItem(
                                label = labelText,
                                value = valueStr,
                                onCopy = { copyToClipboard(context, valueStr, labelText) }
                            )
                        }
                    }
                }
            }

            // Photo Documents Section
            Text(
                text = "تصویر مدارک",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )

            val allImages = remember(card) { card?.getAllImages() ?: emptyList() }
            if (isMultiImageDocument(card?.cardType ?: "")) {
                if (allImages.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        allImages.chunked(2).forEachIndexed { rowIndex, rowImages ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowImages.forEachIndexed { colIndex, imgBase64 ->
                                    val pageNum = rowIndex * 2 + colIndex + 1
                                    val bitmap = remember(imgBase64) { base64ToBitmap(imgBase64) }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "صفحه $pageNum",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.LightGray,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        if (bitmap != null) {
                                            Card(
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(130.dp)
                                                    .clickable { selectedMultiBitmap = bitmap }
                                            ) {
                                                Box(modifier = Modifier.fillMaxSize()) {
                                                    Image(
                                                        bitmap = bitmap.asImageBitmap(),
                                                        contentDescription = "Page $pageNum",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    IconButton(
                                                        onClick = { shareBitmap(context, bitmap) },
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(4.dp)
                                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                                            .size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Share,
                                                            contentDescription = "Share",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (rowImages.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SophisticatedSurface)
                            .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("هیچ تصویری برای این مدرک ثبت نشده است", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Front Image Card
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تصویر روی مدرک",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        val frontBitmap = remember(card?.frontImageBase64) { base64ToBitmap(card?.frontImageBase64) }
                        if (frontBitmap != null) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clickable { isFrontExpanded = true }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = frontBitmap.asImageBitmap(),
                                        contentDescription = "Front Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    IconButton(
                                        onClick = { shareBitmap(context, frontBitmap) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            .size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share Image", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SophisticatedSurface)
                                    .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("تصویر ثبت نشده", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }

                    // Back Image Card
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تصویر پشت مدرک",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        val backBitmap = remember(card?.backImageBase64) { base64ToBitmap(card?.backImageBase64) }
                        if (backBitmap != null) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clickable { isBackExpanded = true }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = backBitmap.asImageBitmap(),
                                        contentDescription = "Back Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    IconButton(
                                        onClick = { shareBitmap(context, backBitmap) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            .size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share Image", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SophisticatedSurface)
                                    .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("تصویر ثبت نشده", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Share text details button
            Button(
                onClick = {
                    val formattedDetails = buildString {
                        appendLine("اطلاعات مدرک: ${card?.title}")
                        if (card?.cardType != "POSTAL_ADDRESS" && !card?.ownerName.isNullOrBlank()) {
                            appendLine("صاحب مدرک: ${card?.ownerName}")
                        }
                        if (!card?.cardNumber.isNullOrBlank()) {
                            appendLine("$numberLabel: ${card?.cardNumber}")
                        }
                        if (!card?.secondNumber.isNullOrBlank()) {
                            appendLine("$subNumberLabel: ${card?.secondNumber}")
                        }
                        if (card?.cardType == "BANK_CARD" && !card?.expiryDate.isNullOrBlank()) {
                            appendLine("تاریخ انقضا: ${card?.expiryDate}")
                        }
                        if (!card?.shebaNumber.isNullOrBlank()) { appendLine("شماره شبا: ${card?.shebaNumber}") }
                        if (!card?.accountNumber.isNullOrBlank()) { appendLine("شماره حساب: ${card?.accountNumber}") }
                    }
                    copyToClipboard(context, formattedDetails, "مشخصات مدرک")
                    Toast.makeText(context, "تمام جزئیات در حافظه کپی شد", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("share_text_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                Spacer(modifier = Modifier.width(8.dp))
                Text("کپی کل اطلاعات متنی جهت اشتراک‌گذاری", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Modal Image Lightboxes
    if (isFrontExpanded) {
        val frontBitmap = remember(card?.frontImageBase64) { base64ToBitmap(card?.frontImageBase64) }
        if (frontBitmap != null) {
            AlertDialog(
                onDismissRequest = { isFrontExpanded = false },
                text = {
                    Image(
                        bitmap = frontBitmap.asImageBitmap(),
                        contentDescription = "Expanded Front",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                },
                confirmButton = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row {
                            IconButton(onClick = { saveBitmapToGallery(context, frontBitmap, "روی_مدرک_${card?.title}") }) {
                                Icon(imageVector = Icons.Default.SaveAlt, contentDescription = "Save to Gallery", tint = Color.White)
                            }
                            IconButton(onClick = { shareBitmap(context, frontBitmap) }) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }
                        }
                        TextButton(onClick = { isFrontExpanded = false }) {
                            Text("بستن", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                containerColor = SophisticatedSurface
            )
        }
    }

    if (isBackExpanded) {
        val backBitmap = remember(card?.backImageBase64) { base64ToBitmap(card?.backImageBase64) }
        if (backBitmap != null) {
            AlertDialog(
                onDismissRequest = { isBackExpanded = false },
                text = {
                    Image(
                        bitmap = backBitmap.asImageBitmap(),
                        contentDescription = "Expanded Back",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                },
                confirmButton = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row {
                            IconButton(onClick = { saveBitmapToGallery(context, backBitmap, "پشت_مدرک_${card?.title}") }) {
                                Icon(imageVector = Icons.Default.SaveAlt, contentDescription = "Save to Gallery", tint = Color.White)
                            }
                            IconButton(onClick = { shareBitmap(context, backBitmap) }) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }
                        }
                        TextButton(onClick = { isBackExpanded = false }) {
                            Text("بستن", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                containerColor = SophisticatedSurface
            )
        }
    }

    selectedMultiBitmap?.let { bmp ->
        AlertDialog(
            onDismissRequest = { selectedMultiBitmap = null },
            text = {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Expanded Document Image",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row {
                        IconButton(onClick = { saveBitmapToGallery(context, bmp, "تصویر_مدرک_${card?.title}") }) {
                            Icon(imageVector = Icons.Default.SaveAlt, contentDescription = "Save to Gallery", tint = Color.White)
                        }
                        IconButton(onClick = { shareBitmap(context, bmp) }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                    }
                    TextButton(onClick = { selectedMultiBitmap = null }) {
                        Text("بستن", color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            containerColor = SophisticatedSurface
        )
    }
}

@Composable
fun DetailRowItem(label: String, value: String, onCopy: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "$label:\n$value")
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "اشتراک‌گذاری"))
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(16.dp))
            }
            IconButton(
                onClick = onCopy,
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Value", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

data class ExtractedCardInfo(
    val title: String?,
    val cardNumber: String?,
    val ownerName: String?,
    val secondNumber: String?,
    val expiryDate: String?,
    val shebaNumber: String? = null,
    val accountNumber: String? = null
)

suspend fun analyzeCardOffline(
    context: android.content.Context,
    bitmap: Bitmap,
    cardType: String
): ExtractedCardInfo {
    return try {
        var allText = extractTextFromBitmap(bitmap)
        var parsed = parseCardInfoFromText(allText, cardType)

        if (cardType == "BANK_CARD" && parsed.cardNumber.isNullOrEmpty()) {
            val matrix = android.graphics.Matrix()
            matrix.postRotate(90f)
            val rotatedBitmap = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            val rotatedText = extractTextFromBitmap(rotatedBitmap)
            val parsedRotated = parseCardInfoFromText(rotatedText, cardType)
            
            if (!parsedRotated.cardNumber.isNullOrEmpty()) {
                parsed = parsedRotated
            } else {
                val matrix2 = android.graphics.Matrix()
                matrix2.postRotate(270f)
                val rotatedBitmap2 = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix2, true)
                val rotatedText2 = extractTextFromBitmap(rotatedBitmap2)
                val parsedRotated2 = parseCardInfoFromText(rotatedText2, cardType)
                if (!parsedRotated2.cardNumber.isNullOrEmpty()) {
                    parsed = parsedRotated2
                }
            }
        }
        parsed
    } catch (e: Exception) {
        ExtractedCardInfo(
            title = if (cardType == "BANK_CARD") "کارت بانکی" else if (cardType == "NATIONAL_ID") "کارت ملی" else "شناسنامه",
            cardNumber = "",
            ownerName = "خطا در استخراج",
            secondNumber = "",
            expiryDate = ""
        )
    }
}
fun buildAdditionalImagesJson(list: List<String>): String? {
    if (list.isEmpty()) return null
    return try {
        val arr = org.json.JSONArray()
        list.forEach { arr.put(it) }
        arr.toString()
    } catch (e: Exception) {
        null
    }
}

@Composable
fun AddEditCardScreen(viewModel: WalletViewModel) {
    CardEditorScreen(viewModel = viewModel, isEditMode = false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditorScreen(viewModel: WalletViewModel, isEditMode: Boolean) {
    val card = if (isEditMode) viewModel.selectedCard.value else null
    val context = LocalContext.current

    BackHandler {
        if (isEditMode) viewModel.navigateTo(AppScreen.CARD_DETAIL)
        else viewModel.navigateTo(AppScreen.DASHBOARD)
    }

    if (isEditMode && card == null) {
        LaunchedEffect(Unit) {
            viewModel.navigateTo(AppScreen.DASHBOARD)
        }
        return
    }

    var title by remember(card) { mutableStateOf(card?.title ?: "") }
    var cardType by remember(card) { mutableStateOf(card?.cardType ?: "BANK_CARD") }
    var ownerName by remember(card) { mutableStateOf(card?.ownerName ?: "") }
    var cardNumber by remember(card) { mutableStateOf(card?.cardNumber ?: "") }
    var secondNumber by remember(card) { mutableStateOf(card?.secondNumber ?: "") }
    var expiryDate by remember(card) { mutableStateOf(card?.expiryDate ?: "") }
    var shebaNumber by remember(card) { mutableStateOf(card?.shebaNumber ?: "IR") }
    var accountNumber by remember(card) { mutableStateOf(card?.accountNumber ?: "") }
    val initialExtraMap = remember(card) { card?.getExtraFieldsMap() ?: emptyMap() }
    var extraFatherName by remember(card) { mutableStateOf(initialExtraMap["father_name"] ?: "") }
    var extraMotherName by remember(card) { mutableStateOf(initialExtraMap["mother_name"] ?: "") }
    var extraBirthDate by remember(card) { mutableStateOf(initialExtraMap["birth_date"] ?: "") }
    var extraIssueDate by remember(card) { mutableStateOf(initialExtraMap["issue_date"] ?: "") }
    var extraIssuePlace by remember(card) { mutableStateOf(initialExtraMap["issue_place"] ?: "") }
    var extraOwnerEn by remember(card) { mutableStateOf(initialExtraMap["owner_en"] ?: "") }
    var extraServiceStatus by remember(card) { mutableStateOf(initialExtraMap["service_status"] ?: "") }
    var extraMilitaryBranch by remember(card) { mutableStateOf(initialExtraMap["military_branch"] ?: "") }
    var extraLicenseType by remember(card) { mutableStateOf(initialExtraMap["license_type"] ?: "") }
    var extraBloodType by remember(card) { mutableStateOf(initialExtraMap["blood_type"] ?: "") }
    var extraStudyField by remember(card) { mutableStateOf(initialExtraMap["study_field"] ?: "") }
    var extraDegreeLevel by remember(card) { mutableStateOf(initialExtraMap["degree_level"] ?: "") }
    var extraCityProvince by remember(card) { mutableStateOf(initialExtraMap["city_province"] ?: "") }
    var extraFullAddress by remember(card) { mutableStateOf(initialExtraMap["full_address"] ?: "") }
    var extraNotes by remember(card) { mutableStateOf(initialExtraMap["notes"] ?: "") }

    var frontImageBase64 by remember(card) { mutableStateOf<String?>(card?.frontImageBase64) }
    var backImageBase64 by remember(card) { mutableStateOf<String?>(card?.backImageBase64) }

    val initialImages = remember(card) { card?.getAllImages() ?: emptyList() }
    val additionalImagesList = remember(card) { mutableStateListOf<String>().apply { addAll(initialImages) } }

    var showExpiryDatePicker by remember { mutableStateOf(false) }
    var showScanOptions by remember { mutableStateOf(false) }
    var showImageOptionsForFront by remember { mutableStateOf(false) }
    var showImageOptionsForBack by remember { mutableStateOf(false) }
    var showImageOptionsForMulti by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var tempFrontUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var tempBackUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var tempMultiUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }

    // Standard Intent Fallbacks (highly robust)
    val frontCameraIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val compressedBytes = outputStream.toByteArray()
                frontImageBase64 = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.DEFAULT)
            }
        }
    }

    val backCameraIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val compressedBytes = outputStream.toByteArray()
                backImageBase64 = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.DEFAULT)
            }
        }
    }

    val frontGalleryIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                frontImageBase64 = uriToBase64(context, uri)
            }
        }
    }

    val backGalleryIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                backImageBase64 = uriToBase64(context, uri)
            }
        }
    }

    val scanCardCameraIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                isScanning = true
                scanError = null
                coroutineScope.launch {
                    val extracted = analyzeCardOffline(context, bitmap, cardType)
                    isScanning = false
                    if (extracted != null) {
                        // if (!extracted.title.isNullOrBlank()) title = extracted.title
                        // if (!extracted.ownerName.isNullOrBlank()) ownerName = extracted.ownerName
                        if (!extracted.cardNumber.isNullOrBlank()) cardNumber = extracted.cardNumber
                        if (!extracted.secondNumber.isNullOrBlank()) secondNumber = extracted.secondNumber
                        if (!extracted.expiryDate.isNullOrBlank()) expiryDate = extracted.expiryDate
                        if (!extracted.shebaNumber.isNullOrBlank()) shebaNumber = extracted.shebaNumber
                        if (!extracted.accountNumber.isNullOrBlank()) accountNumber = extracted.accountNumber
                        Toast.makeText(context, "اطلاعات مدرک با موفقیت استخراج شد.", Toast.LENGTH_LONG).show()
                    } else {
                        scanError = "امکان استخراج اطلاعات وجود نداشت. لطفاً دوباره تلاش کنید."
                        Toast.makeText(context, "خطا در پردازش تصویر مدرک", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Media picking launchers (Photo Picker)
    val frontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            frontImageBase64 = uriToBase64(context, uri)
        }
    }

    val backPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            backImageBase64 = uriToBase64(context, uri)
        }
    }

    // Fallback Content pickers (for AOSP or older devices without Photo Picker)
    val frontGetContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            frontImageBase64 = uriToBase64(context, uri)
        }
    }

    val backGetContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            backImageBase64 = uriToBase64(context, uri)
        }
    }

    // Camera Launchers
    val frontCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempFrontUri != null) {
            frontImageBase64 = uriToBase64(context, tempFrontUri!!)
        }
    }

    val backCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempBackUri != null) {
            backImageBase64 = uriToBase64(context, tempBackUri!!)
        }
    }

    val scanCardCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            isScanning = true
            scanError = null
            coroutineScope.launch {
                val extracted = analyzeCardOffline(context, bitmap, cardType)
                isScanning = false
                if (extracted != null) {
                    // if (!extracted.title.isNullOrBlank()) title = extracted.title
                    // if (!extracted.ownerName.isNullOrBlank()) ownerName = extracted.ownerName
                    if (!extracted.cardNumber.isNullOrBlank()) cardNumber = extracted.cardNumber
                    if (!extracted.secondNumber.isNullOrBlank()) secondNumber = extracted.secondNumber
                    if (!extracted.expiryDate.isNullOrBlank()) expiryDate = extracted.expiryDate
                    if (!extracted.shebaNumber.isNullOrBlank()) shebaNumber = extracted.shebaNumber
                    if (!extracted.accountNumber.isNullOrBlank()) accountNumber = extracted.accountNumber
                    Toast.makeText(context, "اطلاعات مدرک با موفقیت استخراج شد.", Toast.LENGTH_LONG).show()
                } else {
                    scanError = "امکان استخراج اطلاعات وجود نداشت. لطفاً دوباره تلاش کنید."
                    Toast.makeText(context, "خطا در پردازش تصویر مدرک", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            try {
                val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                scanCardCameraIntentLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "دریافت دوربین: ${e.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Gallery Card Scan Launchers
    val scanCardGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bmp
            } catch (e: Exception) {
                null
            }
            if (bitmap != null) {
                isScanning = true
                scanError = null
                coroutineScope.launch {
                    val extracted = analyzeCardOffline(context, bitmap, cardType)
                    isScanning = false
                    if (extracted != null) {
                        // if (!extracted.title.isNullOrBlank()) title = extracted.title
                        // if (!extracted.ownerName.isNullOrBlank()) ownerName = extracted.ownerName
                        if (!extracted.cardNumber.isNullOrBlank()) cardNumber = extracted.cardNumber
                        if (!extracted.secondNumber.isNullOrBlank()) secondNumber = extracted.secondNumber
                        if (!extracted.expiryDate.isNullOrBlank()) expiryDate = extracted.expiryDate
                        if (!extracted.shebaNumber.isNullOrBlank()) shebaNumber = extracted.shebaNumber
                        if (!extracted.accountNumber.isNullOrBlank()) accountNumber = extracted.accountNumber
                        Toast.makeText(context, "اطلاعات مدرک با موفقیت استخراج شد.", Toast.LENGTH_LONG).show()
                    } else {
                        scanError = "امکان استخراج اطلاعات وجود نداشت. لطفاً دوباره تلاش کنید."
                        Toast.makeText(context, "خطا در پردازش تصویر مدرک", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "خطا در خواندن تصویر از گالری", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val scanCardGalleryGetContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bitmap = try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bmp
            } catch (e: Exception) {
                null
            }
            if (bitmap != null) {
                isScanning = true
                scanError = null
                coroutineScope.launch {
                    val extracted = analyzeCardOffline(context, bitmap, cardType)
                    isScanning = false
                    if (extracted != null) {
                        // if (!extracted.title.isNullOrBlank()) title = extracted.title
                        // if (!extracted.ownerName.isNullOrBlank()) ownerName = extracted.ownerName
                        if (!extracted.cardNumber.isNullOrBlank()) cardNumber = extracted.cardNumber
                        if (!extracted.secondNumber.isNullOrBlank()) secondNumber = extracted.secondNumber
                        if (!extracted.expiryDate.isNullOrBlank()) expiryDate = extracted.expiryDate
                        if (!extracted.shebaNumber.isNullOrBlank()) shebaNumber = extracted.shebaNumber
                        if (!extracted.accountNumber.isNullOrBlank()) accountNumber = extracted.accountNumber
                        Toast.makeText(context, "اطلاعات مدرک با موفقیت استخراج شد.", Toast.LENGTH_LONG).show()
                    } else {
                        scanError = "امکان استخراج اطلاعات وجود نداشت. لطفاً دوباره تلاش کنید."
                        Toast.makeText(context, "خطا در پردازش تصویر مدرک", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "خطا در خواندن تصویر از گالری", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // State to store which action we wanted to run after permissions are granted
    var pendingActionAfterPermission by remember { mutableStateOf<String?>(null) }

    // Permission Request Launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val storageGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            (permissions[android.Manifest.permission.READ_MEDIA_IMAGES] ?: false) ||
            (permissions[android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] ?: false)
        } else {
            permissions[storagePermission] ?: false
        }

        when (pendingActionAfterPermission) {
            "CAMERA_SCAN" -> {
                if (cameraGranted) {
                    try {
                        scanCardCameraLauncher.launch(null)
                    } catch (e: Exception) {
                        Toast.makeText(context, "خطا", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "اجازه دسترسی به دوربین داده نشد.", Toast.LENGTH_SHORT).show()
                }
            }
            "CAMERA_FRONT" -> {
                if (cameraGranted) {
                    try {
                        val uri = createTempImageUri(context)
                        tempFrontUri = uri
                        frontCameraLauncher.launch(uri)
                    } catch (e: Exception) {
                        try {
                            val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                            frontCameraIntentLauncher.launch(intent)
                        } catch (ex: Exception) {
                            Toast.makeText(context, "دوربین: ${e.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "دسترسی به دوربین داده نشد.", Toast.LENGTH_SHORT).show()
                }
            }
            "CAMERA_BACK" -> {
                if (cameraGranted) {
                    try {
                        val uri = createTempImageUri(context)
                        tempBackUri = uri
                        backCameraLauncher.launch(uri)
                    } catch (e: Exception) {
                        try {
                            val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                            backCameraIntentLauncher.launch(intent)
                        } catch (ex: Exception) {
                            Toast.makeText(context, "دوربین: ${e.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "دسترسی به دوربین داده نشد.", Toast.LENGTH_SHORT).show()
                }
            }
            "GALLERY_FRONT" -> {
                if (storageGranted) {
                    try {
                        frontGetContentLauncher.launch("image/*")
                    } catch (e: Exception) {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_PICK,
                                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            )
                            frontGalleryIntentLauncher.launch(intent)
                        } catch (ex: Exception) {
                            Toast.makeText(context, "گالری: ${e.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "دسترسی به گالری داده نشد.", Toast.LENGTH_SHORT).show()
                }
            }
            "GALLERY_BACK" -> {
                if (storageGranted) {
                    try {
                        backGetContentLauncher.launch("image/*")
                    } catch (e: Exception) {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_PICK,
                                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            )
                            backGalleryIntentLauncher.launch(intent)
                        } catch (ex: Exception) {
                            Toast.makeText(context, "گالری: ${e.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "دسترسی به گالری داده نشد.", Toast.LENGTH_SHORT).show()
                }
            }
            "GALLERY_SCAN" -> {
                if (storageGranted) {
                    try {
                        scanCardGalleryGetContentLauncher.launch("image/*")
                    } catch (e: Exception) {
                        Toast.makeText(context, "گالری: ${e.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "دسترسی به گالری داده نشد.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        pendingActionAfterPermission = null
    }

    // Camera and Gallery permission check & launch helper functions
    val checkAndLaunchCamera: (Boolean) -> Unit = { isFront ->
        val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            try {
                if (isFront) {
                    val uri = createTempImageUri(context)
                    tempFrontUri = uri
                    frontCameraLauncher.launch(uri)
                } else {
                    val uri = createTempImageUri(context)
                    tempBackUri = uri
                    backCameraLauncher.launch(uri)
                }
            } catch (e: Exception) {
                try {
                    val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                    if (isFront) frontCameraIntentLauncher.launch(intent) else backCameraIntentLauncher.launch(intent)
                } catch (ex: Exception) {
                    Toast.makeText(context, "دوربین: ${e.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            pendingActionAfterPermission = if (isFront) "CAMERA_FRONT" else "CAMERA_BACK"
            requestPermissionLauncher.launch(arrayOf(android.Manifest.permission.CAMERA))
        }
    }

    val checkAndLaunchGallery: (Boolean) -> Unit = { isFront ->
        // Standard Photo Picker does not require permissions on Android 11+
        try {
            val launcher = if (isFront) frontPickerLauncher else backPickerLauncher
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_IMAGES
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }

            val hasStoragePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(context, storagePermission) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }

            if (hasStoragePermission) {
                try {
                    val fallbackLauncher = if (isFront) frontGetContentLauncher else backGetContentLauncher
                    fallbackLauncher.launch("image/*")
                } catch (ex: Exception) {
                    try {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                        if (isFront) frontGalleryIntentLauncher.launch(intent) else backGalleryIntentLauncher.launch(intent)
                    } catch (exc: Exception) {
                        Toast.makeText(context, "گالری: ${e.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                pendingActionAfterPermission = if (isFront) "GALLERY_FRONT" else "GALLERY_BACK"
                val permissionsToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                } else {
                    arrayOf(storagePermission)
                }
                requestPermissionLauncher.launch(permissionsToRequest)
            }
        }
    }

    val checkAndLaunchScanCamera: () -> Unit = {
        val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            try {
                        scanCardCameraLauncher.launch(null)
                    } catch (e: Exception) {
                        Toast.makeText(context, "خطا", Toast.LENGTH_SHORT).show()
                    }
        } else {
            pendingActionAfterPermission = "CAMERA_SCAN"
            requestPermissionLauncher.launch(arrayOf(android.Manifest.permission.CAMERA))
        }
    }

    val checkAndLaunchScanGallery: () -> Unit = {
        try {
            scanCardGalleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_IMAGES
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }

            val hasStoragePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(context, storagePermission) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }

            if (hasStoragePermission) {
                try {
                    scanCardGalleryGetContentLauncher.launch("image/*")
                } catch (ex: Exception) {
                    Toast.makeText(context, "گالری: ${e.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
                }
            } else {
                pendingActionAfterPermission = "GALLERY_SCAN"
                val permissionsToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                } else {
                    arrayOf(storagePermission)
                }
                requestPermissionLauncher.launch(permissionsToRequest)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "ویرایش مدرک" else "ثبت مدرک جدید", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isEditMode) viewModel.navigateTo(AppScreen.CARD_DETAIL)
                            else viewModel.navigateTo(AppScreen.DASHBOARD)
                        }
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SophisticatedDarkBg)
            )
        },
        containerColor = SophisticatedDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Custom Expiry Date Picker Dialog (Solar Hijri Month & Year)
            if (showExpiryDatePicker) {
                var selectedMonth by remember {
                    val parts = expiryDate.split("/")
                    mutableStateOf(if (parts.size == 2) parts[0] else "01")
                }
                var selectedYear by remember {
                    val parts = expiryDate.split("/")
                    mutableStateOf(if (parts.size == 2) parts[1] else "05")
                }

                AlertDialog(
                    onDismissRequest = { showExpiryDatePicker = false },
                    title = {
                        Text(
                            text = "انتخاب تاریخ انقضا (ماه / سال)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Year selection header
                            Text(
                                text = "سال انقضا (خورشیدی):",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val years = listOf(
                                    "03" to "۱۴۰۳",
                                    "04" to "۱۴۰۴",
                                    "05" to "۱۴۰۵",
                                    "06" to "۱۴۰۶",
                                    "07" to "۱۴۰۷",
                                    "08" to "۱۴۰۸",
                                    "09" to "۱۴۰۹",
                                    "10" to "۱۴۱۰",
                                    "11" to "۱۴۱۱",
                                    "12" to "۱۴۱۲",
                                    "13" to "۱۴۱۳",
                                    "14" to "۱۴۱۴",
                                    "15" to "۱۴۱۵"
                                )
                                for ((yCode, yName) in years) {
                                    val isSelected = selectedYear == yCode
                                    Card(
                                        modifier = Modifier
                                            .clickable { selectedYear = yCode }
                                            .width(72.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else SophisticatedSurfaceVariant
                                        ),
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f))
                                    ) {
                                        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = yName,
                                                color = if (isSelected) Color.Black else Color.LightGray,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                            // Month selection header
                            Text(
                                text = "ماه انقضا:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            )
                            val months = listOf(
                                "01" to "فروردین", "02" to "اردیبهشت", "03" to "خرداد",
                                "04" to "تیر", "05" to "مرداد", "06" to "شهریور",
                                "07" to "مهر", "08" to "آبان", "09" to "آذر",
                                "10" to "دی", "11" to "بهمن", "12" to "اسفند"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (row in 0 until 4) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        for (col in 0 until 3) {
                                            val index = row * 3 + col
                                            if (index < months.size) {
                                                val (mCode, mName) = months[index]
                                                val isSelected = selectedMonth == mCode
                                                Card(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { selectedMonth = mCode },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else SophisticatedSurfaceVariant
                                                    ),
                                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f))
                                                ) {
                                                    Box(modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp), contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = "$mCode - $mName",
                                                            color = if (isSelected) Color.Black else Color.LightGray,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                expiryDate = "$selectedMonth/$selectedYear"
                                showExpiryDatePicker = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("تایید انتخاب", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExpiryDatePicker = false }) {
                            Text("انصراف", color = Color.Gray)
                        }
                    },
                    containerColor = SophisticatedSurface,
                    textContentColor = Color.White,
                    titleContentColor = Color.White
                )
            }

            // Beautiful Document Type Selector Dropdown Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "نوع مدرک:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )

                    val currentTypeName = getDocumentTypeName(cardType)

                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownExpanded = true }
                            .testTag("type_dropdown_trigger"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = SophisticatedDarkBg),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentTypeName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown Arrow",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    if (dropdownExpanded) {
                        DocumentTypeSelectorBottomSheet(
                            selectedType = cardType,
                            onDismissRequest = { dropdownExpanded = false },
                            onTypeSelected = { newType -> cardType = newType }
                        )
                    }
                }
            }

            // Loading dialog during scanning
            if (isScanning) {
                AlertDialog(
                    onDismissRequest = {},
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "اسکن هوشمند مدرک",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "در حال تحلیل تصویر و استخراج اطلاعات مدرک شماست...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray
                            )
                            Text(
                                text = "این فرایند ممکن است چند ثانیه زمان ببرد. لطفاً شکیبا باشید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    },
                    confirmButton = {},
                    containerColor = SophisticatedSurface,
                    textContentColor = Color.White,
                    titleContentColor = Color.White
                )
            }

            // Compact Smart OCR Scan Bar (Restricted to Bank Cards - Temporarily Hidden)
            if (false && cardType == "BANK_CARD") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            showScanOptions = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Card",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "اسکن کارت بانکی با هوش مصنوعی",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Dialog for Smart Scan Options (Camera or Gallery)
            if (showScanOptions) {
                AlertDialog(
                    onDismissRequest = { showScanOptions = false },
                    title = { Text("اسکن هوشمند مدرک", fontWeight = FontWeight.Bold, color = Color.White) },
                    text = {
                        Column { Text("لطفاً منبع تصویر مدرک خود را انتخاب کنید تا اطلاعات آن استخراج شود:", color = Color.LightGray) 
 Spacer(modifier = Modifier.height(8.dp)) 
 Text("💡 نکته: برای تشخیص دقیق اعداد و تاریخ، در محیطی با نور کافی عکس بگیرید و زاویه گوشی را کاملاً عمود و موازی با کارت قرار دهید.", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp) }
                    },
                    confirmButton = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    showScanOptions = false
                                    checkAndLaunchScanCamera()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null)
                                    Text("اسکن با دوربین")
                                }
                            }
                            
                            Button(
                                onClick = {
                                    showScanOptions = false
                                    checkAndLaunchScanGallery()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SophisticatedSurface,
                                    contentColor = Color.White
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Color.LightGray)
                                    Text("انتخاب از گالری", color = Color.LightGray)
                                }
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showScanOptions = false }) {
                            Text("انصراف", color = Color.Gray)
                        }
                    },
                    containerColor = SophisticatedSurface,
                    textContentColor = Color.White,
                    titleContentColor = Color.White
                )
            }

            if (scanError != null) {
                Text(
                    text = scanError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Form Inputs Container Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "اطلاعات مدرک",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )

                    if (cardType == "BANK_CARD") {
                        var showBankSelector by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = title,
                            onValueChange = { },
                            label = { Text("نام بانک") },
                            placeholder = { Text("انتخاب بانک...") },
                            modifier = Modifier.fillMaxWidth().testTag("input_title_bank"),
                            readOnly = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { showBankSelector = true }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "انتخاب بانک", tint = Color.White)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            interactionSource = remember { MutableInteractionSource() }.also { interactionSource ->
                                LaunchedEffect(interactionSource) {
                                    interactionSource.interactions.collect {
                                        if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                            showBankSelector = true
                                        }
                                    }
                                }
                            }
                        )
                        if (showBankSelector) {
                            BankSelectorBottomSheet(
                                onDismissRequest = { showBankSelector = false },
                                onBankSelected = { bank -> title = bank.name }
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("عنوان مدرک") },
                            placeholder = { Text("مثال: کارت ملی من، شناسنامه فرزند") },
                            modifier = Modifier.fillMaxWidth().testTag("input_title"),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    if (cardType != "POSTAL_ADDRESS") {
                        val ownerLabel = when (cardType) {
                            "PASSPORT" -> "نام و نام خانوادگی (فارسی)"
                            else -> "نام صاحب مدرک"
                        }
                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text(ownerLabel) },
                            modifier = Modifier.fillMaxWidth().testTag("input_owner"),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    if (cardType == "PASSPORT") {
                        OutlinedTextField(
                            value = extraOwnerEn,
                            onValueChange = { extraOwnerEn = it },
                            label = { Text("نام و نام خانوادگی (انگلیسی - گذرنامه)") },
                            modifier = Modifier.fillMaxWidth().testTag("input_owner_en"),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    val numFieldLabel = when (cardType) {
                        "BANK_CARD" -> "شماره ۱۶ رقمی کارت"
                        "NATIONAL_ID" -> "شماره ملی (۱۰ رقمی)"
                        "SHENASNAMEH" -> "شماره شناسنامه"
                        "PASSPORT" -> "شماره گذرنامه / پاسپورت"
                        "MILITARY_CARD" -> "شماره کارت پایان خدمت"
                        "DRIVERS_LICENSE" -> "شماره گواهینامه"
                        "STUDENT_ID" -> "شماره دانشجویی"
                        "POSTAL_ADDRESS" -> "کد پستی ۱۰ رقمی"
                        else -> "شماره مدرک / شماره شناسه"
                    }
                    val numKeyboardType = if (cardType in listOf("BANK_CARD", "NATIONAL_ID", "POSTAL_ADDRESS", "STUDENT_ID")) KeyboardType.Number else KeyboardType.Text

                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it },
                        label = { Text(numFieldLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = numKeyboardType),
                        modifier = Modifier.fillMaxWidth().testTag("input_number"),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Tailored extra fields per card type
                    when (cardType) {
                        "BANK_CARD" -> {
                            OutlinedTextField(
                                value = secondNumber,
                                onValueChange = { secondNumber = it },
                                label = { Text("CVV2") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("input_second_num"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Box(
                                modifier = Modifier.fillMaxWidth().clickable { showExpiryDatePicker = true }
                            ) {
                                OutlinedTextField(
                                    value = expiryDate,
                                    onValueChange = {},
                                    label = { Text("تاریخ انقضا") },
                                    readOnly = true,
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth().testTag("input_expiry"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        disabledTextColor = Color.White,
                                        disabledLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                                    ),
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Select Expiry Date", tint = MaterialTheme.colorScheme.primary)
                                    }
                                )
                            }

                            OutlinedTextField(
                                value = shebaNumber,
                                onValueChange = { shebaNumber = it },
                                label = { Text("شماره شبا") },
                                modifier = Modifier.fillMaxWidth().testTag("input_sheba"),
                                maxLines = 1,
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = accountNumber,
                                onValueChange = { accountNumber = it },
                                label = { Text("شماره حساب") },
                                modifier = Modifier.fillMaxWidth().testTag("input_account"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                maxLines = 1,
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        "NATIONAL_ID" -> {
                            OutlinedTextField(
                                value = secondNumber,
                                onValueChange = { secondNumber = it },
                                label = { Text("کد پستی ۱۰ رقمی") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("input_second_num"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Pin, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = extraFatherName,
                                onValueChange = { extraFatherName = it },
                                label = { Text("نام پدر") },
                                modifier = Modifier.fillMaxWidth().testTag("input_father_name"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.PersonOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            OutlinedTextField(
                                value = extraBirthDate,
                                onValueChange = { extraBirthDate = it },
                                label = { Text("تاریخ تولد") },
                                placeholder = { Text("۱۳۷۰/۰۱/۰۱") },
                                modifier = Modifier.fillMaxWidth().testTag("input_birth_date"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Cake, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }

                        "SHENASNAMEH" -> {
                            OutlinedTextField(
                                value = secondNumber,
                                onValueChange = { secondNumber = it },
                                label = { Text("شماره سریال شناسنامه (مثال: الف/۱۲ ۳۴۵۶)") },
                                modifier = Modifier.fillMaxWidth().testTag("input_second_num"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Tag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = extraFatherName,
                                onValueChange = { extraFatherName = it },
                                label = { Text("نام پدر") },
                                modifier = Modifier.fillMaxWidth().testTag("input_father_name"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.PersonOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            OutlinedTextField(
                                value = extraMotherName,
                                onValueChange = { extraMotherName = it },
                                label = { Text("نام مادر") },
                                modifier = Modifier.fillMaxWidth().testTag("input_mother_name"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.PersonOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = extraBirthDate,
                                onValueChange = { extraBirthDate = it },
                                label = { Text("تاریخ تولد") },
                                placeholder = { Text("۱۳۷۰/۰۱/۰۱") },
                                modifier = Modifier.fillMaxWidth().testTag("input_birth_date"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Cake, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            OutlinedTextField(
                                value = extraIssuePlace,
                                onValueChange = { extraIssuePlace = it },
                                label = { Text("محل صدور / تولد") },
                                modifier = Modifier.fillMaxWidth().testTag("input_issue_place"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }

                        "PASSPORT" -> {
                            OutlinedTextField(
                                value = secondNumber,
                                onValueChange = { secondNumber = it },
                                label = { Text("کد ملی صاحب گذرنامه") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("input_second_num"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = extraIssueDate,
                                onValueChange = { extraIssueDate = it },
                                label = { Text("تاریخ صدور") },
                                placeholder = { Text("1402/05/10") },
                                modifier = Modifier.fillMaxWidth().testTag("input_issue_date"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            OutlinedTextField(
                                value = expiryDate,
                                onValueChange = { expiryDate = it },
                                label = { Text("تاریخ انقضا") },
                                placeholder = { Text("1407/05/10") },
                                modifier = Modifier.fillMaxWidth().testTag("input_expiry_date"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }

                        "MILITARY_CARD" -> {
                            OutlinedTextField(
                                value = extraServiceStatus,
                                onValueChange = { extraServiceStatus = it },
                                label = { Text("وضعیت خدمت") },
                                placeholder = { Text("پایان خدمت / معافیت") },
                                modifier = Modifier.fillMaxWidth().testTag("input_service_status"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            OutlinedTextField(
                                value = extraMilitaryBranch,
                                onValueChange = { extraMilitaryBranch = it },
                                label = { Text("ارگان خدمت") },
                                placeholder = { Text("ارتش / سپاه / فراجا") },
                                modifier = Modifier.fillMaxWidth().testTag("input_military_branch"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }

                        "DRIVERS_LICENSE" -> {
                            OutlinedTextField(
                                value = extraLicenseType,
                                onValueChange = { extraLicenseType = it },
                                label = { Text("پایه گواهینامه") },
                                placeholder = { Text("پایه سوم / پایه دوم") },
                                modifier = Modifier.fillMaxWidth().testTag("input_license_type"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            OutlinedTextField(
                                value = extraBloodType,
                                onValueChange = { extraBloodType = it },
                                label = { Text("گروه خونی") },
                                placeholder = { Text("O+") },
                                modifier = Modifier.fillMaxWidth().testTag("input_blood_type"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }

                        "STUDENT_ID" -> {
                            OutlinedTextField(
                                value = extraStudyField,
                                onValueChange = { extraStudyField = it },
                                label = { Text("رشته تحصیلی") },
                                modifier = Modifier.fillMaxWidth().testTag("input_study_field"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            OutlinedTextField(
                                value = extraDegreeLevel,
                                onValueChange = { extraDegreeLevel = it },
                                label = { Text("مقطع تحصیلی") },
                                placeholder = { Text("کارشناسی / ارشد") },
                                modifier = Modifier.fillMaxWidth().testTag("input_degree_level"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Class, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }

                        "POSTAL_ADDRESS" -> {
                            OutlinedTextField(
                                value = extraCityProvince,
                                onValueChange = { extraCityProvince = it },
                                label = { Text("استان / شهر") },
                                placeholder = { Text("تهران، تهران") },
                                modifier = Modifier.fillMaxWidth().testTag("input_city_province"),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )

                            OutlinedTextField(
                                value = extraFullAddress,
                                onValueChange = { extraFullAddress = it },
                                label = { Text("آدرس پستی دقیق") },
                                modifier = Modifier.fillMaxWidth().height(100.dp).testTag("input_full_address"),
                                maxLines = 4,
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }

                        else -> {
                            OutlinedTextField(
                                value = extraNotes,
                                onValueChange = { extraNotes = it },
                                label = { Text("توضیحات و یادداشت‌ها") },
                                modifier = Modifier.fillMaxWidth().height(100.dp).testTag("input_notes"),
                                maxLines = 4,
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Note, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                        }
                    }
                }
            }

            // Document Image Picking Section
            val isTwoSided = isCardWithFrontBack(cardType)
            if (isTwoSided) {
                Text("آپلود عکس مدرک (روی مدرک و پشت مدرک):", fontWeight = FontWeight.Bold, color = Color.LightGray)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Front Image Picker
                    Column(modifier = Modifier.weight(1f)) {
                        Text("روی مدرک:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        val frontBitmap = remember(frontImageBase64) { base64ToBitmap(frontImageBase64) }

                        if (frontBitmap != null) {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                                Image(
                                    bitmap = frontBitmap.asImageBitmap(),
                                    contentDescription = "Front Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                )
                                IconButton(
                                    onClick = { frontImageBase64 = null },
                                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Red.copy(alpha = 0.8f), CircleShape)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        } else {
                            Button(
                                onClick = { showImageOptionsForFront = true },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SophisticatedSurface)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Camera", tint = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("انتخاب تصویر", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Back Image Picker
                    Column(modifier = Modifier.weight(1f)) {
                        Text("پشت مدرک:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        val backBitmap = remember(backImageBase64) { base64ToBitmap(backImageBase64) }

                        if (backBitmap != null) {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                                Image(
                                    bitmap = backBitmap.asImageBitmap(),
                                    contentDescription = "Back Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                )
                                IconButton(
                                    onClick = { backImageBase64 = null },
                                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Red.copy(alpha = 0.8f), CircleShape)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        } else {
                            Button(
                                onClick = { showImageOptionsForBack = true },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SophisticatedSurface)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Camera", tint = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("انتخاب تصویر", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // Multi-Image Gallery Picker for Shenasnameh, Passport, Postal Address, etc.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "تصاویر مدرک (می‌توانید هر تعداد صفحه/تصویر اضافه کنید):",
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Render existing additional images
                        additionalImagesList.forEachIndexed { index, imgBase64 ->
                            val bmp = remember(imgBase64) { base64ToBitmap(imgBase64) }
                            if (bmp != null) {
                                Box(modifier = Modifier.size(110.dp)) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Document Page ${index + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                                    )
                                    IconButton(
                                        onClick = { additionalImagesList.removeAt(index) },
                                        modifier = Modifier.align(Alignment.TopEnd).size(26.dp).background(Color.Red.copy(alpha = 0.85f), CircleShape)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                    Text(
                                        text = "صفحه ${index + 1}",
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.BottomStart).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topEnd = 8.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Add new image card button
                        Card(
                            onClick = { showImageOptionsForFront = true },
                            modifier = Modifier.size(110.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = "Add Photo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("افزودن عکس", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Dialogs for Front Image Options
            if (showImageOptionsForFront) {
                ImageSourceSelectionDialog(
                    title = "انتخاب تصویر روی مدرک",
                    onDismissRequest = { showImageOptionsForFront = false },
                    onCameraSelect = { checkAndLaunchCamera(true) },
                    onGallerySelect = { checkAndLaunchGallery(true) }
                )
            }

            // Dialogs for Back Image Options
            if (showImageOptionsForBack) {
                ImageSourceSelectionDialog(
                    title = "انتخاب تصویر پشت مدرک",
                    onDismissRequest = { showImageOptionsForBack = false },
                    onCameraSelect = { checkAndLaunchCamera(false) },
                    onGallerySelect = { checkAndLaunchGallery(false) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save & Cancel row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            Toast.makeText(context, "عنوان مدرک نمی‌تواند خالی باشد", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val extraJson = buildExtraFieldsJson(
                            cardType = cardType,
                            fatherName = extraFatherName,
                            motherName = extraMotherName,
                            birthDate = extraBirthDate,
                            issueDate = extraIssueDate,
                            issuePlace = extraIssuePlace,
                            ownerEn = extraOwnerEn,
                            serviceStatus = extraServiceStatus,
                            militaryBranch = extraMilitaryBranch,
                            licenseType = extraLicenseType,
                            bloodType = extraBloodType,
                            studyField = extraStudyField,
                            degreeLevel = extraDegreeLevel,
                            cityProvince = extraCityProvince,
                            fullAddress = extraFullAddress,
                            notes = extraNotes
                        )

                        val additionalJson = if (!isTwoSided) buildAdditionalImagesJson(additionalImagesList) else null
                        val finalFront = if (isTwoSided) frontImageBase64 else additionalImagesList.firstOrNull()
                        val finalBack = if (isTwoSided) backImageBase64 else additionalImagesList.getOrNull(1)

                        viewModel.saveCard(
                            id = card?.id ?: 0,
                            title = title,
                            cardType = cardType,
                            ownerName = if (cardType == "POSTAL_ADDRESS") "" else ownerName,
                            cardNumber = cardNumber,
                            secondNumber = secondNumber,
                            expiryDate = expiryDate,
                            shebaNumber = shebaNumber,
                            accountNumber = accountNumber,
                            frontImageBase64 = finalFront,
                            backImageBase64 = finalBack,
                            additionalImagesJson = additionalJson,
                            extraFieldsJson = extraJson
                        )
                        Toast.makeText(context, if (isEditMode) "مدرک با موفقیت بروزرسانی شد" else "مدرک با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                        if (isEditMode) viewModel.navigateTo(AppScreen.CARD_DETAIL)
                        else viewModel.navigateTo(AppScreen.DASHBOARD)
                    },
                    modifier = Modifier.weight(1f).height(54.dp).testTag("save_card_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(if (isEditMode) "بروزرسانی مدرک" else "ذخیره‌سازی امن", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                OutlinedButton(
                    onClick = {
                        if (isEditMode) viewModel.navigateTo(AppScreen.CARD_DETAIL)
                        else viewModel.navigateTo(AppScreen.DASHBOARD)
                    },
                    modifier = Modifier.weight(0.5f).height(54.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                ) {
                    Text("لغو")
                }
            }
        }
    }
}

// --- SCREEN 5B: EDIT CARD SCREEN ---
@Composable
fun EditCardScreen(viewModel: WalletViewModel) {
    CardEditorScreen(viewModel = viewModel, isEditMode = true)
}

@Composable
private fun legacyEditCardScreenUnused(viewModel: WalletViewModel) {
    if (false) {
    val card = viewModel.selectedCard.value
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    BackHandler {
        viewModel.navigateTo(AppScreen.CARD_DETAIL)
    }

    if (card == null) {
        // Fallback safety
        LaunchedEffect(Unit) {
            viewModel.navigateTo(AppScreen.DASHBOARD)
        }
        return
    }

    var title by remember { mutableStateOf(card.title) }
    var cardType by remember { mutableStateOf(card.cardType) }
    var ownerName by remember { mutableStateOf(card.ownerName) }
    var cardNumber by remember { mutableStateOf(card.cardNumber ?: "") }
    var secondNumber by remember { mutableStateOf(card.secondNumber ?: "") }
    var expiryDate by remember { mutableStateOf(card.expiryDate ?: "") }
    var shebaNumber by remember { mutableStateOf(card.shebaNumber ?: "IR") }
    var accountNumber by remember { mutableStateOf(card.accountNumber ?: "") }
    var showExpiryDatePicker by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var frontImageBase64 by remember { mutableStateOf<String?>(card.frontImageBase64) }
    var backImageBase64 by remember { mutableStateOf<String?>(card.backImageBase64) }

    val coroutineScope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var tempFrontUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var tempBackUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }

    // Standard Intent Fallbacks (highly robust)
    val frontCameraIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val compressedBytes = outputStream.toByteArray()
                frontImageBase64 = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.DEFAULT)
            }
        }
    }

    val backCameraIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val compressedBytes = outputStream.toByteArray()
                backImageBase64 = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.DEFAULT)
            }
        }
    }

    val frontGalleryIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                frontImageBase64 = uriToBase64(context, uri)
            }
        }
    }

    val backGalleryIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                backImageBase64 = uriToBase64(context, uri)
            }
        }
    }

    val scanCardCameraIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                isScanning = true
                scanError = null
                coroutineScope.launch {
                    val extracted = analyzeCardOffline(context, bitmap, cardType)
                    isScanning = false
                    if (extracted != null) {
                        // if (!extracted.title.isNullOrBlank()) title = extracted.title
                        // if (!extracted.ownerName.isNullOrBlank()) ownerName = extracted.ownerName
                        if (!extracted.cardNumber.isNullOrBlank()) cardNumber = extracted.cardNumber
                        if (!extracted.secondNumber.isNullOrBlank()) secondNumber = extracted.secondNumber
                        if (!extracted.expiryDate.isNullOrBlank()) expiryDate = extracted.expiryDate
                        if (!extracted.shebaNumber.isNullOrBlank()) shebaNumber = extracted.shebaNumber
                        if (!extracted.accountNumber.isNullOrBlank()) accountNumber = extracted.accountNumber
                        Toast.makeText(context, "اطلاعات مدرک با موفقیت مجدداً استخراج شد.", Toast.LENGTH_LONG).show()
                    } else {
                        scanError = "امکان استخراج اطلاعات وجود نداشت. لطفاً دوباره تلاش کنید."
                        Toast.makeText(context, "خطا در پردازش تصویر مدرک", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Media picking launchers (Photo Picker)
    val frontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            frontImageBase64 = uriToBase64(context, uri)
        }
    }

    val backPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            backImageBase64 = uriToBase64(context, uri)
        }
    }

    // Fallback Content pickers (for AOSP or older devices without Photo Picker)
    val frontGetContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            frontImageBase64 = uriToBase64(context, uri)
        }
    }

    val backGetContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            backImageBase64 = uriToBase64(context, uri)
        }
    }

    // Camera Launchers
    val frontCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempFrontUri != null) {
            frontImageBase64 = uriToBase64(context, tempFrontUri!!)
        }
    }

    val backCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempBackUri != null) {
            backImageBase64 = uriToBase64(context, tempBackUri!!)
        }
    }

    val scanCardCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            isScanning = true
            scanError = null
            coroutineScope.launch {
                val extracted = analyzeCardOffline(context, bitmap, cardType)
                isScanning = false
                if (extracted != null) {
                    // if (!extracted.title.isNullOrBlank()) title = extracted.title
                    // if (!extracted.ownerName.isNullOrBlank()) ownerName = extracted.ownerName
                    if (!extracted.cardNumber.isNullOrBlank()) cardNumber = extracted.cardNumber
                    if (!extracted.secondNumber.isNullOrBlank()) secondNumber = extracted.secondNumber
                    if (!extracted.expiryDate.isNullOrBlank()) expiryDate = extracted.expiryDate
                    if (!extracted.shebaNumber.isNullOrBlank()) shebaNumber = extracted.shebaNumber
                    if (!extracted.accountNumber.isNullOrBlank()) accountNumber = extracted.accountNumber
                    Toast.makeText(context, "اطلاعات مدرک با موفقیت مجدداً استخراج شد.", Toast.LENGTH_LONG).show()
                } else {
                    scanError = "امکان استخراج اطلاعات وجود نداشت. لطفاً دوباره تلاش کنید."
                    Toast.makeText(context, "خطا در پردازش تصویر مدرک", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            try {
                val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                scanCardCameraIntentLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "دریافت دوربین: ${e.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Gallery Card Scan Launchers
    val scanCardGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bmp
            } catch (e: Exception) {
                null
            }
            if (bitmap != null) {
                isScanning = true
                scanError = null
                coroutineScope.launch {
                    val extracted = analyzeCardOffline(context, bitmap, cardType)
                    isScanning = false
                    if (extracted != null) {
                        // if (!extracted.title.isNullOrBlank()) title = extracted.title
                        // if (!extracted.ownerName.isNullOrBlank()) ownerName = extracted.ownerName
                        if (!extracted.cardNumber.isNullOrBlank()) cardNumber = extracted.cardNumber
                        if (!extracted.secondNumber.isNullOrBlank()) secondNumber = extracted.secondNumber
                        if (!extracted.expiryDate.isNullOrBlank()) expiryDate = extracted.expiryDate
                        if (!extracted.shebaNumber.isNullOrBlank()) shebaNumber = extracted.shebaNumber
                        if (!extracted.accountNumber.isNullOrBlank()) accountNumber = extracted.accountNumber
                        Toast.makeText(context, "اطلاعات مدرک با موفقیت استخراج شد.", Toast.LENGTH_LONG).show()
                    } else {
                        scanError = "امکان استخراج اطلاعات وجود نداشت. لطفاً دوباره تلاش کنید."
                        Toast.makeText(context, "خطا در پردازش تصویر مدرک", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "خطا در خواندن تصویر از گالری", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val scanCardGalleryGetContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bitmap = try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bmp
            } catch (e: Exception) {
                null
            }
            if (bitmap != null) {
                isScanning = true
                scanError = null
                coroutineScope.launch {
                    val extracted = analyzeCardOffline(context, bitmap, cardType)
                    isScanning = false
                    if (extracted != null) {
                        // if (!extracted.title.isNullOrBlank()) title = extracted.title
                        // if (!extracted.ownerName.isNullOrBlank()) ownerName = extracted.ownerName
                        if (!extracted.cardNumber.isNullOrBlank()) cardNumber = extracted.cardNumber
                        if (!extracted.secondNumber.isNullOrBlank()) secondNumber = extracted.secondNumber
                        if (!extracted.expiryDate.isNullOrBlank()) expiryDate = extracted.expiryDate
                        if (!extracted.shebaNumber.isNullOrBlank()) shebaNumber = extracted.shebaNumber
                        if (!extracted.accountNumber.isNullOrBlank()) accountNumber = extracted.accountNumber
                        Toast.makeText(context, "اطلاعات مدرک با موفقیت استخراج شد.", Toast.LENGTH_LONG).show()
                    } else {
                        scanError = "امکان استخراج اطلاعات وجود نداشت. لطفاً دوباره تلاش کنید."
                        Toast.makeText(context, "خطا در پردازش تصویر مدرک", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "خطا در خواندن تصویر از گالری", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // State to store which action we wanted to run after permissions are granted
    var pendingActionAfterPermission by remember { mutableStateOf<String?>(null) }

    // Permission Request Launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val storageGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            (permissions[android.Manifest.permission.READ_MEDIA_IMAGES] ?: false) ||
            (permissions[android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] ?: false)
        } else {
            permissions[storagePermission] ?: false
        }

        when (pendingActionAfterPermission) {
            "CAMERA_SCAN" -> {
                if (cameraGranted) {
                    try {
                        scanCardCameraLauncher.launch(null)
                    } catch (e: Exception) {
                        Toast.makeText(context, "خطا", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "اجازه دسترسی به دوربین داده نشد.", Toast.LENGTH_SHORT).show()
                }
            }
            "CAMERA_FRONT" -> {
                if (cameraGranted) {
                    try {
                        val uri = createTempImageUri(context)
                        tempFrontUri = uri
                        frontCameraLauncher.launch(uri)
                    } catch (e: Exception) {
                        try {
                            val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                            frontCameraIntentLauncher.launch(intent)
                        } catch (ex: Exception) {
                            Toast.makeText(context, "دوربین: ${e.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "دسترسی به دوربین داده نشد.", Toast.LENGTH_SHORT).show()
                }
            }
            "CAMERA_BACK" -> {
                if (cameraGranted) {
                    try {
                        val uri = createTempImageUri(context)
                        tempBackUri = uri
                        backCameraLauncher.launch(uri)
                    } catch (e: Exception) {
                        try {
                            val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                            backCameraIntentLauncher.launch(intent)
                        } catch (ex: Exception) {
                            Toast.makeText(context, "دوربین: ${e.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "دسترسی به دوربین داده نشد.", Toast.LENGTH_SHORT).show()
                }
            }
            "GALLERY_FRONT" -> {
                if (storageGranted) {
                    try {
                        frontGetContentLauncher.launch("image/*")
                    } catch (e: Exception) {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_PICK,
                                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            )
                            frontGalleryIntentLauncher.launch(intent)
                        } catch (ex: Exception) {
                            Toast.makeText(context, "گالری: ${e.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "دسترسی به گالری داده نشد.", Toast.LENGTH_SHORT).show()
                }
            }
            "GALLERY_BACK" -> {
                if (storageGranted) {
                    try {
                        backGetContentLauncher.launch("image/*")
                    } catch (e: Exception) {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_PICK,
                                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            )
                            backGalleryIntentLauncher.launch(intent)
                        } catch (ex: Exception) {
                            Toast.makeText(context, "گالری: ${e.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "دسترسی به گالری داده نشد.", Toast.LENGTH_SHORT).show()
                }
            }
            "GALLERY_SCAN" -> {
                if (storageGranted) {
                    try {
                        scanCardGalleryGetContentLauncher.launch("image/*")
                    } catch (e: Exception) {
                        Toast.makeText(context, "گالری: ${e.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "دسترسی به گالری داده نشد.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        pendingActionAfterPermission = null
    }

    // Camera and Gallery permission check & launch helper functions
    val checkAndLaunchCamera: (Boolean) -> Unit = { isFront ->
        val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            try {
                if (isFront) {
                    val uri = createTempImageUri(context)
                    tempFrontUri = uri
                    frontCameraLauncher.launch(uri)
                } else {
                    val uri = createTempImageUri(context)
                    tempBackUri = uri
                    backCameraLauncher.launch(uri)
                }
            } catch (e: Exception) {
                try {
                    val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                    if (isFront) frontCameraIntentLauncher.launch(intent) else backCameraIntentLauncher.launch(intent)
                } catch (ex: Exception) {
                    Toast.makeText(context, "دوربین: ${e.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            pendingActionAfterPermission = if (isFront) "CAMERA_FRONT" else "CAMERA_BACK"
            requestPermissionLauncher.launch(arrayOf(android.Manifest.permission.CAMERA))
        }
    }

    val checkAndLaunchGallery: (Boolean) -> Unit = { isFront ->
        // Standard Photo Picker does not require permissions on Android 11+
        try {
            val launcher = if (isFront) frontPickerLauncher else backPickerLauncher
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_IMAGES
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }

            val hasStoragePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(context, storagePermission) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }

            if (hasStoragePermission) {
                try {
                    val fallbackLauncher = if (isFront) frontGetContentLauncher else backGetContentLauncher
                    fallbackLauncher.launch("image/*")
                } catch (ex: Exception) {
                    try {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                        if (isFront) frontGalleryIntentLauncher.launch(intent) else backGalleryIntentLauncher.launch(intent)
                    } catch (exc: Exception) {
                        Toast.makeText(context, "گالری: ${e.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                pendingActionAfterPermission = if (isFront) "GALLERY_FRONT" else "GALLERY_BACK"
                val permissionsToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                } else {
                    arrayOf(storagePermission)
                }
                requestPermissionLauncher.launch(permissionsToRequest)
            }
        }
    }

    val checkAndLaunchScanCamera: () -> Unit = {
        val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            try {
                        scanCardCameraLauncher.launch(null)
                    } catch (e: Exception) {
                        Toast.makeText(context, "خطا", Toast.LENGTH_SHORT).show()
                    }
        } else {
            pendingActionAfterPermission = "CAMERA_SCAN"
            requestPermissionLauncher.launch(arrayOf(android.Manifest.permission.CAMERA))
        }
    }

    val checkAndLaunchScanGallery: () -> Unit = {
        try {
            scanCardGalleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_IMAGES
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }

            val hasStoragePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(context, storagePermission) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }

            if (hasStoragePermission) {
                try {
                    scanCardGalleryGetContentLauncher.launch("image/*")
                } catch (ex: Exception) {
                    Toast.makeText(context, "گالری: ${e.message ?: "Unknown"}", Toast.LENGTH_LONG).show()
                }
            } else {
                pendingActionAfterPermission = "GALLERY_SCAN"
                val permissionsToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                } else {
                    arrayOf(storagePermission)
                }
                requestPermissionLauncher.launch(permissionsToRequest)
            }
        }
    }

    // Dialog state variables
    var showScanOptions by remember { mutableStateOf(false) }
    var showImageOptionsForFront by remember { mutableStateOf(false) }
    var showImageOptionsForBack by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ویرایش مدرک", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.navigateTo(AppScreen.CARD_DETAIL)
                        }
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SophisticatedDarkBg)
            )
        },
        containerColor = SophisticatedDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card showing current editing document info
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "در حال ویرایش مدرک:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                        Text(
                            text = card.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Custom Expiry Date Picker Dialog (Solar Hijri Month & Year)
            if (showExpiryDatePicker) {
                var selectedMonth by remember {
                    val parts = expiryDate.split("/")
                    mutableStateOf(if (parts.size == 2) parts[0] else "01")
                }
                var selectedYear by remember {
                    val parts = expiryDate.split("/")
                    mutableStateOf(if (parts.size == 2) parts[1] else "05")
                }

                AlertDialog(
                    onDismissRequest = { showExpiryDatePicker = false },
                    title = {
                        Text(
                            text = "انتخاب تاریخ انقضا (ماه / سال)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Year selection header
                            Text(
                                text = "سال انقضا (خورشیدی):",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                (1..20).map { it.toString().padStart(2, '0') }.forEach { yName ->
                                    val isSelected = selectedYear == yName
                                    Card(
                                        modifier = Modifier
                                            .width(52.dp)
                                            .clickable { selectedYear = yName },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else SophisticatedSurface
                                        )
                                    ) {
                                        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = yName,
                                                color = if (isSelected) Color.Black else Color.LightGray,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Month selection header
                            Text(
                                text = "ماه انقضا (خورشیدی):",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            )
                            val months = listOf(
                                "01" to "فروردین", "02" to "اردیبهشت", "03" to "خرداد",
                                "04" to "تیر", "05" to "مرداد", "06" to "شهریور",
                                "07" to "مهر", "08" to "آبان", "09" to "آذر",
                                "10" to "دی", "11" to "بهمن", "12" to "اسفند"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                months.forEach { (mCode, mName) ->
                                    val isSelected = selectedMonth == mCode
                                    Card(
                                        modifier = Modifier
                                            .width(82.dp)
                                            .clickable { selectedMonth = mCode },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else SophisticatedSurface
                                        )
                                    ) {
                                        Box(modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$mCode - $mName",
                                                color = if (isSelected) Color.Black else Color.LightGray,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                expiryDate = "$selectedMonth/$selectedYear"
                                showExpiryDatePicker = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("تایید انتخاب", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExpiryDatePicker = false }) {
                            Text("انصراف", color = Color.Gray)
                        }
                    },
                    containerColor = SophisticatedDarkBg,
                    titleContentColor = Color.White,
                    textContentColor = Color.White
                )
            }

            // Document Type Selection Row
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "نوع مدرک:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )

                val currentTypeName = getDocumentTypeName(cardType)

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dropdownExpanded = true }
                        .testTag("type_dropdown_trigger_edit"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = SophisticatedDarkBg),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentTypeName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown Arrow",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                if (dropdownExpanded) {
                    DocumentTypeSelectorBottomSheet(
                        selectedType = cardType,
                        onDismissRequest = { dropdownExpanded = false },
                        onTypeSelected = { newType -> cardType = newType }
                    )
                }
            }

            // AI Smart Scanning Section (Restricted to Bank Cards - Temporarily Hidden)
            if (false && cardType == "BANK_CARD") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "اسکن و استخراج هوشمند کارت بانکی",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Text(
                            text = "می‌توانید عکس کارت بانکی را مجدداً آپلود کرده و اطلاعات آن را استخراج یا جایگزین کنید.",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )

                        if (isScanning) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("درحال پردازش و استخراج اطلاعات کارت...", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = { 
                                    showScanOptions = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("اسکن مجدد کارت بانکی", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // Dialog for Smart Scan Options (Camera or Gallery)
                        if (showScanOptions) {
                            AlertDialog(
                                onDismissRequest = { showScanOptions = false },
                                title = { Text("اسکن مجدد کارت بانکی", fontWeight = FontWeight.Bold, color = Color.White) },
                                text = {
                                    Column { Text("لطفاً منبع تصویر کارت بانکی خود را انتخاب کنید تا اطلاعات آن استخراج شود:", color = Color.LightGray) 
     Spacer(modifier = Modifier.height(8.dp)) 
     Text("💡 نکته: برای تشخیص دقیق اعداد و تاریخ، در محیطی با نور کافی عکس بگیرید و زاویه گوشی را کاملاً عمود و موازی با کارت قرار دهید.", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp) }
                                },
                                confirmButton = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                showScanOptions = false
                                                checkAndLaunchScanCamera()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null)
                                                Text("اسکن با دوربین")
                                            }
                                        }
                                        
                                        Button(
                                            onClick = {
                                                showScanOptions = false
                                                checkAndLaunchScanGallery()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SophisticatedSurface,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Color.LightGray)
                                                Text("انتخاب از گالری", color = Color.LightGray)
                                            }
                                        }
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showScanOptions = false }) {
                                        Text("انصراف", color = Color.Gray)
                                    }
                                },
                                containerColor = SophisticatedSurface,
                                textContentColor = Color.White,
                                titleContentColor = Color.White
                            )
                        }

                        scanError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }

            // Form Inputs Container Card (Groups Core details nicely to avoid visual clutter)
            Card(
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "اطلاعات مدرک",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )

                    if (cardType == "BANK_CARD") {
                        var showBankSelector by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = title,
                            onValueChange = { },
                            label = { Text("نام بانک") },
                            placeholder = { Text("انتخاب بانک...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_input_title_bank"),
                            readOnly = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { showBankSelector = true }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "انتخاب بانک", tint = Color.White)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            interactionSource = remember { MutableInteractionSource() }.also { interactionSource ->
                                LaunchedEffect(interactionSource) {
                                    interactionSource.interactions.collect {
                                        if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                            showBankSelector = true
                                        }
                                    }
                                }
                            }
                        )
                        if (showBankSelector) {
                            BankSelectorBottomSheet(
                                onDismissRequest = { showBankSelector = false },
                                onBankSelected = { bank -> title = bank.name }
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("عنوان مدرک") },
                            placeholder = { Text("مثال: کارت ملت من، کارت ملی علی") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_input_title"),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("نام صاحب مدرک") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_input_owner"),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    val numFieldLabel = when (cardType) {
                        "BANK_CARD" -> "شماره ۱۶ رقمی کارت"
                        "NATIONAL_ID" -> "شماره ملی (۱۰ رقمی)"
                        "SHENASNAMEH" -> "شماره شناسنامه"
                        else -> "شماره مدرک / شماره شناسه"
                    }
                    val numKeyboardType = if (cardType == "BANK_CARD" || cardType == "NATIONAL_ID") KeyboardType.Number else KeyboardType.Text

                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it },
                        label = { Text(numFieldLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = numKeyboardType),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_input_number"),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // CVV2 / Expiry for Bank Card, or serial for Shenasnameh
                    if (cardType == "BANK_CARD") {
                        OutlinedTextField(
                            value = secondNumber,
                            onValueChange = { secondNumber = it },
                            label = { Text("CVV2") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_input_second_num"),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        // Interactive Date Picker Trigger Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showExpiryDatePicker = true }
                        ) {
                            OutlinedTextField(
                                value = expiryDate,
                                onValueChange = {},
                                label = { Text("تاریخ انقضا") },
                                readOnly = true,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth().testTag("edit_input_expiry"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    disabledTextColor = Color.White,
                                    disabledLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Select Expiry Date",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                    } else {
                        val subLabel = when (cardType) {
                            "NATIONAL_ID" -> "کد پستی"
                            "SHENASNAMEH" -> "شماره سریال شناسنامه (مثال: الف/۱۲ ۳۴۵۶)"
                            else -> "سایر شناسه‌ها (اختیاری)"
                        }
                        OutlinedTextField(
                            value = secondNumber,
                            onValueChange = { secondNumber = it },
                            label = { Text(subLabel) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_input_second_num"),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    OutlinedTextField(
                        value = shebaNumber,
                        onValueChange = { shebaNumber = it },
                        label = { Text("شماره شبا") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_input_sheba"),
                        maxLines = 1,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text("شماره حساب") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_input_account"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        maxLines = 1,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }

            // Document Image Picking
            Text("آپلود عکس مدرک (اختیاری):", fontWeight = FontWeight.Bold, color = Color.LightGray)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Front Image Picker
                Column(modifier = Modifier.weight(1f)) {
                    Text("روی مدرک:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    val frontBitmap = remember(frontImageBase64) { base64ToBitmap(frontImageBase64) }

                    if (frontBitmap != null) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                            Image(
                                bitmap = frontBitmap.asImageBitmap(),
                                contentDescription = "Front Image Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                            )
                            IconButton(
                                onClick = { frontImageBase64 = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Red.copy(alpha = 0.8f), CircleShape)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                showImageOptionsForFront = true
                            },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SophisticatedSurface)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Camera", tint = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("انتخاب تصویر", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Back Image Picker
                Column(modifier = Modifier.weight(1f)) {
                    Text("پشت مدرک:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    val backBitmap = remember(backImageBase64) { base64ToBitmap(backImageBase64) }

                    if (backBitmap != null) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                            Image(
                                bitmap = backBitmap.asImageBitmap(),
                                contentDescription = "Back Image Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                            )
                            IconButton(
                                onClick = { backImageBase64 = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Red.copy(alpha = 0.8f), CircleShape)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                showImageOptionsForBack = true
                            },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SophisticatedSurface)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Camera", tint = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("انتخاب تصویر", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Dialogs for Front Image Options
            if (showImageOptionsForFront) {
                ImageSourceSelectionDialog(
                    title = "انتخاب تصویر روی مدرک",
                    onDismissRequest = { showImageOptionsForFront = false },
                    onCameraSelect = { checkAndLaunchCamera(true) },
                    onGallerySelect = { checkAndLaunchGallery(true) }
                )
            }

            // Dialogs for Back Image Options
            if (showImageOptionsForBack) {
                ImageSourceSelectionDialog(
                    title = "انتخاب تصویر پشت مدرک",
                    onDismissRequest = { showImageOptionsForBack = false },
                    onCameraSelect = { checkAndLaunchCamera(false) },
                    onGallerySelect = { checkAndLaunchGallery(false) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save & Cancel row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            Toast.makeText(context, "عنوان مدرک نمی‌تواند خالی باشد", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.saveCard(
                            id = card.id,
                            title = title,
                            cardType = cardType,
                            ownerName = ownerName,
                            cardNumber = cardNumber,
                            secondNumber = secondNumber,
                            expiryDate = expiryDate,
                            shebaNumber = shebaNumber,
                            accountNumber = accountNumber,
                            frontImageBase64 = frontImageBase64,
                            backImageBase64 = backImageBase64
                        )
                        Toast.makeText(context, "مدرک با موفقیت بروزرسانی شد", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("edit_save_card_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("بروزرسانی اطلاعات مدرک", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.navigateTo(AppScreen.CARD_DETAIL)
                    },
                    modifier = Modifier
                        .weight(0.5f)
                        .height(54.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                ) {
                    Text("لغو")
                }
            }
        }
    }
    }
}

// --- SCREEN 6: SETTINGS ---
@Composable
fun SettingsScreen(viewModel: WalletViewModel) {
    val context = LocalContext.current
    var pinResetMode by remember { mutableStateOf(false) }
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var biometricEnabled by remember { mutableStateOf(viewModel.isBiometricEnabled()) }
    var autoSyncEnabled by remember { mutableStateOf(viewModel.isAutoSyncEnabled()) }
    var connectedEmail by remember { mutableStateOf(viewModel.getSignedInAccountEmail()) }
    val isBiometricAvailable = viewModel.isBiometricHardwareAvailable()

    BackHandler {
        viewModel.navigateTo(AppScreen.DASHBOARD)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات امنیتی", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SophisticatedDarkBg)
            )
        },
        containerColor = SophisticatedDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "امنیت بیومتریک (اثر انگشت)",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "در صورت پشتیبانی گوشی شما، می‌توانید قفل ورود را با اثر انگشت باز کنید.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isBiometricAvailable) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("فعال‌سازی ورود با اثر انگشت", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = {
                                    biometricEnabled = it
                                    viewModel.setBiometricEnabled(it)
                                    Toast.makeText(context, "تغییرات اثر انگشت ذخیره شد", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("biometric_switch")
                            )
                        }
                    } else {
                        Text(
                            text = "❌ سخت‌افزار اثر انگشت روی این دستگاه یافت نشد یا تعریف نشده است.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Pin reset card
            Card(
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "تغییر پین‌کد ورود",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (!pinResetMode) {
                        Button(
                            onClick = { pinResetMode = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("تغییر پین‌کد ورود", color = Color.White)
                        }
                    } else {
                        OutlinedTextField(
                            value = oldPin,
                            onValueChange = { if (it.length <= 4) oldPin = it },
                            label = { Text("پین‌کد قبلی") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = PasswordVisualTransformation('*'),
                            modifier = Modifier.fillMaxWidth().testTag("old_pin_input")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { if (it.length <= 4) newPin = it },
                            label = { Text("پین‌کد جدید (۴ رقم)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = PasswordVisualTransformation('*'),
                            modifier = Modifier.fillMaxWidth().testTag("new_pin_input")
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (viewModel.securityManager.verifyPin(oldPin)) {
                                        if (newPin.length == 4) {
                                            viewModel.securityManager.savePin(newPin)
                                            Toast.makeText(context, "پین‌کد با موفقیت بروز شد", Toast.LENGTH_SHORT).show()
                                            pinResetMode = false
                                            oldPin = ""
                                            newPin = ""
                                        } else {
                                            Toast.makeText(context, "پین‌کد جدید باید ۴ رقم باشد", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "پین‌کد قبلی نادرست است", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("save_new_pin_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("تایید تغییر")
                            }
                            OutlinedButton(
                                onClick = { pinResetMode = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("لغو")
                            }
                        }
                    }
                }
            }

            // Google Drive Sync Card
            /*
            Card(
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "همگام‌سازی با گوگل درایو",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "برای دسترسی به مدارک از طریق افزونه کروم، حساب گوگل درایو خود را متصل کنید.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val connectLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == android.app.Activity.RESULT_OK) {
                            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
                            try {
                                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                                connectedEmail = account?.email
                                Toast.makeText(context, "با موفقیت متصل شد", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "خطا در ورود به گوگل", Toast.LENGTH_LONG).show()
                            }
                        }
                    }

                    val backupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == android.app.Activity.RESULT_OK) {
                            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
                            try {
                                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                                connectedEmail = account?.email
                                Toast.makeText(context, "در حال ایجاد بکاپ در درایو...", Toast.LENGTH_SHORT).show()
                                viewModel.backupToDrive(account!!) { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "خطا در ورود به گوگل", Toast.LENGTH_LONG).show()
                            }
                        }
                    }

                    val restoreLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == android.app.Activity.RESULT_OK) {
                            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
                            try {
                                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                                connectedEmail = account?.email
                                Toast.makeText(context, "در حال بازگردانی از درایو...", Toast.LENGTH_SHORT).show()
                                viewModel.restoreFromDrive(account!!) { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "خطا در ورود به گوگل", Toast.LENGTH_LONG).show()
                            }
                        }
                    }

                    if (connectedEmail == null) {
                        Button(
                            onClick = {
                                val signInIntent = viewModel.getDriveSignInClient().signInIntent
                                connectLauncher.launch(signInIntent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "Connect", tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اتصال به حساب گوگل درایو", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "متصل به: $connectedEmail",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        viewModel.signOutFromDrive {
                                            connectedEmail = null
                                            autoSyncEnabled = false
                                            viewModel.setAutoSyncEnabled(false)
                                            Toast.makeText(context, "از حساب گوگل خارج شدید.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Text("خروج", color = MaterialTheme.colorScheme.error)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("همگام‌سازی خودکار", color = Color.White)
                                Switch(
                                    checked = autoSyncEnabled,
                                    onCheckedChange = { 
                                        autoSyncEnabled = it
                                        viewModel.setAutoSyncEnabled(it)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color.DarkGray
                                    )
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val signInIntent = viewModel.getDriveSignInClient().signInIntent
                                        backupLauncher.launch(signInIntent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("بکاپ", color = Color.Black)
                                }
                                Button(
                                    onClick = {
                                        val signInIntent = viewModel.getDriveSignInClient().signInIntent
                                        restoreLauncher.launch(signInIntent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text("بازگردانی", color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
            */

            // About vault card
            Card(
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "درباره کیف مدارک امن",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "این برنامه به عنوان یک گاوصندوق آفلاین و امنیتی عمل می‌کند. " +
                                "هیچ اطلاعاتی از این برنامه به اینترنت فرستاده نمی‌شود و تمام تصاویر و شماره کارت‌ها درون خود گوشی شما رمزگذاری و ذخیره می‌شوند. " +
                                "با خیالی آسوده مدارک هویتی خود را ثبت کرده و سریعا به اشتراک بگذارید.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { viewModel.navigateTo(AppScreen.PRIVACY_POLICY) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("سیاست حفظ حریم خصوصی")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.navigateTo(AppScreen.TERMS) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("قوانین و مقررات استفاده")
                    }
                }
            }
        }
    }
}

fun shareBitmap(context: android.content.Context, bitmap: android.graphics.Bitmap) {
    try {
        val file = java.io.File(context.cacheDir, "shared_image_${System.currentTimeMillis()}.jpg")
        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "اشتراک‌گذاری تصویر"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "خطا در اشتراک‌گذاری تصویر", android.widget.Toast.LENGTH_SHORT).show()
    }
}

fun saveBitmapToGallery(context: android.content.Context, bitmap: android.graphics.Bitmap, title: String) {
    try {
        val filename = "${title}_${System.currentTimeMillis()}.jpg"
        var fos: java.io.OutputStream? = null
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
            }
            val imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                fos = resolver.openOutputStream(imageUri)
            }
        } else {
            val imagesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
            val image = java.io.File(imagesDir, filename)
            fos = java.io.FileOutputStream(image)
        }
        
        fos?.use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, it)
            android.widget.Toast.makeText(context, "تصویر در گالری ذخیره شد", android.widget.Toast.LENGTH_SHORT).show()
        } ?: run {
            android.widget.Toast.makeText(context, "خطا در ذخیره تصویر", android.widget.Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "خطا در ذخیره تصویر", android.widget.Toast.LENGTH_SHORT).show()
    }
}

fun createTempImageUri(context: android.content.Context): android.net.Uri {
    val file = java.io.File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
    return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Composable
fun ImageSourceSelectionDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onCameraSelect: () -> Unit,
    onGallerySelect: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp, horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top handle bar
                Box(
                    modifier = Modifier
                        .size(36.dp, 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "لطفاً منبع تصویر مدرک خود را انتخاب کنید:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Camera & Gallery options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Camera tile
                    Surface(
                        onClick = {
                            onDismissRequest()
                            onCameraSelect()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "دوربین",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ثبت با دوربین",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }

                    // Gallery tile
                    Surface(
                        onClick = {
                            onDismissRequest()
                            onGallerySelect()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "گالری",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "انتخاب از گالری",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    Text(
                        text = "انصراف",
                        color = Color.LightGray.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentTypeSelectorBottomSheet(
    selectedType: String,
    onDismissRequest: () -> Unit,
    onTypeSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val types = listOf(
        "BANK_CARD" to "💳 کارت بانکی",
        "NATIONAL_ID" to "🪪 کارت ملی",
        "SHENASNAMEH" to "📜 شناسنامه",
        "PASSPORT" to "🛂 پاسپورت / گذرنامه",
        "MILITARY_CARD" to "🪖 کارت پایان خدمت",
        "DRIVERS_LICENSE" to "🚘 گواهینامه رانندگی",
        "STUDENT_ID" to "🎓 کارت دانشجویی",
        "POSTAL_ADDRESS" to "📮 آدرس پستی",
        "OTHER" to "📂 سایر مدارک"
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = SophisticatedSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "انتخاب نوع مدرک",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(types) { (tKey, tVal) ->
                    val isSelected = selectedType == tKey
                    Surface(
                        onClick = {
                            onTypeSelected(tKey)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("type_sel_$tKey"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else SophisticatedDarkBg,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tVal,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                fontSize = 14.sp
                            )

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "انتخاب شده",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

