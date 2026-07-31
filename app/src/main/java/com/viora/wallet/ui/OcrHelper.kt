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
    // 1. Scale up for better OCR on small details
    val scale = 2.0f
    val matrix = android.graphics.Matrix()
    matrix.postScale(scale, scale)
    val scaledBitmap = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)

    val result = Bitmap.createBitmap(scaledBitmap.width, scaledBitmap.height, scaledBitmap.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    
    val colorMatrix = ColorMatrix()
    // 2. Grayscale
    colorMatrix.setSaturation(0f)
    
    // 3. Increase Contrast (1.5x)
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
    
    canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)
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

fun normalizeDigits(text: String): String {
    var result = text
    val persianDigits = arrayOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹")
    val arabicDigits = arrayOf("٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩")
    for (i in 0..9) {
        result = result.replace(persianDigits[i], i.toString())
        result = result.replace(arabicDigits[i], i.toString())
    }
    return result
}

fun parseCardInfoFromText(allTextRaw: String, cardType: String): ExtractedCardInfo {
    val allText = normalizeDigits(allTextRaw)
    val nationalIdRegex = Regex("\\b\\d{10}\\b")
    // Support expiry formats with optional spaces around slash or dash: 03/12, 1403/12, 03 - 12
    val expiryRegex = Regex("\\b(?:13|14)?\\d{2}\\s*[/\\\\\\-]\\s*\\d{2}\\b")
    
    var cardNumber = ""
    var shebaMatchStr = ""
    var accountNumber = ""
    var title = ""

    if (cardType == "BANK_CARD") {
        // Find 16 digits by stripping all non-digits first
        val allDigits = allText.replace(Regex("\\D"), "")
        val matcher = Regex("[56]\\d{15}").find(allDigits)
        if (matcher != null) {
            cardNumber = matcher.value
        } else {
            cardNumber = Regex("\\d{16}").find(allDigits)?.value ?: ""
        }
        
        // Extract Sheba
        val allTextNoSpaces = allText.replace(Regex("\\s+"), "")
        val shebaExtracted = Regex("(?:IR)?\\d{24}").find(allTextNoSpaces)?.value ?: ""
        shebaMatchStr = if (shebaExtracted.isNotEmpty() && !shebaExtracted.startsWith("IR")) {
            "IR$shebaExtracted"
        } else {
            shebaExtracted
        }
        
        // Extract Account Number (find lines with "حساب" or "شماره حساب")
        val lines = allText.split("\n")
        for (i in lines.indices) {
            val line = lines[i]
            if (line.contains("حساب")) {
                val digitsInSameLine = Regex("\\d{8,16}").find(line.replace(Regex("\\D"), ""))?.value
                if (digitsInSameLine != null && digitsInSameLine != cardNumber && !shebaMatchStr.contains(digitsInSameLine)) {
                    accountNumber = digitsInSameLine
                    break
                }
                if (i + 1 < lines.size) {
                    val nextLine = lines[i+1]
                    val digitsInNextLine = Regex("\\d{8,16}").find(nextLine.replace(Regex("\\D"), ""))?.value
                    if (digitsInNextLine != null && digitsInNextLine != cardNumber && !shebaMatchStr.contains(digitsInNextLine)) {
                        accountNumber = digitsInNextLine
                        break
                    }
                }
            }
        }
        
        title = getBankNameFromCardNumber(cardNumber) ?: "کارت بانکی"
    } else {
        cardNumber = nationalIdRegex.find(allText)?.value ?: ""
        if (cardType == "NATIONAL_ID") {
            title = "کارت ملی"
        } else if (cardType == "SHENASNAMEH") {
            title = "شناسنامه"
        }
    }
    
    // Find Expiry
    val possibleExpiries = expiryRegex.findAll(allText).map { it.value }.toList()
    var expiryMatch = possibleExpiries.firstOrNull() ?: ""
    expiryMatch = expiryMatch.replace(Regex("\\s+"), "").replace("\\", "/").replace("-", "/")
    
    // Find CVV
    var cvv = ""
    val cvvRegex = Regex("(?i)cvv2?\\s*[:=\\-]?\\s*(\\d{3,4})")
    val cvvMatch = cvvRegex.find(allText)
    if (cvvMatch != null) {
        cvv = cvvMatch.groupValues[1]
    }
    
    // Fallback to any 3-4 digit number
    if (cvv.isEmpty() && cardType == "BANK_CARD") {
         val cvvRegexFallback = Regex("\\b\\d{3,4}\\b")
         val possibleCvvs = cvvRegexFallback.findAll(allText).map { it.value }.toList()
         val dateParts = expiryMatch.split("/")
         cvv = possibleCvvs.firstOrNull {
              it.length in 3..4 &&
              it != expiryMatch &&
              !dateParts.contains(it) &&
              !cardNumber.contains(it) &&
              (shebaMatchStr.isEmpty() || !shebaMatchStr.contains(it)) &&
              (accountNumber.isEmpty() || !accountNumber.contains(it)) &&
              !(it.length == 4 && (it.startsWith("140") || it.startsWith("139")))
         } ?: ""
    }
    
    return ExtractedCardInfo(
        title = title,
        cardNumber = cardNumber,
        ownerName = "امکان استخراج نام آفلاین نیست", 
        secondNumber = cvv,
        expiryDate = expiryMatch,
        shebaNumber = shebaMatchStr,
        accountNumber = accountNumber
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
