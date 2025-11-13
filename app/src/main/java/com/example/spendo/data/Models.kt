package com.example.spendo.data

import com.google.firebase.Timestamp

data class AppUser(
    val uid: String = "",
    val name: String = "",
    val email: String = ""
)

enum class TransactionType { INCOME, EXPENSE }

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val amount: Long = 0,
    val currency: String = "LKR",
    val category: String = "",
    val description: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val date: Timestamp = Timestamp.now()
)