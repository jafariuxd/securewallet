import re

text = """
5022291029193234
CVV2
1234
03/12
"""

cvv_regex = re.compile(r'(?i)cvv2?\s*[:=\-]?\s*(\d{3,4})')
match = cvv_regex.search(text)
if match:
    print(f"Matched CVV: {match.group(1)}")
else:
    print("No match")
