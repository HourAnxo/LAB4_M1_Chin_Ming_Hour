package com.example.expensetracker

import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate

private const val TAG = "ExpenseTracker"

// Number of values each Expense is flattened into by ExpenseListSaver.
private const val FIELDS_PER_EXPENSE = 6

// Lets rememberSaveable keep the list when the screen rotates (or the app is killed in the
// background). A Bundle can't hold an Expense or a LocalDate, so each expense is flattened
// into plain values (the date as an ISO string) and rebuilt on restore.
private val ExpenseListSaver = listSaver<SnapshotStateList<Expense>, Any?>(
    save = { list ->
        list.flatMap { listOf(it.id, it.amount, it.currency, it.category, it.date.toString(), it.remark) }
    },
    restore = { values ->
        values.chunked(FIELDS_PER_EXPENSE).map {
            Expense(
                id = it[0] as String,
                amount = it[1] as Double,
                currency = it[2] as String,
                category = it[3] as String,
                date = LocalDate.parse(it[4] as String),
                remark = it[5] as String?
            )
        }.toMutableStateList()
    }
)

// Remembers where a deleted expense was, so Undo can put it back in the same place.
private data class DeletedExpense(val index: Int, val expense: Expense)

// Stateful "caller". It owns the state and passes it down to the stateless DashboardScreen.
@Composable
fun ExpenseApp() {
    // rememberSaveable (not remember) so the list and the sample counter survive rotation.
    val expenses = rememberSaveable(saver = ExpenseListSaver) { mutableStateListOf<Expense>() }
    var sampleCount by rememberSaveable { mutableIntStateOf(0) }
    var lastDeleted by remember { mutableStateOf<DeletedExpense?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Runs every time lastDeleted changes. Shows the Undo message.
    LaunchedEffect(lastDeleted) {
        val deleted = lastDeleted ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = context.getString(R.string.expense_deleted),
            actionLabel = context.getString(R.string.undo),
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            expenses.add(deleted.index.coerceIn(0, expenses.size), deleted.expense)
            Log.d(TAG, "Undo: restored expense ${deleted.expense.id}")
        }
        lastDeleted = null
    }

    DashboardScreen(
        expenses = expenses,
        snackbarHostState = snackbarHostState,
        onAddExpense = {
            val expense = createSampleExpense(sampleCount)
            sampleCount++
            expenses.add(0, expense) // newest first
            Log.d(TAG, "Added expense ${expense.id}")
        },
        onDeleteExpense = { expense ->
            val index = expenses.indexOf(expense)
            if (index >= 0) {
                expenses.removeAt(index)
                lastDeleted = DeletedExpense(index, expense)
                Log.d(TAG, "Deleted expense ${expense.id}")
            }
        }
    )
}
