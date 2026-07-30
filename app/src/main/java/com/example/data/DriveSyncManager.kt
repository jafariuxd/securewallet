package com.example.data

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.client.http.ByteArrayContent
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections
import java.io.ByteArrayOutputStream

class DriveSyncManager(private val context: Context) {

    fun getSignInClient(): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        return GoogleSignIn.getClient(context, signInOptions)
    }

    private fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
        .setApplicationName("Secure Wallet")
        .build()
    }

    private fun findFileByName(driveService: Drive, name: String): String? {
        val query = "name='$name' and 'root' in parents and trashed=false"
        val fileList = driveService.files().list().setQ(query).setSpaces("drive").execute()
        return if (fileList.files.isNotEmpty()) fileList.files[0].id else null
    }

    private fun uploadTextFile(driveService: Drive, name: String, contentStr: String) {
        if (contentStr.isEmpty()) return
        val existingId = findFileByName(driveService, name)
        val fileContent = ByteArrayContent.fromString("text/plain", contentStr)
        if (existingId != null) {
            driveService.files().update(existingId, null, fileContent).execute()
        } else {
            val fileMetadata = File().apply {
                this.name = name
                mimeType = "text/plain"
            }
            driveService.files().create(fileMetadata, fileContent).execute()
        }
    }

    private fun downloadTextFile(driveService: Drive, name: String): String? {
        val existingId = findFileByName(driveService, name) ?: return null
        val outputStream = ByteArrayOutputStream()
        driveService.files().get(existingId).executeMediaAndDownloadTo(outputStream)
        return String(outputStream.toByteArray())
    }

    suspend fun backupDataToDrive(account: GoogleSignInAccount, cards: List<WalletCard>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(account)
            
            // 1. Separate metadata from images
            val metadataList = cards.map { 
                it.copy(frontImageBase64 = null, backImageBase64 = null) 
            }
            val jsonMetadata = Gson().toJson(metadataList)
            
            // Upload index
            val existingIndexId = findFileByName(driveService, "wallet_index.json")
            val indexContent = ByteArrayContent.fromString("application/json", jsonMetadata)
            
            if (existingIndexId != null) {
                driveService.files().update(existingIndexId, null, indexContent).execute()
            } else {
                val fileMetadata = File().apply {
                    name = "wallet_index.json"
                    mimeType = "application/json"
                }
                driveService.files().create(fileMetadata, indexContent).execute()
            }

            // 2. Upload images separately
            cards.forEach { card ->
                if (!card.frontImageBase64.isNullOrEmpty()) {
                    uploadTextFile(driveService, "img_front_${card.id}.txt", card.frontImageBase64)
                }
                if (!card.backImageBase64.isNullOrEmpty()) {
                    uploadTextFile(driveService, "img_back_${card.id}.txt", card.backImageBase64)
                }
            }

            Result.success("بکاپ (همراه با جداسازی تصاویر) با موفقیت در گوگل درایو آپدیت شد.")
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun restoreDataFromDrive(account: GoogleSignInAccount): Result<List<WalletCard>> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(account)
            val indexId = findFileByName(driveService, "wallet_index.json") 
                ?: return@withContext Result.failure(Exception("فایل ایندکس بکاپ یافت نشد."))
            
            val outputStream = ByteArrayOutputStream()
            driveService.files().get(indexId).executeMediaAndDownloadTo(outputStream)
            val jsonStr = String(outputStream.toByteArray())
            
            val type = object : com.google.gson.reflect.TypeToken<List<WalletCard>>() {}.type
            val metadataCards: List<WalletCard> = Gson().fromJson(jsonStr, type)
            
            // Now download images for each card
            val fullCards = metadataCards.map { card ->
                val frontImg = downloadTextFile(driveService, "img_front_${card.id}.txt")
                val backImg = downloadTextFile(driveService, "img_back_${card.id}.txt")
                card.copy(frontImageBase64 = frontImg, backImageBase64 = backImg)
            }
            
            Result.success(fullCards)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
