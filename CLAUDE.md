# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Single-module Android app (`:app`), an Expense Tracker built with Jetpack Compose and Material 3. This is a university lab assignment (Lab 4). Package and namespace: `com.example.expensetracker`.

Toolchain: AGP 8.7.3, Kotlin 2.0.21 (with the Compose compiler plugin), Compose BOM 2024.09.00, Java 17, compileSdk/targetSdk 35. minSdk is 26 because the code uses `java.time` (`LocalDate`, `YearMonth`) without desugaring. There is no version catalog: plugin versions are in the root `build.gradle.kts` and library versions are written inline in `app/build.gradle.kts`.

## Build / run

The Gradle wrapper is pinned to **Gradle 8.14** because AGP 8.7.3 does not support Gradle 9. Gradle 8.14 can't run on Java 25, which is the JDK bundled with Android Studio (the error is "Unsupported class file major version 69"), so `gradle/gradle-daemon-jvm.properties` pins the Gradle daemon to JDK 21. There are no test sources.

```
./gradlew :app:assembleDebug   # build debug APK
./gradlew :app:installDebug    # install on a connected device/emulator
./gradlew :app:lint            # Android lint
```

On Windows PowerShell use `.\gradlew.bat` instead of `./gradlew`.

UI is checked with the `@Preview` composables in `DashboardScreen.kt`: light, dark with an empty list, Khmer (`locale = "km"`), and "Full app with Undo" (wraps `ExpenseApp()`). The first three use `InteractiveDashboardPreview`, which keeps its own list, so add/delete work in every preview once Interactive Mode is started. Add, delete and undo events are logged under the logcat tag `ExpenseTracker`.

## Architecture

State hoisting with a single screen and no ViewModel, navigation, or persistence. There is no database: the list lives only in saved instance state, so it survives rotation and background process death but is gone once the user closes the app.

- `MainActivity` → `ExpenseTrackerTheme { ExpenseApp() }`.
- `ExpenseApp` is the **stateful** owner. It holds the `mutableStateListOf<Expense>` and a sample counter (both `rememberSaveable`; the list uses `ExpenseListSaver`, which flattens each `Expense` into 6 values, so **adding a field to `Expense` means updating the saver too**), plus the `SnackbarHostState` and `lastDeleted` (plain `remember`). Delete removes an item and records `DeletedExpense(index, expense)`. A `LaunchedEffect(lastDeleted)` shows the Undo snackbar and puts the item back at its original index (clamped to the list size).
- `DashboardScreen` is **stateless**. It gets the list and event lambdas as parameters. The month total and per-category totals count only expenses in the current `YearMonth`, while the list shows every expense. They are plain `val`s worked out from the list on every recomposition, deliberately not kept as separate state. `derivedStateOf` is used only for the empty/non-empty switch. List items are keyed by `Expense.id` (a UUID).
- "Add" does not open a form. It calls `createSampleExpense(n)` (`SampleData.kt`), which cycles through a fixed sample list dated today.

### Conventions that span files

- **Categories are keys, not labels.** `Expense.category` stores a key from `Categories` (`"food"`, `"transport"`, …). Show it via `stringResource(categoryLabelRes(key))`. To add a category, update `Categories` (constants plus `all`), `categoryLabelRes`, and **both** `values/strings.xml` and `values-km/strings.xml`. An unknown key quietly shows the "Other" label but is left out of every per-category row, because the breakdown loops over `Categories.all`. It still counts toward the month total.
- **Localization: English plus Khmer (`values-km`).** All user-visible text, including content descriptions and snackbar text, comes from string resources. Every new string must also be added to `values-km/strings.xml`. Money is formatted with `R.string.money_format` (`%1$s %2$.2f`). The snackbar strings are read with `context.getString(...)` inside the `LaunchedEffect`, because `stringResource` can't be called there. `Expense.remark` is free text the user would type, so it is not a resource (the sample remarks in `SampleData.kt` are intentionally plain English), and it is shown only when it is not blank.
- **Currency:** totals are labelled `BASE_CURRENCY` ("USD") and amounts are summed as they are, with no conversion. Each `Expense` has its own `currency` field ("KHR" or "USD"), but mixing currencies would make the totals wrong. Each row shows its own `expense.currency`, while the totals always show `BASE_CURRENCY`. For now the sample data always uses `BASE_CURRENCY`, so this only becomes a problem once real input is added.
- **Theming:** composables use only `MaterialTheme.colorScheme` / `typography`, never the raw colours in `ui/theme/Color.kt`. Those are referenced only by `Theme.kt`. On Android 12+ the theme uses dynamic (wallpaper) colour. Below that it falls back to the custom purple/teal light and dark schemes. The XML themes in `res/values*/themes.xml` set only the pre-Compose window background.
- **Spacing** comes from `res/values/dimens.xml` via `dimensionResource(...)`, not hard-coded `dp`.
