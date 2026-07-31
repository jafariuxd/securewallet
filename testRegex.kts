val sample1 = "CVV2 1234"
val sample2 = "cvv2: 456"
val sample3 = "CVV:7890"
val sample4 = "CVV21234"
val sample5 = "cvv 123"

val cvvRegex = Regex("(?i)cvv2?\\s*[:=]?\\s*(\\d{3,4})")

println(cvvRegex.find(sample1)?.groupValues?.get(1))
println(cvvRegex.find(sample2)?.groupValues?.get(1))
println(cvvRegex.find(sample3)?.groupValues?.get(1))
println(cvvRegex.find(sample4)?.groupValues?.get(1))
println(cvvRegex.find(sample5)?.groupValues?.get(1))
