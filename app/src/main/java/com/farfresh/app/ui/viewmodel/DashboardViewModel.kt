package com.farfresh.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farfresh.app.data.model.*
import com.farfresh.app.data.repository.CustomerRepository
import com.farfresh.app.data.repository.SaleRepository
import kotlinx.coroutines.flow.*
import java.util.*

class DashboardViewModel : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        SaleRepository.getSales(),
        SaleRepository.getPayments()
    ) { sales, payments ->
        calculateDashboardState(sales, payments)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    private fun calculateDashboardState(
        sales: List<Sale>,
        payments: List<Payment>
    ): DashboardUiState {
        val today = Calendar.getInstance()
        
        val salesToday = sales.filter { isSameDay(it.timestamp, today) }
        val paymentsToday = payments.filter { isSameDay(it.timestamp, today) }

        val dailyVentas = salesToday.sumOf { it.totalAmount }
        val dailyProfit = salesToday.sumOf { it.totalProfit }
        
        val dailyCobrado = paymentsToday.sumOf { it.amount }
        val totalPendiente = sales.sumOf { it.pendingBalance }

        val cashToday = paymentsToday.filter { it.paymentMethod == PaymentMethod.EFECTIVO }.sumOf { it.amount }
        val yapeToday = paymentsToday.filter { it.paymentMethod == PaymentMethod.YAPE }.sumOf { it.amount }
        val otherToday = paymentsToday.filter { it.paymentMethod == PaymentMethod.OTRO }.sumOf { it.amount }

        val latestSales = sales.sortedByDescending { it.timestamp }.take(5).map { sale ->
            SaleDisplayItem(sale, sale.customerName ?: "Consumidor general")
        }

        val recentPending = sales.filter { it.pendingBalance > 0 }
            .sortedByDescending { it.timestamp }
            .take(3)
            .map { sale ->
                PendingDisplayItem(sale.id, sale.customerName ?: "Desconocido", sale.pendingBalance)
            }

        return DashboardUiState(
            dailyVentas = dailyVentas,
            dailyCobrado = dailyCobrado,
            totalPendiente = totalPendiente,
            dailyProfit = dailyProfit,
            cashToday = cashToday,
            yapeToday = yapeToday,
            otherToday = otherToday,
            latestSales = latestSales,
            recentPending = recentPending,
            hasSales = sales.isNotEmpty()
        )
    }

    private fun isSameDay(timestamp: Long, day: Calendar): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return cal.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
    }
}

data class DashboardUiState(
    val dailyVentas: Double = 0.0,
    val dailyCobrado: Double = 0.0,
    val totalPendiente: Double = 0.0,
    val dailyProfit: Double = 0.0,
    val cashToday: Double = 0.0,
    val yapeToday: Double = 0.0,
    val otherToday: Double = 0.0,
    val latestSales: List<SaleDisplayItem> = emptyList(),
    val recentPending: List<PendingDisplayItem> = emptyList(),
    val hasSales: Boolean = false
)

data class SaleDisplayItem(
    val sale: Sale,
    val customerName: String
)

data class PendingDisplayItem(
    val saleId: String,
    val customerName: String,
    val pendingAmount: Double
)
