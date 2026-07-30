package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import com.example.ui.SecureWalletApp
import com.example.ui.WalletViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : AppCompatActivity() {
  private val viewModel: WalletViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
    )
    
    val isPickerMode = intent?.action == android.content.Intent.ACTION_GET_CONTENT || intent?.action == android.content.Intent.ACTION_PICK

    setContent {
      MyApplicationTheme {
        if (isPickerMode) {
          com.example.ui.DocumentPickerApp(viewModel = viewModel, onImagePicked = { uri ->
              setResult(RESULT_OK, android.content.Intent().apply { 
                  data = uri 
                  addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
              })
              finish()
          }, onCancel = {
              setResult(RESULT_CANCELED)
              finish()
          })
        } else {
          SecureWalletApp(viewModel = viewModel)
        }
      }
    }
  }
}
