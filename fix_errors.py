import re

# Fix DocumentPickerApp.kt
with open('app/src/main/java/com/example/ui/DocumentPickerApp.kt', 'r') as f:
    content = f.read()

content = content.replace("LoginScreen(viewModel = viewModel)", "LoginScreen(viewModel = viewModel, activity = null)")
content = content.replace("val cards by viewModel.cards", "val cards by viewModel.allCards")
with open('app/src/main/java/com/example/ui/DocumentPickerApp.kt', 'w') as f:
    f.write(content)


# Fix SecureWalletApp.kt
with open('app/src/main/java/com/example/ui/SecureWalletApp.kt', 'r') as f:
    content = f.read()

content = content.replace("backCameraLauncher.launch(null)", """val uri = createTempImageUri(context)
                        tempBackUri = uri
                        backCameraLauncher.launch(uri)""")

with open('app/src/main/java/com/example/ui/SecureWalletApp.kt', 'w') as f:
    f.write(content)
