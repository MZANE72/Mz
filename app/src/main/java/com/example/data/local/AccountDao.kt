package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY lastUpdated DESC")
    fun getAllAccountsFlow(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveAccount(): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccount(id: String)

    @Transaction
    suspend fun switchActiveAccount(newAccountId: String) {
        clearActive()
        setActive(newAccountId)
    }

    @Query("UPDATE accounts SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE accounts SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: String)
}
