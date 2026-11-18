package com.example.expensetracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.util.UUID
import androidx.annotation.StringRes

// The 5 values the user can change.
// A data class, so `==` compares every field (used for "did anything change?").
data class FormValues(
    val amountText: String,
    val currency: String,
    val category: String?,
    val date: LocalDate,
    val remark: String,
)

// Part 3: ONE state holder for the whole form.
// It keeps the values, the rules, and the errors. The screen only draws what it says.
@Stable
class ExpenseFormState(
    val editingId: String?,          // null = new expense, not null = editing
    val initial: FormValues,         // values when the form opened
    current: FormValues = initial,
) {
    // ----- Field values -----
    var amountText by mutableStateOf(current.amountText)
    var currency by mutableStateOf(current.currency)
    var category by mutableStateOf(current.category)
    var date by mutableStateOf(current.date)
    var remark by mutableStateOf(current.remark)

    // ----- Has the user touched this field yet? -----
    // No error when the form opens. An error shows only after its field is touched.
    var amountTouched by mutableStateOf(false)
    var categoryTouched by mutableStateOf(false)
    var remarkTouched by mutableStateOf(false)

    // ----- Events from the screen -----

    fun onAmountChange(text: String) {
        // Keep only digits and ONE dot, so pasted text like "-5" or "1,000" can't get in.
        var hasDot = false
        amountText = buildString {
            for (c in text) {
                when {
                    c.isDigit() -> append(c)
                    c == '.' && !hasDot -> { hasDot = true; append(c) }
                }
            }
        }
        amountTouched = true
    }

    fun onCategoryPicked(key: String) {
        category = key
        categoryTouched = true
    }

    // The user opened the picker and closed it without choosing.
    fun onCategoryPickerDismissed() {
        categoryTouched = true
    }

    fun onRemarkChange(text: String) {
        // We don't cut the text at 60. The user sees the error and fixes it.
        remark = text
        remarkTouched = true
    }

    // ----- Rules (the table in Part 3) -----
    // Getters are recalculated every time they're read, so they're always up to date.

    private val amountValue: Double? get() = amountText.toDoubleOrNull()

    @get:StringRes val amountError: Int?
        get() = if ((amountValue ?: 0.0) > 0.0) null else AMOUNT_ERROR

    @get:StringRes val categoryError: Int?
        get() = if (category != null) null else CATEGORY_ERROR

    @get:StringRes val remarkError: Int?
        get() = if (remark.length <= MAX_REMARK_LENGTH) null else REMARK_ERROR

    // Show an error only if the rule fails AND the user touched that field
    val showAmountError: Boolean get() = amountTouched && amountError != null
    val showCategoryError: Boolean get() = categoryTouched && categoryError != null
    val showRemarkError: Boolean get() = remarkTouched && remarkError != null

    // Save is enabled only when every field passes its rule
    val canSave: Boolean
        get() = amountError == null && categoryError == null && remarkError == null

    val currentValues: FormValues
        get() = FormValues(amountText, currency, category, date, remark)

    // Used by Back: ask "Discard changes?" only if something changed
    val hasChanges: Boolean get() = currentValues != initial

    // New expense -> new UUID. Edit -> keep the same id.
    fun toExpense(): Expense = Expense(
        id = editingId ?: UUID.randomUUID().toString(),
        amount = amountValue!!,
        currency = currency,
        category = category!!,
        date = date,
        remark = remark.trim().ifBlank { null },
    )

    companion object {
        // String resource ids. The screen turns them into text (English or Khmer).
        @StringRes val AMOUNT_ERROR = R.string.error_amount
        @StringRes val CATEGORY_ERROR = R.string.error_category
        @StringRes val REMARK_ERROR = R.string.error_remark   // takes MAX_REMARK_LENGTH

        // Add: empty form. Currency starts at KHR. Date starts at today.
        fun forNew() = ExpenseFormState(
            editingId = null,
            initial = FormValues(
                amountText = "",
                currency = "KHR",
                category = null,
                date = LocalDate.now(),
                remark = "",
            ),
        )

        // Edit: form filled in with the expense
        fun forEdit(expense: Expense) = ExpenseFormState(
            editingId = expense.id,
            initial = FormValues(
                // 5.0 -> "5", 2.5 -> "2.5"
                amountText = if (expense.amount % 1.0 == 0.0) expense.amount.toLong().toString()
                else expense.amount.toString(),
                currency = expense.currency,
                category = expense.category,
                date = expense.date,
                remark = expense.remark ?: "",
            ),
        )

        // Lets rememberSaveable keep what the user typed after a rotation.
        // Same idea as your ExpenseListSaver: flatten into simple values.
        val Saver = listSaver<ExpenseFormState, Any?>(
            save = { s ->
                listOf<Any?>(s.editingId) +          // index 0
                        s.initial.toSaveList() +         // index 1..5
                        s.currentValues.toSaveList() +   // index 6..10
                        listOf(s.amountTouched, s.categoryTouched, s.remarkTouched) // 11..13
            },
            restore = { list ->
                ExpenseFormState(
                    editingId = list[0] as String?,
                    initial = formValuesFrom(list, 1),
                    current = formValuesFrom(list, 6),
                ).apply {
                    amountTouched = list[11] as Boolean
                    categoryTouched = list[12] as Boolean
                    remarkTouched = list[13] as Boolean
                }
            },
        )
    }
}

// FormValues -> 5 simple values (LocalDate saved as a Long day number)
private fun FormValues.toSaveList(): List<Any?> =
    listOf(amountText, currency, category, date.toEpochDay(), remark)

// 5 simple values -> FormValues
private fun formValuesFrom(list: List<Any?>, start: Int) = FormValues(
    amountText = list[start] as String,
    currency = list[start + 1] as String,
    category = list[start + 2] as String?,
    date = LocalDate.ofEpochDay(list[start + 3] as Long),
    remark = list[start + 4] as String,
)

// The screen calls this. Creates the state once, and restores it after rotation.
@Composable
fun rememberExpenseFormState(existing: Expense?): ExpenseFormState =
    rememberSaveable(saver = ExpenseFormState.Saver) {
        if (existing == null) ExpenseFormState.forNew() else ExpenseFormState.forEdit(existing)
    }