import re

with open('app/src/main/java/com/viora/wallet/ui/SecureWalletApp.kt', 'r') as f:
    code = f.read()

pattern = r'\s*//\n\s*viewModel = viewModel,[\s\S]*?backImageBase64 = backImageBase64\n\s*\)'
code = re.sub(pattern, '', code)

with open('app/src/main/java/com/viora/wallet/ui/SecureWalletApp.kt', 'w') as f:
    f.write(code)

