with open('app/src/main/java/com/viora/wallet/ui/OcrHelper.kt', 'r') as f:
    lines = f.readlines()

with open('app/src/main/java/com/viora/wallet/ui/OcrHelper.kt', 'w') as f:
    for line in lines:
        if 'val lines = allText.split("' in line and '")' not in line:
            f.write('        val lines = allText.split("\\n")\n')
        elif '")' in line and line.strip() == '")':
            pass
        else:
            f.write(line)
