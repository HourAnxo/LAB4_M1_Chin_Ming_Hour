package com.example.expensetracker

import java.time.LocalDate

private data class SampleItem(val remark: String?, val category: String, val amount: Double)

// My sample expenses from Lab 3 (SpendWise).
private val sampleItems = listOf(
    SampleItem("Iced coffee", Categories.FOOD, 4.50),
    SampleItem("Lunch with friends", Categories.FOOD, 12.00),
    SampleItem(null, Categories.TRANSPORT, 25.00),
    SampleItem("New headphones", Categories.SHOPPING, 60.00),
    SampleItem(null, Categories.FOOD, 8.75),
    SampleItem("Movie ticket", Categories.ENTERTAINMENT, 15.00),
    SampleItem("Grab rides", Categories.TRANSPORT, 30.00),
    SampleItem("Internet", Categories.BILLS, 45.00),
    SampleItem(null, Categories.SHOPPING, 22.50),
    SampleItem("Noodles", Categories.FOOD, 6.00)
)

// Each press of "+" uses the next sample, dated today.
fun createSampleExpense(index: Int): Expense {
    val item = sampleItems[index % sampleItems.size]
    return Expense(
        amount = item.amount,
        currency = BASE_CURRENCY,
        category = item.category,
        date = LocalDate.now(),
        remark = item.remark
    )
}
