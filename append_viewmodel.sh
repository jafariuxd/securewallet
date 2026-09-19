cat << 'INNER_EOF' >> app/src/main/java/com/viora/wallet/ui/WalletViewModel.kt
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
INNER_EOF
