package com.example.expensetracker

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// Part 1: one NavKey per screen.
// @Serializable lets rememberNavBackStack save the back stack,
// so a rotation keeps the same screen open.

@Serializable
data object DashboardKey : NavKey

// Only the ID, never the whole Expense. The screen looks it up,
// so it always shows the latest values (or "Expense not found").
@Serializable
data class DetailKey(val expenseId: String) : NavKey

// expenseId == null -> add a new expense. Not null -> edit that expense.
@Serializable
data class FormKey(val expenseId: String? = null) : NavKey