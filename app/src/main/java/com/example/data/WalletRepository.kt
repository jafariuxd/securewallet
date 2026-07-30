package com.example.data

import kotlinx.coroutines.flow.Flow

class WalletRepository(private val walletCardDao: WalletCardDao) {
    val allCards: Flow<List<WalletCard>> = walletCardDao.getAllCards()

    suspend fun getAllCardsList(): List<WalletCard> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            walletCardDao.getAllCardsSync()
        }
    }

    suspend fun getCardById(id: Int): WalletCard? {
        return walletCardDao.getCardById(id)
    }

    suspend fun insertCard(card: WalletCard) {
        walletCardDao.insertCard(card)
    }

    suspend fun updateCard(card: WalletCard) {
        walletCardDao.updateCard(card)
    }

    suspend fun deleteCard(card: WalletCard) {
        walletCardDao.deleteCard(card)
    }

    suspend fun deleteCardById(id: Int) {
        walletCardDao.deleteCardById(id)
    }
}
