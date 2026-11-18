package com.example.expensetracker

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

object Categories {
    // Keys stored in Expense.category. These are data, not labels.
    const val FOOD = "food"
    const val TRANSPORT = "transport"
    const val SHOPPING = "shopping"
    const val BILLS = "bills"
    const val ENTERTAINMENT = "entertainment"
    const val OTHER = "other"

    val all = listOf(FOOD, TRANSPORT, SHOPPING, BILLS, ENTERTAINMENT, OTHER)
}

// Turns a category key into its label in strings.xml, so it can be translated to Khmer.
@StringRes
fun categoryLabelRes(category: String): Int = when (category) {
    Categories.FOOD -> R.string.category_food
    Categories.TRANSPORT -> R.string.category_transport
    Categories.SHOPPING -> R.string.category_shopping
    Categories.BILLS -> R.string.category_bills
    Categories.ENTERTAINMENT -> R.string.category_entertainment
    else -> R.string.category_other
}

// NEW (Lab 5): short names used by the Form picker (D7) and the Dashboard filter (D9).
val categoryKeys: List<String> get() = Categories.all

// NEW (Lab 5): the translated label for a key, e.g. "food" -> "Food" (or the Khmer text).
@Composable
fun categoryLabel(category: String): String = stringResource(categoryLabelRes(category))