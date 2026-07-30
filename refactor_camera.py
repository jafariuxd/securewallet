import re

with open('app/src/main/java/com/example/ui/SecureWalletApp.kt', 'r') as f:
    content = f.read()

# Add a createTempImageUri function at the end of the file
if 'fun createTempImageUri' not in content:
    content += """
fun createTempImageUri(context: android.content.Context): android.net.Uri {
    val file = java.io.File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
    return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
"""

# Replace in AddEditCardScreen and EditCardScreen
# We need to add `var tempFrontUri by remember { mutableStateOf<android.net.Uri?>(null) }`
# `var tempBackUri by remember { mutableStateOf<android.net.Uri?>(null) }`

content = re.sub(r'var isScanning by remember { mutableStateOf\(false\) }', 
    r'var isScanning by remember { mutableStateOf(false) }\n    var tempFrontUri by remember { mutableStateOf<android.net.Uri?>(null) }\n    var tempBackUri by remember { mutableStateOf<android.net.Uri?>(null) }', content)

# frontCameraLauncher
front_launcher_regex = re.compile(
r'''val frontCameraLauncher = rememberLauncherForActivityResult\(
\s*contract = ActivityResultContracts\.TakePicturePreview\(\)
\s*\) \{ bitmap ->
\s*if \(bitmap != null\) \{
\s*val outputStream = ByteArrayOutputStream\(\)
\s*bitmap\.compress\(Bitmap\.CompressFormat\.JPEG, 70, outputStream\)
\s*val compressedBytes = outputStream\.toByteArray\(\)
\s*frontImageBase64 = android\.util\.Base64\.encodeToString\(compressedBytes, android\.util\.Base64\.DEFAULT\)
\s*\} else \{
\s*try \{
\s*val intent = android\.content\.Intent\(android\.provider\.MediaStore\.ACTION_IMAGE_CAPTURE\)
\s*frontCameraIntentLauncher\.launch\(intent\)
\s*\} catch \(e: Exception\) \{
\s*Toast\.makeText\(context, "دریافت دوربین: \$\{e\.message \?: "Unknown"\}", Toast\.LENGTH_LONG\)\.show\(\)
\s*\}
\s*\}
\s*\}'''
)

replacement_front = """val frontCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempFrontUri != null) {
            frontImageBase64 = uriToBase64(context, tempFrontUri!!)
        }
    }"""

content = front_launcher_regex.sub(replacement_front, content)

# backCameraLauncher
back_launcher_regex = re.compile(
r'''val backCameraLauncher = rememberLauncherForActivityResult\(
\s*contract = ActivityResultContracts\.TakePicturePreview\(\)
\s*\) \{ bitmap ->
\s*if \(bitmap != null\) \{
\s*val outputStream = ByteArrayOutputStream\(\)
\s*bitmap\.compress\(Bitmap\.CompressFormat\.JPEG, 70, outputStream\)
\s*val compressedBytes = outputStream\.toByteArray\(\)
\s*backImageBase64 = android\.util\.Base64\.encodeToString\(compressedBytes, android\.util\.Base64\.DEFAULT\)
\s*\} else \{
\s*try \{
\s*val intent = android\.content\.Intent\(android\.provider\.MediaStore\.ACTION_IMAGE_CAPTURE\)
\s*backCameraIntentLauncher\.launch\(intent\)
\s*\} catch \(e: Exception\) \{
\s*Toast\.makeText\(context, "دریافت دوربین: \$\{e\.message \?: "Unknown"\}", Toast\.LENGTH_LONG\)\.show\(\)
\s*\}
\s*\}
\s*\}'''
)

replacement_back = """val backCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempBackUri != null) {
            backImageBase64 = uriToBase64(context, tempBackUri!!)
        }
    }"""

content = back_launcher_regex.sub(replacement_back, content)

# Replace launch sites
content = content.replace("if (isFront) frontCameraLauncher.launch(null) else backCameraLauncher.launch(null)", """if (isFront) {
                    val uri = createTempImageUri(context)
                    tempFrontUri = uri
                    frontCameraLauncher.launch(uri)
                } else {
                    val uri = createTempImageUri(context)
                    tempBackUri = uri
                    backCameraLauncher.launch(uri)
                }""")
                
content = content.replace("frontCameraLauncher.launch(null)", """val uri = createTempImageUri(context)
                        tempFrontUri = uri
                        frontCameraLauncher.launch(uri)""")

with open('app/src/main/java/com/example/ui/SecureWalletApp.kt', 'w') as f:
    f.write(content)
