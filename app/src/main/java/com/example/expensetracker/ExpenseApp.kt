package com.example.expensetracker

import android.util.Log
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import java.time.LocalDate
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource

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

// NEW (Part 1): Detail slides in from the right and slides out to the right
// (back arrow, phone Back, and the predictive back gesture).
private val slideFromRight = metadata {
    put(NavDisplay.TransitionKey) {
        slideInHorizontally(initialOffsetX = { it }) togetherWith ExitTransition.KeepUntilTransitionsFinished
    }
    put(NavDisplay.PopTransitionKey) {
        EnterTransition.None togetherWith slideOutHorizontally(targetOffsetX = { it })
    }
    put(NavDisplay.PredictivePopTransitionKey, { _: Int ->
        EnterTransition.None togetherWith slideOutHorizontally(targetOffsetX = { it })
    })
}

// Stateful "caller". It owns the state and passes it down to the screens.
@Composable
fun ExpenseApp() {
    // rememberSaveable (not remember) so the list survives rotation.
    val expenses = rememberSaveable(saver = ExpenseListSaver) { mutableStateListOf<Expense>() }
    var lastDeleted by remember { mutableStateOf<DeletedExpense?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()                       // NEW: for "Expense saved"

    // NEW (Part 1): the back stack. Saved in the Bundle -> rotation keeps the same screen.
    val backStack = rememberNavBackStack(DashboardKey)

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

    // ---------- NEW: small helpers used by the screens ----------

    fun findExpense(id: String): Expense? = expenses.find { it.id == id }

    // New id -> add at the top. Same id -> replace the old values (edit).
    fun saveExpense(expense: Expense) {
        val index = expenses.indexOfFirst { it.id == expense.id }
        if (index >= 0) expenses[index] = expense else expenses.add(0, expense)
        Log.d(TAG, "Saved expense ${expense.id}")
    }

    // Your Lab 4 delete logic, now shared by the Dashboard and the Detail screen.
    fun deleteExpense(expense: Expense) {
        val index = expenses.indexOf(expense)
        if (index >= 0) {
            expenses.removeAt(index)
            lastDeleted = DeletedExpense(index, expense)
            Log.d(TAG, "Deleted expense ${expense.id}")
        }
    }

    // Pop only if this screen is still on top, so a double tap can't pop two screens.
    fun popIfTop(key: NavKey) {
        if (backStack.lastOrNull() == key) backStack.removeLastOrNull()
    }

    // ---------- NEW: the 3 screens ----------

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        entryProvider = entryProvider {

            entry<DashboardKey> {
                DashboardScreen(
                    expenses = expenses,
                    snackbarHostState = snackbarHostState,
                    onAddExpense = { backStack.add(FormKey()) },              // CHANGED: opens an empty Form
                    onDeleteExpense = { deleteExpense(it) },
                    onExpenseClick = { backStack.add(DetailKey(it.id)) },     // NEW
                )
            }

            entry<DetailKey>(metadata = slideFromRight) { key ->
                DetailScreen(
                    expense = findExpense(key.expenseId),
                    snackbarHostState = snackbarHostState,
                    onBack = { popIfTop(key) },
                    onEdit = { backStack.add(FormKey(key.expenseId)) },
                    onDelete = {
                        findExpense(key.expenseId)?.let { deleteExpense(it) }
                        popIfTop(key)          // Detail leaves the stack -> Back never shows it again
                    },
                )
            }

            entry<FormKey> { key ->
                val existing = key.expenseId?.let { findExpense(it) }
                if (key.expenseId != null && existing == null) {
                    NotFoundScreen(stringResource(R.string.edit_expense), onBack = { popIfTop(key) })
                } else {
                    ExpenseFormScreen(
                        existing = existing,
                        onClose = { popIfTop(key) },
                        onSave = { expense ->
                            saveExpense(expense)
                            popIfTop(key)      // Form leaves the stack -> Back never shows it again
                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.expense_saved)) }
                        },
                    )
                }
            }
        },
    )
}