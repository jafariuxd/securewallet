package com.viora.wallet.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrHelperTest {
    @Test
    fun testRegex() {
        val persianNumber = "۶۰۳۷۹۹۱۱۱۲۳۴۵۶۷۸"
        val allDigits = persianNumber.replace(Regex("\\D"), "")
        println("All digits: " + allDigits)
    }
}
