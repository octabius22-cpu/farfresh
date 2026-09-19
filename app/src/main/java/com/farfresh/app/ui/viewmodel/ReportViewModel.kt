package com.farfresh.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farfresh.app.data.model.*
import com.farfresh.app.data.repository.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.*

enum class ReportPeriod {
    HOY, AYER, ESTA_SEMANA, ESTE_MES, MES_ANTERIOR, PERSONALIZADO
}

class ReportViewModel : ViewModel() {
    private val productRepository = ProductRepository()
    private val customerRepository = CustomerRepository()
    private val stockRepository = StockRepository()

    var selectedPeriod by mutableStateOf(ReportPeriod.HOY)
    var startDate by mutableStateOf(System.currentTimeMillis())
    var endDate by mutableStateOf(System.currentTimeMillis())

    private data class CombinedData(
        val sales: List<Sale>,
        val payments: List<Payment>,
        val saleItems: List<SaleItem>,
        val products: List<Product>,
        val customers: List<Customer>,
        val stockMovements: List<StockMovement>
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val reportState: StateFlow<ReportUiState> = combine(
        snapshotFlow { selectedPeriod },
        snapshotFlow { startDate },
        snapshotFlow { endDate }
    ) { period, start, end ->
        getRangeForPeriod(period, start, end)
    }.flatMapLatest { range ->
        combine(
            SaleRepository.getSalesByRange(range.first, range.second),
            SaleRepository.getPaymentsByRange(range.first, range.second),
            SaleRepository.getSaleItemsByRange(range.first, range.second),
            productRepository.getProducts(),
            customerRepository.getCustomers(),
            stockRepository.getMovements()
        ) { args: Array<Any> ->
            @Suppress("UNCHECKED_CAST")
            val data = CombinedData(
                sales = args[0] as List<Sale>,
                payments = args[1] as List<Payment>,
                saleItems = args[2] as List<SaleItem>,
                products = args[3] as List<Product>,
                customers = args[4] as List<Customer>,
                stockMovements = args[5] as List<StockMovement>
            )
            calculateReport(data, range.first, range.second)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportUiState())

    private fun calculateReport(data: CombinedData, start: Long, end: Long): ReportUiState {
        val salesInPeriod = data.sales
        val paymentsInPeriod = data.payments
        val stockMovementsInPeriod = data.stockMovements.filter { it.timestamp in start..end }
        
        val totalVentas = salesInPeriod.sumOf { it.totalAmount }
        val totalCobrado = paymentsInPeriod.sumOf { it.amount }
        val totalGanancia = salesInPeriod.sumOf { it.totalProfit }
        val porCobrarPeriodo = salesInPeriod.sumOf { it.pendingBalance }

        val efectivo = paymentsInPeriod.filter { it.paymentMethod == PaymentMethod.EFECTIVO }.sumOf { it.amount }
        val yape = paymentsInPeriod.filter { it.paymentMethod == PaymentMethod.YAPE }.sumOf { it.amount }
        val otros = paymentsInPeriod.filter { it.paymentMethod == PaymentMethod.OTRO }.sumOf { it.amount }

        val itemsInPeriod = data.saleItems.filter { item -> salesInPeriod.any { it.id == item.saleId } }
        val topSoldProducts = itemsInPeriod.groupBy { it.productId }
            .map { (id, items) ->
                val name = items.firstOrNull()?.productName ?: "Desconocido"
                ProductReportItem(
                    name = name,
                    quantity = items.sumOf { it.quantity },
                    amount = items.sumOf { it.subtotal },
                    profit = items.sumOf { it.profit }
                )
            }
            .sortedByDescending { it.quantity }

        val topProfitProducts = topSoldProducts.sortedByDescending { it.profit }

        val activeProducts = data.products.count { it.isActive }
        val lowStock = data.products.count { it.isActive && it.stock > 0 && it.stock <= 10 }
        val noStock = data.products.count { it.isActive && it.stock <= 0 }
        val totalUnits = data.products.sumOf { it.stock }

        val entries = stockMovementsInPeriod.count { it.type == StockMovementType.ENTRADA }
        val exits = stockMovementsInPeriod.count { it.type == StockMovementType.SALIDA }
        val adjustments = stockMovementsInPeriod.count { it.type == StockMovementType.AJUSTE }

        val totalCustomers = data.customers.size
        val debtorCount = data.customers.count { customer -> 
            data.sales.any { it.customerId == customer.id && it.pendingBalance > 0 }
        }
        val totalToCollectAll = data.sales.sumOf { it.pendingBalance }

        return ReportUiState(
            totalVentas = totalVentas,
            totalCobrado = totalCobrado,
            totalGanancia = totalGanancia,
            porCobrar = porCobrarPeriodo,
            cash = efectivo,
            yape = yape,
            other = otros,
            saleCount = salesInPeriod.size,
            unitsSold = itemsInPeriod.sumOf { it.quantity },
            topSold = topSoldProducts,
            topProfit = topProfitProducts,
            activeProducts = activeProducts,
            lowStockCount = lowStock,
            noStockCount = noStock,
            totalInventoryUnits = totalUnits,
            inventoryEntries = entries,
            inventoryExits = exits,
            inventoryAdjustments = adjustments,
            registeredCustomers = totalCustomers,
            debtorsCount = debtorCount,
            totalToCollectAll = totalToCollectAll,
            hasData = salesInPeriod.isNotEmpty() || paymentsInPeriod.isNotEmpty() || stockMovementsInPeriod.isNotEmpty()
        )
    }

    private fun getRangeForPeriod(period: ReportPeriod, customStart: Long, customEnd: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (period) {
            ReportPeriod.HOY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val s = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
                s to cal.timeInMillis
            }
            ReportPeriod.AYER -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val s = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
                s to cal.timeInMillis
            }
            ReportPeriod.ESTA_SEMANA -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                val s = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                s to cal.timeInMillis
            }
            ReportPeriod.ESTE_MES -> {
                cal.set(Calendar.DAY_OF_MONTH, 1); cal.set(Calendar.HOUR_OF_DAY, 0)
                val s = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                s to cal.timeInMillis
            }
            ReportPeriod.MES_ANTERIOR -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1); cal.set(Calendar.HOUR_OF_DAY, 0)
                val s = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                s to cal.timeInMillis
            }
            ReportPeriod.PERSONALIZADO -> {
                val s = Calendar.getInstance().apply { timeInMillis = customStart; set(Calendar.HOUR_OF_DAY, 0) }.timeInMillis
                val e = Calendar.getInstance().apply { timeInMillis = customEnd; set(Calendar.HOUR_OF_DAY, 23) }.timeInMillis
                s to e
            }
        }
    }
}

data class ReportUiState(
    val totalVentas: Double = 0.0,
    val totalCobrado: Double = 0.0,
    val totalGanancia: Double = 0.0,
    val porCobrar: Double = 0.0,
    val cash: Double = 0.0,
    val yape: Double = 0.0,
    val other: Double = 0.0,
    val saleCount: Int = 0,
    val unitsSold: Double = 0.0,
    val topSold: List<ProductReportItem> = emptyList(),
    val topProfit: List<ProductReportItem> = emptyList(),
    val activeProducts: Int = 0,
    val lowStockCount: Int = 0,
    val noStockCount: Int = 0,
    val totalInventoryUnits: Double = 0.0,
    val inventoryEntries: Int = 0,
    val inventoryExits: Int = 0,
    val inventoryAdjustments: Int = 0,
    val registeredCustomers: Int = 0,
    val debtorsCount: Int = 0,
    val totalToCollectAll: Double = 0.0,
    val hasData: Boolean = false
)

data class ProductReportItem(
    val name: String,
    val quantity: Double,
    val amount: Double,
    val profit: Double
)
