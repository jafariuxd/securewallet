package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class SecurityManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)

    fun hasPin(): Boolean {
        return prefs.getString("master_pin", null) != null
    }

    fun savePin(pin: String) {
        prefs.edit().putString("master_pin", pin).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val savedPin = prefs.getString("master_pin", null)
        return savedPin == pin
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean("biometric_enabled", false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    fun isAutoSyncEnabled(): Boolean {
        return prefs.getBoolean("auto_sync_enabled", false)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_sync_enabled", enabled).apply()
    }

    fun isBiometricHardwareAvailable(): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                BiometricManager.BIOMETRIC_SUCCESS -> true
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun authenticateBiometric(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("احراز هویت ناموفق بود")
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("ورود امن")
            .setSubtitle("با استفاده از اثر انگشت وارد شوید")
            .setNegativeButtonText("استفاده از پین")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
