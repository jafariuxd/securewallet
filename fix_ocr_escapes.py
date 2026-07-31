import re

with open('app/src/main/java/com/viora/wallet/ui/OcrHelper.kt', 'r') as f:
    content = f.read()

content = content.replace(r'Regex("\b\d{10}\b")', r'Regex("\\b\\d{10}\\b")')
content = content.replace(r'Regex("\b(?:13|14)?\d{2}\s*[/\\-]\s*\d{2}\b")', r'Regex("\\b(?:13|14)?\\d{2}\\s*[/\\\\\\-]\\s*\\d{2}\\b")')
content = content.replace(r'Regex("\D")', r'Regex("\\D")')
content = content.replace(r'Regex("[56]\d{15}")', r'Regex("[56]\\d{15}")')
content = content.replace(r'Regex("\d{16}")', r'Regex("\\d{16}")')
content = content.replace(r'Regex("\s+")', r'Regex("\\s+")')
content = content.replace(r'Regex("(?:IR)?\d{24}")', r'Regex("(?:IR)?\\d{24}")')
content = content.replace(r'Regex("\d{8,16}")', r'Regex("\\d{8,16}")')
content = content.replace(r'Regex("(?i)cvv2?\s*[:=\-]?\s*(\d{3,4})")', r'Regex("(?i)cvv2?\\s*[:=\\-]?\\s*(\\d{3,4})")')
content = content.replace(r'Regex("\b\d{3,4}\b")', r'Regex("\\b\\d{3,4}\\b")')

content = content.replace(r'.split("")', r'.split("\n")')
content = content.replace(r'replace("\", "/")', r'replace("\\", "/")')

with open('app/src/main/java/com/viora/wallet/ui/OcrHelper.kt', 'w') as f:
    f.write(content)

