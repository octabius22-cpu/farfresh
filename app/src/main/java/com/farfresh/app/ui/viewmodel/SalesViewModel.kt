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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.*

enum class PeriodFilter {
    HOY, SEMANA, MES, TODAS
}

class SalesViewModel : ViewModel() {

    var selectedFilter by mutableStateOf(PeriodFilter.HOY)
    var searchQuery by mutableStateOf("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val salesState: StateFlow<SalesUiState> = snapshotFlow { selectedFilter }
        .flatMapLatest { filter ->
            val start = getStartTimeForFilter(filter)
            // Traemos solo lo necesario para el período
            combine(
                SaleRepository.getSales(limit = 200, startTime = start),
                SaleRepository.getPayments(limit = 300),
                SaleRepository.getSaleItems(limit = 1000),
                snapshotFlow { searchQuery }
            ) { sales, payments, items, query ->
                processSalesData(sales, payments, items, query, filter)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SalesUiState()
        )

    private fun getStartTimeForFilter(filter: PeriodFilter): Long? {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return when (filter) {
            PeriodFilter.HOY -> cal.timeInMillis
            PeriodFilter.SEMANA -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.timeInMillis
            }
            PeriodFilter.MES -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.timeInMillis
            }
            PeriodFilter.TODAS -> null
        }
    }

    private fun processSalesData(
        sales: List<Sale>,
        payments: List<Payment>,
        items: List<SaleItem>,
        query: String,
        filter: PeriodFilter
    ): SalesUiState {
        val now = Calendar.getInstance()
        
        // El filtro de Firestore ya trajo solo lo relevante para 'start',
        // pero para Semana/Mes afinamos localmente si es necesario.
        val salesInPeriod = if (filter == PeriodFilter.TODAS) sales 
        else sales.filter { sale ->
            when (filter) {
                PeriodFilter.HOY -> isSameDay(sale.timestamp, now)
                PeriodFilter.SEMANA -> isSameWeek(sale.timestamp, now)
                PeriodFilter.MES -> isSameMonth(sale.timestamp, now)
                else -> true
            }
        }

        val filteredSales = salesInPeriod.filter { sale ->
            val customerName = sale.customerName ?: "Consumidor general"
            val matchCustomer = customerName.contains(query, ignoreCase = true)
            query.isEmpty() || matchCustomer
        }.sortedByDescending { it.timestamp }

        return SalesUiState(
            sales = filteredSales.map { sale ->
                val customerName = sale.customerName ?: "Consumidor general"
                val count = items.count { it.saleId == sale.id }
                SaleListItem(sale, customerName, count)
            },
            totalSales = salesInPeriod.sumOf { it.totalAmount },
            totalCollected = payments.filter { p -> salesInPeriod.any { it.id == p.saleId } }.sumOf { it.amount },
            totalPending = salesInPeriod.sumOf { it.pendingBalance }
        )
    }

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
