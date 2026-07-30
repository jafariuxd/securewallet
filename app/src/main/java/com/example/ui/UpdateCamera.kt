package com.example.ui

import com.example.data.WalletCard

fun saveFormToViewModelAndNavigate(
    viewModel: WalletViewModel,
    id: Int,
    title: String,
    cardType: String,
    ownerName: String,
    cardNumber: String,
    secondNumber: String,
    expiryDate: String,
    shebaNumber: String,
    accountNumber: String,
    frontImageBase64: String?,
    backImageBase64: String?,
    additionalImagesJson: String? = null,
    extraFieldsJson: String? = null
) {
    val tempCard = WalletCard(
        id = id,
        title = title,
        cardType = cardType,
        ownerName = ownerName,
        cardNumber = cardNumber,
        secondNumber = secondNumber,
        expiryDate = expiryDate,
        shebaNumber = shebaNumber,
        accountNumber = accountNumber,
        frontImageBase64 = frontImageBase64,
        backImageBase64 = backImageBase64,
        additionalImagesJson = additionalImagesJson,
        extraFieldsJson = extraFieldsJson,
        timestamp = System.currentTimeMillis()
    )
    viewModel.selectCard(tempCard)
    viewModel.navigateTo(AppScreen.LIVE_SCANNER)
}
