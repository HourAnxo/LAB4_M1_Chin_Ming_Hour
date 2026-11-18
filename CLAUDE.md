# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Single-module Android app (`:app`), an Expense Tracker built with Jetpack Compose and Material 3. It is a university lab assignment: it started as Lab 4 (one screen) and is being extended for Lab 5 (Navigation 3, add/edit form, detail screen, category filter, KHR totals). Comments marked `NEW` / `CHANGED` / `Part N` / `Dn` refer to the Lab 5 steps. Package and namespace: `com.example.expensetracker`.

Toolchain: AGP 8.13.0, Kotlin 2.2.21 (Compose compiler and serialization plugins), Compose BOM 2025.12.00, Navigation 3 1.2.1, Java 17, compileSdk 36 (Navigation 3 needs it), targetSdk 35. minSdk is 26 because the code uses `java.time` without desugaring. There is no version catalog: plugin versions are in the root `build.gradle.kts` and library versions are written inline in `app/build.gradle.kts`. Kotlin 2.2 rejects `kotlinOptions`, so the JVM target is set in a top-level `kotlin { compilerOptions { } }` block.

## Build / run

The Gradle wrapper is pinned to **Gradle 8.14**. It can't run on Java 25 (the JDK bundled with Android Studio, which fails with "Unsupported class file major version 69"), so `gradle/gradle-daemon-jvm.properties` pins the daemon to JDK 21. There are no test sources.

```
./gradlew :app:assembleDebug   # build debug APK
./gradlew :app:installDebug    # install on a connected device/emulator
./gradlew :app:lint            # Android lint
```

On Windows PowerShell use `.\gradlew.bat`.

The `@Preview`s in `DashboardScreen.kt` (light, dark/empty, Khmer, and "Full app with Undo", which wraps `ExpenseApp()`) are how the UI gets checked. The first three use `InteractiveDashboardPreview`, which keeps its own list and adds `createSampleExpense(n)` items. That is the only remaining use of `SampleData.kt`. Save, delete and undo are logged under the logcat tag `ExpenseTracker`.

## Architecture

State hoisting, with no ViewModel and no persistence. The list lives only in saved instance state, so it survives rotation and background process death but is lost when the app is closed.

- `MainActivity` → `ExpenseTrackerTheme { ExpenseApp() }`.
- **`ExpenseApp` owns all app state and navigation.** It holds the `mutableStateListOf<Expense>` (`rememberSaveable` with `ExpenseListSaver`, which flattens each `Expense` into `FIELDS_PER_EXPENSE` = 6 values; **adding a field to `Expense` means updating this saver and `ExpenseFormState.Saver`/`FormValues`**), the `SnackbarHostState`, `lastDeleted`, and the Navigation 3 back stack (`rememberNavBackStack(DashboardKey)`). The local helpers `findExpense`, `saveExpense` (same id replaces, new id is added at the top), `deleteExpense`, and `popIfTop` are passed to the screens as lambdas.
- **Navigation 3 (`NavKeys.kt`)**: `DashboardKey`, `DetailKey(expenseId)`, and `FormKey(expenseId: String? = null)` (null means add, non-null means edit). The keys are `@Serializable` so that the back stack survives rotation. They carry only the **id**, never the `Expense`. The screen looks it up, and if it's missing it shows `NotFoundScreen` (in `DetailScreen.kt`). Screens close with `popIfTop(key)` rather than `removeLastOrNull()`, so a double tap can't pop two entries. Detail uses the `slideFromRight` transition metadata.
- **Delete / Undo**: `deleteExpense` records `DeletedExpense(index, expense)`, and `LaunchedEffect(lastDeleted)` shows the Undo snackbar and puts the item back at its old index (clamped). The single `SnackbarHostState` is shared by Dashboard and Detail, so the snackbar still shows after Detail pops itself on delete.
- **`DashboardScreen`** is stateless apart from the `selectedCategory` filter (`rememberSaveable`, null means "All"). It works out plain `val`s on each recomposition, not separate state. The headline total is over the **filtered** list across all dates. The per-category breakdown is **current-month only** and ignores the filter. `derivedStateOf` is used only for the empty/non-empty switch. List items are keyed by `Expense.id`.
- **Form**: `ExpenseFormState` is a `@Stable` state holder with its own `Saver`, created via `rememberExpenseFormState(existing)`. It owns the field values, validation rules (amount > 0, category chosen, remark ≤ `MAX_REMARK_LENGTH`), "touched" flags (an error shows only after its field has been touched), `canSave`, and `hasChanges` (compares `FormValues` with `initial`). `ExpenseFormScreen` only renders it. Back and the top-bar arrow show a "Discard changes?" dialog only when `hasChanges` is true, and `BackHandler` is enabled only in that case. The DatePicker works in UTC millis (`toPickerMillis` / `toLocalDateUtc`).

### Conventions that span files

- **Categories are keys, not labels.** `Expense.category` stores a key from `Categories` (`"food"`, …). Display it with `categoryLabel(key)` / `stringResource(categoryLabelRes(key))`. To add a category, update `Categories` (constants plus `all`), `categoryLabelRes`, and both `values/strings.xml` and `values-km/strings.xml`. The form picker, the filter chips and the breakdown all loop over `Categories.all`, so an unknown key shows as "Other" but is missing from every per-category view.
- **Currency**: each `Expense` is "KHR" or "USD". Totals are always in KHR via `Expense.amountInKhr()` (`KHR_PER_USD` = 4000, defined once in `Expense.kt`). Use `formatMoney(amount, currency)` for "$5.00" / "20,000 ៛". `BASE_CURRENCY` ("USD") remains only for `SampleData.kt`. The Dashboard still formats amounts with `R.string.money_format` (`%1$s %2$.2f`) while Detail uses `formatMoney`, so the two screens show amounts differently.
- **Localization: English plus Khmer (`values-km`).** All user-visible text, including content descriptions, comes from string resources, and every string must also be added to `values-km/strings.xml` (both files have the same set of names). Outside composition (snackbars in `LaunchedEffect` / `scope.launch`) use `context.getString(...)`. `ExpenseFormState` is not composable, so its validation errors are `@StringRes Int?` ids (`AMOUNT_ERROR` etc.) and `ExpenseFormScreen` turns them into text. `Expense.remark` is user free text, so it is not a resource.
- **Theming**: composables use only `MaterialTheme.colorScheme` / `typography`, never `ui/theme/Color.kt` directly (only `Theme.kt` uses it). Dynamic colour is used on Android 12+, with a custom purple/teal fallback below that.
- **Spacing** comes from `res/values/dimens.xml` via `dimensionResource(...)`, not hard-coded `dp`.
