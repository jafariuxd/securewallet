package com.viora.wallet.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun enhanceBitmapForOcr(original: Bitmap): Bitmap {
    val result = Bitmap.createBitmap(original.width, original.height, original.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    
    val colorMatrix = ColorMatrix()
    // 1. Grayscale
    colorMatrix.setSaturation(0f)
    
    // 2. Increase Contrast (1.5x)
    val contrast = 1.5f
    val brightness = -20f
    val contrastMatrix = ColorMatrix(floatArrayOf(
        contrast, 0f, 0f, 0f, brightness,
        0f, contrast, 0f, 0f, brightness,
        0f, 0f, contrast, 0f, brightness,
        0f, 0f, 0f, 1f, 0f
    ))
    colorMatrix.postConcat(contrastMatrix)
    
    val paint = Paint()
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    
    canvas.drawBitmap(original, 0f, 0f, paint)
    return result
}

suspend fun extractTextFromBitmap(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
    val enhancedBitmap = enhanceBitmapForOcr(bitmap)
    val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
    val image = com.google.mlkit.vision.common.InputImage.fromBitmap(enhancedBitmap, 0)
    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            continuation.resume(visionText.text)
        }
        .addOnFailureListener { e ->
            continuation.resumeWithException(e)
        }
}

fun parseCardInfoFromText(allText: String, cardType: String): ExtractedCardInfo {
    val nationalIdRegex = Regex("\\b\\d{10}\\b")
    // Support expiry formats with optional spaces around slash or dash: 03/12, 1403/12, 03 - 12
    val expiryRegex = Regex("\\b(?:13|14)?\\d{2}\\s*[/\\\\\\-]\\s*\\d{2}\\b")
    
    var cardNumber = ""
    
    if (cardType == "BANK_CARD") {
        // Find 16 digits
        val matches = Regex("(?:\\d[\\s\\-._]*){16,}").findAll(allText)
        val possibleCards = matches.map { it.value.replace(Regex("\\D"), "") }.filter { it.length == 16 }.toList()
        cardNumber = possibleCards.firstOrNull { it.startsWith("6") || it.startsWith("5") } 
            ?: possibleCards.firstOrNull() 
            ?: ""
    } else {
        cardNumber = nationalIdRegex.find(allText)?.value ?: ""
    }
    
    // Find Expiry
    val possibleExpiries = expiryRegex.findAll(allText).map { it.value }.toList()
    var expiryMatch = possibleExpiries.firstOrNull() ?: ""
    // Normalize expiry to remove spaces and fix slashes
    expiryMatch = expiryMatch.replace(Regex("\\s+"), "").replace("\\", "/").replace("-", "/")
    
    var cvv = ""
    val lines = allText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    
    // 1. Look for CVV2 / CVV explicitly in the same line
    for (line in lines) {
        val upperLine = line.uppercase()
        if (upperLine.contains("CVV2") || upperLine.contains("CVV")) {
            val digits = Regex("\\b\\d{3,4}\\b").findAll(line).map { it.value }.toList()
            val validDigits = digits.filter { !cardNumber.contains(it) && !expiryMatch.contains(it) }
            if (validDigits.isNotEmpty()) {
                cvv = validDigits.last()
                break
            }
        }
    }
    
    // 2. Look for CVV2 explicitly in the next lines
    if (cvv.isEmpty()) {
        for (i in lines.indices) {
            val upperLine = lines[i].uppercase()
            if (upperLine == "CVV2" || upperLine == "CVV") {
                if (i + 1 < lines.size) {
                    val nextLine = lines[i+1]
                    val possibleCvv = Regex("^\\d{3,4}$").find(nextLine)?.value
                    if (possibleCvv != null) {
                        cvv = possibleCvv
                        break
                    }
                }
            }
        }
    }
    
    // 3. Fallback to any 3-4 digit number
    if (cvv.isEmpty()) { 
         val cvvRegex = Regex("\\b\\d{3,4}\\b")
         val possibleCvvs = cvvRegex.findAll(allText).map { it.value }.toList()
         val dateParts = expiryMatch.split("/")
         cvv = possibleCvvs.firstOrNull { 
             it.length in 3..4 && 
             it != expiryMatch && 
             !dateParts.contains(it) && 
             !cardNumber.contains(it) &&
             // Avoid classifying year 140x/139x as CVV
             !(it.length == 4 && (it.startsWith("140") || it.startsWith("139")))
         } ?: ""
    }
    
    var title = ""
    var shebaMatchStr = ""
    if (cardType == "BANK_CARD") {
        title = getBankNameFromCardNumber(cardNumber) ?: "کارت بانکی"
        val allTextNoSpaces = allText.replace(Regex("\\s+"), "")
        val shebaExtracted = Regex("(?:IR)?\\d{24}").find(allTextNoSpaces)?.value ?: ""
        shebaMatchStr = if (shebaExtracted.isNotEmpty() && !shebaExtracted.startsWith("IR")) {
            "IR$shebaExtracted"
        } else {
            shebaExtracted
        }
    } else if (cardType == "NATIONAL_ID") {
        title = "کارت ملی"
    } else if (cardType == "SHENASNAMEH") {
        title = "شناسنامه"
    }
    
    return ExtractedCardInfo(
        title = title,
        cardNumber = cardNumber,
        ownerName = "امکان استخراج نام آفلاین نیست", 
        secondNumber = cvv,
        expiryDate = expiryMatch,
        shebaNumber = shebaMatchStr
    )
}

fun getBankNameFromCardNumber(cardNumber: String): String? {
    if (cardNumber.length < 6) return null
    val prefix = cardNumber.substring(0, 6)
    return when(prefix) {
        "603799" -> "بانک ملی ایران"
        "589210" -> "بانک سپه"
        "603769" -> "بانک صادرات"
        "610433" -> "بانک ملت"
        "627353", "585983" -> "بانک تجارت"
        "502229", "639347" -> "بانک پاسارگاد"
        "621986" -> "بانک سامان"
        "622106", "627884", "639981" -> "بانک پارسیان"
        "603770", "639217" -> "بانک کشاورزی"
        "589463" -> "بانک رفاه کارگران"
        "628023" -> "بانک مسکن"
        "636214" -> "بانک آینده"
        "627412" -> "بانک اقتصاد نوین"
        "502806", "504706" -> "بانک شهر"
        "639346" -> "بانک سینا"
        "502938" -> "بانک دی"
        "639607" -> "بانک سرمایه"
        "627488", "502910" -> "بانک کارآفرین"
        "505416" -> "بانک گردشگری"
        "502908" -> "بانک توسعه تعاون"
        "627760" -> "پست بانک ایران"
        "627961" -> "بانک صنعت و معدن"
        "627648" -> "بانک توسعه صادرات"
        "505809" -> "بانک خاورمیانه"
        "505785" -> "بانک ایران زمین"
        "504172" -> "بانک قرض الحسنه رسالت"
        "606373", "639370" -> "بانک قرض الحسنه مهر ایران"
        else -> null
    }
}
