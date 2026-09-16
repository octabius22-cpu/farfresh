package com.farfresh.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farfresh.app.data.model.*
import com.farfresh.app.data.repository.CustomerRepository
import com.farfresh.app.data.repository.SaleRepository
import kotlinx.coroutines.flow.*
import java.util.*

enum class PeriodFilter {
    HOY, SEMANA, MES, TODAS
}

class SalesViewModel : ViewModel() {

    var selectedFilter by mutableStateOf(PeriodFilter.HOY)
    var searchQuery by mutableStateOf("")

    val salesState: StateFlow<SalesUiState> = combine(
        combine(
            SaleRepository.getSales(),
            SaleRepository.getPayments(),
            SaleRepository.getSaleItems()
        ) { s, p, i -> Triple(s, p, i) },
        snapshotFlow { selectedFilter },
        snapshotFlow { searchQuery }
    ) { data, filter, query ->
        val sales = data.first
        val payments = data.second
        val items = data.third

        val now = Calendar.getInstance()
        
        val salesInPeriod = sales.filter { sale ->
            when (filter) {
                PeriodFilter.HOY -> isSameDay(sale.timestamp, now)
                PeriodFilter.SEMANA -> isSameWeek(sale.timestamp, now)
                PeriodFilter.MES -> isSameMonth(sale.timestamp, now)
                PeriodFilter.TODAS -> true
            }
        }

        val paymentsInPeriod = payments.filter { payment ->
            when (filter) {
                PeriodFilter.HOY -> isSameDay(payment.timestamp, now)
                PeriodFilter.SEMANA -> isSameWeek(payment.timestamp, now)
                PeriodFilter.MES -> isSameMonth(payment.timestamp, now)
                PeriodFilter.TODAS -> true
            }
        }

        val filteredSales = salesInPeriod.filter { sale ->
            val customerName = sale.customerName ?: "Consumidor general"
            val saleItems = items.filter { it.saleId == sale.id }
            val matchCustomer = customerName.contains(query, ignoreCase = true)
            val matchProduct = saleItems.any { it.productName.contains(query, ignoreCase = true) }
            
            query.isEmpty() || matchCustomer || matchProduct
        }.sortedByDescending { it.timestamp }

        SalesUiState(
            sales = filteredSales.map { sale ->
                val customerName = sale.customerName ?: "Consumidor general"
                val count = items.count { it.saleId == sale.id }
                SaleListItem(sale, customerName, count)
            },
            totalSales = salesInPeriod.sumOf { it.totalAmount },
            totalCollected = paymentsInPeriod.sumOf { it.amount },
            totalPending = salesInPeriod.sumOf { it.pendingBalance }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SalesUiState()
    )

    private fun isSameDay(timestamp: Long, now: Calendar): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameWeek(timestamp: Long, now: Calendar): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                cal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isSameMonth(timestamp: Long, now: Calendar): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
    }
}

data class SalesUiState(
    val sales: List<SaleListItem> = emptyList(),
    val totalSales: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalPending: Double = 0.0
)

data class SaleListItem(
    val sale: Sale,
    val customerName: String,
    val itemCount: Int
)
