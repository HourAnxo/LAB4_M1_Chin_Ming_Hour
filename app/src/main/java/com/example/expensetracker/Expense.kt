package com.example.expensetracker

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

// The currency that totals are shown in. Amounts are added as they are,
// so keep every amount in this currency for now.
// (Lab 4 code still uses this. We'll remove it when we update DashboardScreen.kt.)
const val BASE_CURRENCY = "USD"

// NEW (Part 5): the rate, written ONCE. 1 USD = 4,000 KHR.
const val KHR_PER_USD = 4_000.0

// NEW (Part 3): the remark limit, written once.
const val MAX_REMARK_LENGTH = 60

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val currency: String,      // "KHR" or "USD"
    val category: String,      // one of the keys in Categories
    val date: LocalDate,
    val remark: String? = null
)

// NEW (Part 5): the total is in KHR. A USD expense counts at the fixed rate.
// Example: 5 USD -> 20,000 KHR
fun Expense.amountInKhr(): Double =
    if (currency == "USD") amount * KHR_PER_USD else amount

// NEW: "$5.00" for USD, "20,000 ៛" for KHR
fun formatMoney(amount: Double, currency: String): String =
    if (currency == "USD") String.format(Locale.US, "$%,.2f", amount)
    else String.format(Locale.US, "%,.0f ៛", amount)

// NEW: "Wed, 18 Nov 2026"
fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.US))