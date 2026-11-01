package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String, // M3U, XTREAM, STALKER
    val sourceUrl: String,
    val username: String? = null,
    val password: String? = null,
    val macAddress: String? = null,
    val epgUrl: String? = null,
    val expirationMillis: Long = 0L,
    val isActive: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
