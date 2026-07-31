import re

with open('app/src/main/java/com/viora/wallet/ui/OcrHelper.kt', 'r') as f:
    content = f.read()

# I will just write a new parseCardInfoFromText

new_parse = """fun parseCardInfoFromText(allTextRaw: String, cardType: String): ExtractedCardInfo {
    val allText = normalizeDigits(allTextRaw)
    val nationalIdRegex = Regex("\\\\b\\\\d{10}\\\\b")
    // Support expiry formats with optional spaces around slash or dash: 03/12, 1403/12, 03 - 12
    val expiryRegex = Regex("\\\\b(?:13|14)?\\\\d{2}\\\\s*[/\\\\\\\\-]\\\\s*\\\\d{2}\\\\b")
    
    var cardNumber = ""
    var shebaMatchStr = ""
    var accountNumber = ""
    var title = ""

    if (cardType == "BANK_CARD") {
        // Find 16 digits by stripping all non-digits first
        val allDigits = allText.replace(Regex("\\\\D"), "")
        val matcher = Regex("[56]\\\\d{15}").find(allDigits)
        if (matcher != null) {
            cardNumber = matcher.value
        } else {
            cardNumber = Regex("\\\\d{16}").find(allDigits)?.value ?: ""
        }
        
        // Extract Sheba
        val allTextNoSpaces = allText.replace(Regex("\\\\s+"), "")
        val shebaExtracted = Regex("(?:IR)?\\\\d{24}").find(allTextNoSpaces)?.value ?: ""
        shebaMatchStr = if (shebaExtracted.isNotEmpty() && !shebaExtracted.startsWith("IR")) {
            "IR$shebaExtracted"
        } else {
            shebaExtracted
        }
        
        // Extract Account Number (find lines with "حساب" or "شماره حساب")
        val lines = allText.split("\\n")
        for (i in lines.indices) {
            val line = lines[i]
            if (line.contains("حساب")) {
                val digitsInSameLine = Regex("\\\\d{8,16}").find(line.replace(Regex("\\\\D"), ""))?.value
                if (digitsInSameLine != null && digitsInSameLine != cardNumber && !shebaMatchStr.contains(digitsInSameLine)) {
                    accountNumber = digitsInSameLine
                    break
                }
                if (i + 1 < lines.size) {
                    val nextLine = lines[i+1]
                    val digitsInNextLine = Regex("\\\\d{8,16}").find(nextLine.replace(Regex("\\\\D"), ""))?.value
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
    expiryMatch = expiryMatch.replace(Regex("\\\\s+"), "").replace("\\\\", "/").replace("-", "/")
    
    // Find CVV
    var cvv = ""
    val cvvRegex = Regex("(?i)cvv2?\\\\s*[:=\\\\-]?\\\\s*(\\\\d{3,4})")
    val cvvMatch = cvvRegex.find(allText)
    if (cvvMatch != null) {
        cvv = cvvMatch.groupValues[1]
    }
    
    // Fallback to any 3-4 digit number
    if (cvv.isEmpty() && cardType == "BANK_CARD") {
         val cvvRegexFallback = Regex("\\\\b\\\\d{3,4}\\\\b")
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
}"""

content = re.sub(r'fun parseCardInfoFromText\(.*?\n\}', new_parse, content, flags=re.DOTALL)

with open('app/src/main/java/com/viora/wallet/ui/OcrHelper.kt', 'w') as f:
    f.write(content)
