import re

samples = [
    "CVV2 1234",
    "cvv2: 456",
    "CVV:7890",
    "CVV21234",
    "cvv 123",
    "CVV2 \n 1234",
    "cvv2  \n456"
]

cvv_regex = re.compile(r'(?i)cvv2?\s*[:=-]?\s*(\d{3,4})')

for s in samples:
    match = cvv_regex.search(s)
    print(f"'{s}' -> {match.group(1) if match else 'None'}")
