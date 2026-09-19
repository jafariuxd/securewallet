package com.viora.wallet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.viora.wallet.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppScreen {
    LOGIN,
    FIRST_TIME_SETUP,
    DASHBOARD,
    CARD_DETAIL,
    ADD_EDIT_CARD,
    EDIT_CARD,
    SETTINGS,
    PRIVACY_POLICY,
    TERMS
}

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = WalletRepository(database.walletCardDao())
    val securityManager = SecurityManager(application)

    fun getSignedInAccountEmail(): String? {
        return null
    }

    fun signOutFromDrive(onComplete: () -> Unit) {
        onComplete()
    }

    // Screen State
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.LOGIN)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Cards State
    val allCards: StateFlow<List<WalletCard>> = repository.allCards
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Selected Card for Details
    private val _selectedCard = MutableStateFlow<WalletCard?>(null)
    val selectedCard: StateFlow<WalletCard?> = _selectedCard.asStateFlow()

    // Authentication State
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    init {
        // Decide start screen
        if (!securityManager.hasPin()) {
            _currentScreen.value = AppScreen.FIRST_TIME_SETUP
        } else {
            _currentScreen.value = AppScreen.LOGIN
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectCard(card: WalletCard?) {
        _selectedCard.value = card
        if (card != null) {
            _currentScreen.value = AppScreen.CARD_DETAIL
        }
    }

    // Security Actions
    fun setupPin(pin: String) {
        if (pin.length == 4) {
            securityManager.savePin(pin)
            _isAuthenticated.value = true
            _currentScreen.value = AppScreen.DASHBOARD
        } else {
            _pinError.value = "پین باید ۴ رقمی باشد"
        }
    }

    fun loginWithPin(pin: String): Boolean {
        if (securityManager.verifyPin(pin)) {
            _isAuthenticated.value = true
            _pinError.value = null
            _currentScreen.value = AppScreen.DASHBOARD
            return true
        } else {
            _pinError.value = "پین وارد شده نادرست است"
            return false
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securityManager.setBiometricEnabled(enabled)
    }

    fun isBiometricEnabled(): Boolean {
        return securityManager.isBiometricEnabled()
    }

    fun isBiometricHardwareAvailable(): Boolean {
        return securityManager.isBiometricHardwareAvailable()
    }

    fun isAutoSyncEnabled(): Boolean {
        return false
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
    }

    fun setAuthenticatedDirectly() {
        _isAuthenticated.value = true
        _currentScreen.value = AppScreen.DASHBOARD
    }

    fun logout() {
        _isAuthenticated.value = false
        _currentScreen.value = AppScreen.LOGIN
    }

    // Database Actions
    fun saveCard(
        id: Int = 0,
        title: String,
        cardType: String,
        ownerName: String,
        cardNumber: String?,
        secondNumber: String?,
        expiryDate: String?,
        shebaNumber: String?,
        accountNumber: String?,
        frontImageBase64: String?,
        backImageBase64: String?,
        additionalImagesJson: String? = null,
        extraFieldsJson: String? = null
    ) {
        viewModelScope.launch {
            val card = WalletCard(
                id = id,
                title = title,
                cardType = cardType,
                ownerName = ownerName,
                cardNumber = cardNumber,
                secondNumber = secondNumber,
                expiryDate = expiryDate,
                shebaNumber = shebaNumber,
                accountNumber = accountNumber,
                frontImageBase64 = frontImageBase64,
                backImageBase64 = backImageBase64,
                additionalImagesJson = additionalImagesJson,
                extraFieldsJson = extraFieldsJson,
                timestamp = System.currentTimeMillis()
            )
            if (id == 0) {
                repository.insertCard(card)
            } else {
                repository.updateCard(card)
            }
            
            // autoBackupIfSignedIn() -> Removed for offline-only

            // Navigate back to dashboard or details
            if (id != 0 && _selectedCard.value?.id == id) {
                _selectedCard.value = card
                _currentScreen.value = AppScreen.CARD_DETAIL
            } else {
                _currentScreen.value = AppScreen.DASHBOARD
            }
        }
    }

    fun deleteCard(card: WalletCard) {
        viewModelScope.launch {
            repository.deleteCard(card)
            _selectedCard.value = null
            _currentScreen.value = AppScreen.DASHBOARD
            
            // autoBackupIfSignedIn() -> Removed for offline-only
        }
    }

    fun backupToUri(uri: android.net.Uri, password: String, context: android.content.Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val cards = repository.getAllCardsList()
                val gson = com.google.gson.Gson()
                val jsonString = gson.toJson(cards)
                
                val encryptedBytes = encryptData(jsonString, password)
                
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(encryptedBytes)
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(true, "بکاپ با موفقیت ایجاد شد")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "خطا در ایجاد بکاپ: ${e.message}")
                }
            }
        }
    }
    
    fun restoreFromUri(uri: android.net.Uri, password: String, context: android.content.Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(false, "خطا در خواندن فایل")
                    }
                    return@launch
                }
                
                val decryptedJson = decryptData(bytes, password)
                
                val gson = com.google.gson.Gson()
                val listType = object : com.google.gson.reflect.TypeToken<List<WalletCard>>() {}.type
                val cards: List<WalletCard> = gson.fromJson(decryptedJson, listType)
                
                for (card in cards) {
                    repository.insertCard(card.copy(id = 0))
                }
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(true, "بکاپ با موفقیت بازیابی شد")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "رمز عبور اشتباه است یا فایل نامعتبر است.")
                }
            }
        }
    }
    
    private fun encryptData(plainText: String, password: String): ByteArray {
        val secretKey = generateKey(password)
        val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16).apply { java.security.SecureRandom().nextBytes(this) }
        val ivSpec = javax.crypto.spec.IvParameterSpec(iv)
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val cipherText = cipher.doFinal(plainText.toByteArray(kotlin.text.Charsets.UTF_8))
        return iv + cipherText
    }

    private fun decryptData(cipherData: ByteArray, password: String): String {
        val secretKey = generateKey(password)
        val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = cipherData.copyOfRange(0, 16)
        val cipherText = cipherData.copyOfRange(16, cipherData.size)
        val ivSpec = javax.crypto.spec.IvParameterSpec(iv)
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val plainText = cipher.doFinal(cipherText)
        return String(plainText, kotlin.text.Charsets.UTF_8)
    }

    private fun generateKey(password: String): javax.crypto.SecretKey {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(password.toByteArray(kotlin.text.Charsets.UTF_8))
        return javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
    }
}
