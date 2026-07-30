package com.viora.wallet.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletCardDao {
    @Query("SELECT * FROM wallet_cards ORDER BY timestamp DESC")
    fun getAllCards(): Flow<List<WalletCard>>

    @Query("SELECT * FROM wallet_cards ORDER BY timestamp DESC")
    fun getAllCardsSync(): List<WalletCard>

    @Query("SELECT * FROM wallet_cards WHERE id = :id LIMIT 1")
    fun getCardByIdSync(id: Int): WalletCard?

    @Query("SELECT * FROM wallet_cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Int): WalletCard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: WalletCard)

    @Update
    suspend fun updateCard(card: WalletCard)

    @Delete
    suspend fun deleteCard(card: WalletCard)

    @Query("DELETE FROM wallet_cards WHERE id = :id")
    suspend fun deleteCardById(id: Int)
}
