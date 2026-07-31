import re

with open('app/src/main/java/com/viora/wallet/ui/SecureWalletApp.kt', 'r') as f:
    content = f.read()

target = r'(if \(!extracted\.expiryDate\.isNullOrBlank\(\)\) expiryDate = extracted\.expiryDate)'
replacement = r'\1\n                        if (!extracted.shebaNumber.isNullOrBlank()) shebaNumber = extracted.shebaNumber\n                        if (!extracted.accountNumber.isNullOrBlank()) accountNumber = extracted.accountNumber'

# Some blocks have different indentations. It's better to just use a simpler replacement without forcing indentation, or match the indentation.

def replacer(match):
    indent = match.group(1)
    expr = match.group(2)
    return f"{indent}{expr}\n{indent}if (!extracted.shebaNumber.isNullOrBlank()) shebaNumber = extracted.shebaNumber\n{indent}if (!extracted.accountNumber.isNullOrBlank()) accountNumber = extracted.accountNumber"

content = re.sub(r'([ \t]+)(if \(!extracted\.expiryDate\.isNullOrBlank\(\)\) expiryDate = extracted\.expiryDate)', replacer, content)

with open('app/src/main/java/com/viora/wallet/ui/SecureWalletApp.kt', 'w') as f:
    f.write(content)

print("Replaced!")
