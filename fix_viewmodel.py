import sys

with open('app/src/main/java/com/viora/wallet/ui/WalletViewModel.kt', 'r') as f:
    lines = f.read().splitlines()

# find the last '}' that closes the class
# The class starts at: class WalletViewModel(application: Application) : AndroidViewModel(application) {
# It probably closes near the end. Let's find the original last '}' before the appended stuff.
for i in range(len(lines)):
    if lines[i].strip() == "fun backupToUri(uri: android.net.Uri, password: String, context: android.content.Context, onResult: (Boolean, String) -> Unit) {":
        # The line before this should be '}'
        # let's remove the '}' and put it at the very end
        idx = i - 1
        while idx > 0 and lines[idx].strip() != '}':
            idx -= 1
        
        if idx > 0 and lines[idx].strip() == '}':
            lines[idx] = '' # remove the closing brace of the class
            lines.append('}') # add it to the end
            break

with open('app/src/main/java/com/viora/wallet/ui/WalletViewModel.kt', 'w') as f:
    f.write("\n".join(lines) + "\n")
