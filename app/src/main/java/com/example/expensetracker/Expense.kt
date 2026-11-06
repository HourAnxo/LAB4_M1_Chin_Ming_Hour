package com.example.expensetracker

import java.time.LocalDate
import java.util.UUID

// The currency that totals are shown in. Amounts are added as they are,
// so keep every amount in this currency for now.
const val BASE_CURRENCY = "USD"

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val currency: String,      // "KHR" or "USD"
    val category: String,      // one of the keys in Categories
    val date: LocalDate,
    val remark: String? = null
)
