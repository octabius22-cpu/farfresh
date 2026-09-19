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
import kotlinx.coroutines.launch
import java.util.UUID

class PendingViewModel : ViewModel() {

    var searchQuery by mutableStateOf("")
    var saleMessage by mutableStateOf<String?>(null)

    val pendingSales = combine(
        SaleRepository.getPendingSales(),
        snapshotFlow { searchQuery }
    ) { sales, query ->
        sales.map { sale ->
                PendingSaleItem(sale, sale.customerName ?: "Consumidor general")
            }
            .filter { item ->
                query.isEmpty() || item.customerName.contains(query, ignoreCase = true)
            }
            .sortedByDescending { it.sale.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPendingBalance = pendingSales.map { list ->
        list.sumOf { it.sale.pendingBalance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val debtorCount = pendingSales.map { list ->
        list.mapNotNull { it.sale.customerId }.distinct().size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingSaleCount = pendingSales.map { list ->
        list.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun registerPayment(saleId: String, amount: Double, method: PaymentMethod, note: String?) {
        viewModelScope.launch {
            try {
                val payment = Payment(
                    id = UUID.randomUUID().toString(),
                    saleId = saleId,
                    amount = amount,
                    paymentMethod = method,
                    note = note,
                    timestamp = System.currentTimeMillis()
                )
                SaleRepository.registerPayment(payment)
                saleMessage = "Pago registrado con éxito"
            } catch (e: Exception) {
                saleMessage = "Error: ${e.message}"
            }
        }
    }

    fun deleteSale(saleId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                SaleRepository.deleteSale(saleId)
                saleMessage = "Venta eliminada y stock restaurado"
                onSuccess()
            } catch (e: Exception) {
                saleMessage = "Error al eliminar: ${e.message}"
            }
        }
    }

    fun getItemsForSale(saleId: String): Flow<List<SaleItem>> {
        return SaleRepository.getSaleItems().map { items ->
            items.filter { it.saleId == saleId }
        }
    }

    fun getPaymentsForSale(saleId: String): Flow<List<Payment>> {
        return SaleRepository.getPayments().map { payments ->
            payments.filter { it.saleId == saleId }
        }
    }
}

data class PendingSaleItem(
    val sale: Sale,
    val customerName: String
)
