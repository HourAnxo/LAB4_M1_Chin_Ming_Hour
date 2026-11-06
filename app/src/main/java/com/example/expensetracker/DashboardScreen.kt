package com.example.expensetracker

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Stateless: it only shows the list it is given and calls the events it is given.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    expenses: List<Expense>,
    snackbarHostState: SnackbarHostState,
    onAddExpense: () -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    // Plain vals: worked out from the list on every change. No second state to keep in sync.
    val thisMonth = YearMonth.now()
    val monthExpenses = expenses.filter { YearMonth.from(it.date) == thisMonth }
    val monthTotal = monthExpenses.sumOf { it.amount }
    val categoryTotals = Categories.all.associateWith { category ->
        monthExpenses.filter { it.category == category }.sumOf { it.amount }
    }

    // derivedStateOf: only recalculates the boolean, and only changes when empty <-> not empty.
    val isEmpty by remember(expenses) { derivedStateOf { expenses.isEmpty() } }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_expense))
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = dimensionResource(R.dimen.screen_padding),
                top = dimensionResource(R.dimen.screen_padding),
                end = dimensionResource(R.dimen.screen_padding),
                bottom = dimensionResource(R.dimen.fab_clearance)
            ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
        ) {
            // Month total
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(dimensionResource(R.dimen.card_padding))) {
                        Text(
                            text = stringResource(R.string.this_month_total),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.money_format, BASE_CURRENCY, monthTotal),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Spending by category
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(dimensionResource(R.dimen.card_padding)),
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
                    ) {
                        Text(
                            text = stringResource(R.string.spending_by_category),
                            style = MaterialTheme.typography.titleMedium
                        )
                        categoryTotals.forEach { (category, total) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(categoryLabelRes(category)),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(R.string.money_format, BASE_CURRENCY, total),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }

            // Recent expenses heading
            item {
                Text(
                    text = stringResource(R.string.recent_expenses),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Empty message OR the list
            if (isEmpty) {
                item {
                    Text(
                        text = stringResource(R.string.no_expenses_yet),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimensionResource(R.dimen.spacing_large))
                    )
                }
            } else {
                items(expenses, key = { it.id }) { expense ->
                    ExpenseRow(
                        expense = expense,
                        onDelete = { onDeleteExpense(expense) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = dimensionResource(R.dimen.card_padding)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
            ) {
                Text(
                    text = stringResource(R.string.money_format, expense.currency, expense.amount),
                    style = MaterialTheme.typography.titleMedium
                )
                // Remark is optional user text, so it is shown only when there is one.
                expense.remark?.takeIf { it.isNotBlank() }?.let { remark ->
                    Text(
                        text = remark,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = stringResource(
                        R.string.expense_row_summary,
                        stringResource(categoryLabelRes(expense.category)),
                        // Formatted for the phone's language (e.g. "Nov 7, 2026"), not ISO.
                        expense.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_expense),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// Holds its own list (like ExpenseApp does) so + and delete really work in the previews.
// Android Studio previews are still pictures until you click "Start Interactive Mode".
@Composable
private fun InteractiveDashboardPreview(initial: List<Expense>) {
    val expenses = remember { initial.toMutableStateList() }
    var sampleCount by remember { mutableIntStateOf(initial.size) }
    DashboardScreen(
        expenses = expenses,
        snackbarHostState = remember { SnackbarHostState() },
        onAddExpense = { expenses.add(0, createSampleExpense(sampleCount++)) },
        onDeleteExpense = { expenses.remove(it) }
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun DashboardPreviewLight() {
    ExpenseTrackerTheme(darkTheme = false) {
        InteractiveDashboardPreview(listOf(createSampleExpense(0), createSampleExpense(2)))
    }
}

@Preview(name = "Dark, empty", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardPreviewDarkEmpty() {
    ExpenseTrackerTheme(darkTheme = true) {
        InteractiveDashboardPreview(emptyList())
    }
}

// Same screen with all text from values-km/strings.xml.
@Preview(name = "Khmer", showBackground = true, locale = "km")
@Composable
private fun DashboardPreviewKhmer() {
    ExpenseTrackerTheme(darkTheme = false) {
        InteractiveDashboardPreview(listOf(createSampleExpense(0), createSampleExpense(3)))
    }
}

// The whole real app (ExpenseApp), including the Undo snackbar.
@Preview(name = "Full app with Undo", showBackground = true)
@Composable
private fun ExpenseAppInteractivePreview() {
    ExpenseTrackerTheme {
        ExpenseApp()
    }
}
