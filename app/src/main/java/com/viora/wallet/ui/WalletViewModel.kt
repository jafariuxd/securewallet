package com.viora.wallet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.viora.wallet.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

enum class AppScreen {
    LOGIN,
    FIRST_TIME_SETUP,
    DASHBOARD,
    CARD_DETAIL,
    ADD_EDIT_CARD,
    EDIT_CARD,
    SETTINGS,
    PRIVACY_POLICY,
    TERMS,
    LIVE_SCANNER
}

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = WalletRepository(database.walletCardDao())
    val securityManager = SecurityManager(application)
    private val driveSyncManager = DriveSyncManager(application)

    fun getDriveSignInClient() = driveSyncManager.getSignInClient()

    fun getSignedInAccountEmail(): String? {
        val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(getApplication())
        return account?.email
    }

    fun signOutFromDrive(onComplete: () -> Unit) {
        val client = getDriveSignInClient()
        client.signOut().addOnCompleteListener {
            onComplete()
        }
    }

    fun backupToDrive(account: GoogleSignInAccount, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val cards = repository.getAllCardsList()
                val result = driveSyncManager.backupDataToDrive(account, cards)
                result.onSuccess { 
                    onResult(it)
                }.onFailure {
                    onResult("خطا در همگام‌سازی: ${it.message}")
                }
            } catch (e: Exception) {
                onResult("خطا: ${e.message}")
            }
        }
    }

    fun restoreFromDrive(account: GoogleSignInAccount, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = driveSyncManager.restoreDataFromDrive(account)
                result.onSuccess { cards ->
                    cards.forEach { card ->
                        repository.insertCard(card.copy(id = 0)) 
                    }
                    onResult("بازگردانی با موفقیت انجام شد.")
                }.onFailure {
                    onResult("خطا در بازگردانی: ${it.message}")
                }
            } catch (e: Exception) {
                onResult("خطا: ${e.message}")
            }
        }
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
        return securityManager.isAutoSyncEnabled()
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        securityManager.setAutoSyncEnabled(enabled)
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
            
            autoBackupIfSignedIn()

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
            
            autoBackupIfSignedIn()
        }
    }

    private fun autoBackupIfSignedIn() {
        if (!securityManager.isAutoSyncEnabled()) return
        val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(getApplication())
        if (account != null) {
            backupToDrive(account) { _ -> }
        }
    }
}
