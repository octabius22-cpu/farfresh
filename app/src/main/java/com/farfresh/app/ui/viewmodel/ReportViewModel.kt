package com.farfresh.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farfresh.app.data.model.*
import com.farfresh.app.data.repository.*
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

    val reportState: StateFlow<ReportUiState> = combine(
        SaleRepository.getSales(),
        SaleRepository.getPayments(),
        SaleRepository.getSaleItems(),
        productRepository.getProducts(),
        customerRepository.getCustomers(),
        stockRepository.getMovements(),
        snapshotFlow { selectedPeriod },
        snapshotFlow { startDate },
        snapshotFlow { endDate }
    ) { args: Array<Any> ->
        @Suppress("UNCHECKED_CAST")
        val sales = args[0] as List<Sale>
        @Suppress("UNCHECKED_CAST")
        val payments = args[1] as List<Payment>
        @Suppress("UNCHECKED_CAST")
        val saleItems = args[2] as List<SaleItem>
        @Suppress("UNCHECKED_CAST")
        val products = args[3] as List<Product>
        @Suppress("UNCHECKED_CAST")
        val customers = args[4] as List<Customer>
        @Suppress("UNCHECKED_CAST")
        val stockMovements = args[5] as List<StockMovement>
        val period = args[6] as ReportPeriod
        val startD = args[7] as Long
        val endD = args[8] as Long

        val data = CombinedData(sales, payments, saleItems, products, customers, stockMovements)
        calculateReport(data, period, startD, endD)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportUiState())

    private fun calculateReport(data: CombinedData, period: ReportPeriod, customStart: Long, customEnd: Long): ReportUiState {
        val (start, end) = getRangeForPeriod(period, customStart, customEnd)
        
        val salesInPeriod = data.sales.filter { it.timestamp in start..end }
        val paymentsInPeriod = data.payments.filter { it.timestamp in start..end }
        val stockMovementsInPeriod = data.stockMovements.filter { it.timestamp in start..end }
        
        // Resumen General
        val totalVentas = salesInPeriod.sumOf { it.totalAmount }
        val totalCobrado = paymentsInPeriod.sumOf { it.amount }
        val totalGanancia = salesInPeriod.sumOf { it.totalProfit }
        val porCobrarPeriodo = salesInPeriod.sumOf { it.pendingBalance }

        // Métodos de Pago
        val efectivo = paymentsInPeriod.filter { it.paymentMethod == PaymentMethod.EFECTIVO }.sumOf { it.amount }
        val yape = paymentsInPeriod.filter { it.paymentMethod == PaymentMethod.YAPE }.sumOf { it.amount }
        val otros = paymentsInPeriod.filter { it.paymentMethod == PaymentMethod.OTRO }.sumOf { it.amount }

        // Productos más vendidos
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

        // Inventario
        val activeProducts = data.products.count { it.isActive }
        val lowStock = data.products.count { it.isActive && it.stock > 0 && it.stock <= 10 }
        val noStock = data.products.count { it.isActive && it.stock <= 0 }
        val totalUnits = data.products.sumOf { it.stock }

        // Movimientos
        val entries = stockMovementsInPeriod.count { it.type == StockMovementType.ENTRADA }
        val exits = stockMovementsInPeriod.count { it.type == StockMovementType.SALIDA }
        val adjustments = stockMovementsInPeriod.count { it.type == StockMovementType.AJUSTE }

        // Clientes
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
        val now = cal.timeInMillis
        
        return when (period) {
            ReportPeriod.HOY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                start to cal.timeInMillis
            }
            ReportPeriod.AYER -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                start to cal.timeInMillis
            }
            ReportPeriod.ESTA_SEMANA -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                start to cal.timeInMillis
            }
            ReportPeriod.ESTE_MES -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                start to cal.timeInMillis
            }
            ReportPeriod.MES_ANTERIOR -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                start to cal.timeInMillis
            }
            ReportPeriod.PERSONALIZADO -> {
                val s = Calendar.getInstance().apply { 
                    timeInMillis = customStart
                    set(Calendar.HOUR_OF_DAY, 0)
                }.timeInMillis
                val e = Calendar.getInstance().apply {
                    timeInMillis = customEnd
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                }.timeInMillis
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
