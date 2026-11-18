package com.example.expensetracker

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import androidx.compose.ui.res.stringResource

// DatePicker works with UTC milliseconds. Our Expense uses LocalDate.
// We use UTC for both directions, so the picked day never shifts by one (Cambodia is UTC+7).
private fun LocalDate.toPickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDateUtc(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormScreen(
    existing: Expense?,              // null = add, not null = edit
    onClose: () -> Unit,
    onSave: (Expense) -> Unit,
) {
    val form = rememberExpenseFormState(existing)

    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    // Same rule for the top-bar arrow and the phone's Back
    fun tryToClose() {
        if (form.hasChanges) {
            showDiscardDialog = true
        } else {
            onClose()
        }
    }

    // Part 3: catch the phone's Back ONLY when something changed.
    // With no changes it's disabled, so Navigation 3 closes the Form at once.
    BackHandler(enabled = form.hasChanges) { showDiscardDialog = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (existing == null) R.string.add_expense else R.string.edit_expense)) },
                navigationIcon = { BackArrowButton(onClick = { tryToClose() }) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()                         // keep fields above the keyboard
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AmountAndCurrencyRow(form)
            CategoryPicker(form)
            DateField(date = form.date, onClick = { showDatePicker = true })
            RemarkField(form)

            // D8: Save at the bottom, full width.
            // enabled = false -> Material 3 draws it gray automatically.
            Button(
                onClick = { onSave(form.toExpense()) },
                enabled = form.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }

    // Date: from a calendar, never typed. Starts at the form's date (today for new).
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = form.date.toPickerMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { form.date = it.toLocalDateUtc() }
                        showDatePicker = false
                    },
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            // showModeToggle = false hides the pencil icon -> the date can't be typed
            DatePicker(state = pickerState, showModeToggle = false)
        }
    }

    // Part 3: "Discard changes?"
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_changes_question)) },
            text = { Text(stringResource(R.string.changes_not_saved)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onClose()                     // close with nothing saved
                    },
                ) { Text(stringResource(R.string.discard), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text(stringResource(R.string.keep_editing)) }
            },
        )
    }
}

// The error text under a field.
// expandVertically makes its height grow smoothly -> the fields below move down smoothly.
// slideInVertically makes the text slide in.
@Composable
private fun ErrorText(visible: Boolean, message: String) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Top) +
                slideInVertically(initialOffsetY = { -it }) +
                fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        Text(message)
    }
}

// Amount + currency next to it
@Composable
private fun AmountAndCurrencyRow(form: ExpenseFormState) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        OutlinedTextField(
            value = form.amountText,
            onValueChange = form::onAmountChange,
            label = { Text(stringResource(R.string.label_amount)) },
            singleLine = true,
            // Number keyboard only
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
            isError = form.showAmountError,
            supportingText = {
                ErrorText(form.showAmountError, stringResource(ExpenseFormState.AMOUNT_ERROR))
            },
            modifier = Modifier.weight(1f),
        )

        val currencies = listOf("KHR", "USD")
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .padding(top = 8.dp)                  // line up with the text box
                .width(150.dp),
        ) {
            currencies.forEachIndexed { index, currency ->
                SegmentedButton(
                    selected = form.currency == currency,
                    onClick = { form.currency = currency },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = currencies.size),
                ) {
                    Text(currency)
                }
            }
        }
    }
}

// D7: a dropdown picker.
// Opening it and closing it without choosing counts as "touched" -> shows the error.
// ExposedDropdownMenuBox anchors the menu to the field (same width, opens right under it).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(form: ExpenseFormState) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun closeWithoutChoosing() {
        menuOpen = false
        form.onCategoryPickerDismissed()
    }

    ExposedDropdownMenuBox(
        expanded = menuOpen,
        onExpandedChange = { open ->
            if (open) {
                // Hide the keyboard first, so the menu has room to open below the field
                focusManager.clearFocus()
                menuOpen = true
            } else {
                closeWithoutChoosing()
            }
        },
    ) {
        OutlinedTextField(
            value = form.category?.let { categoryLabel(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.label_category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuOpen) },
            isError = form.showCategoryError,
            supportingText = {
                ErrorText(form.showCategoryError, stringResource(ExpenseFormState.CATEGORY_ERROR))
            },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { closeWithoutChoosing() },
        ) {
            categoryKeys.forEach { key ->
                DropdownMenuItem(
                    text = { Text(categoryLabel(key)) },
                    onClick = {
                        form.onCategoryPicked(key)
                        menuOpen = false
                    },
                )
            }
        }
    }
}

// Date: read-only field. A tap opens the calendar.
@Composable
private fun DateField(date: LocalDate, onClick: () -> Unit) {
    Box {
        OutlinedTextField(
            value = formatDate(date),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.label_date)) },
            trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.pick_date)) },
            supportingText = {},                      // same spacing as the other fields
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            Modifier
                .matchParentSize()
                .clickable(onClick = onClick),
        )
    }
}

// Remark: optional, max 60 characters, with a counter
@Composable
private fun RemarkField(form: ExpenseFormState) {
    OutlinedTextField(
        value = form.remark,
        onValueChange = form::onRemarkChange,
        label = { Text(stringResource(R.string.label_remark_optional)) },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        isError = form.showRemarkError,
        supportingText = {
            Column {
                ErrorText(form.showRemarkError, stringResource(ExpenseFormState.REMARK_ERROR, MAX_REMARK_LENGTH))
                Text(
                    "${form.remark.length} / $MAX_REMARK_LENGTH",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}