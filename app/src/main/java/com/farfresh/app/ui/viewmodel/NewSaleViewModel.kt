package com.farfresh.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farfresh.app.data.model.*
import com.farfresh.app.data.repository.CustomerRepository
import com.farfresh.app.data.repository.ProductRepository
import com.farfresh.app.data.repository.SaleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class NewSaleViewModel : ViewModel() {
    
    private val productRepository = ProductRepository()
    private val customerRepository = CustomerRepository()

    // Estado de la UI
    var selectedCustomer by mutableStateOf<Customer?>(null)
    val cart = mutableStateListOf<CartItem>()
    var paymentMethod by mutableStateOf(PaymentMethod.EFECTIVO)
    var receivedAmount by mutableStateOf("")
    var isPartialPayment by mutableStateOf(false)
    var saleMessage by mutableStateOf<String?>(null)
    var customerIdInput by mutableStateOf("")

    val products = productRepository.getProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Eliminamos la carga de todos los clientes
    // val customers = customerRepository.getCustomers()...

    fun searchCustomerById(id: String) {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return
        
        viewModelScope.launch {
            try {
                val found = customerRepository.getCustomerById(cleanId)
                if (found != null) {
                    selectedCustomer = found
                    saleMessage = "Cliente encontrado: ${found.name}"
                } else {
                    selectedCustomer = null
                    saleMessage = "Código $cleanId no registrado"
                }
            } catch (e: Exception) {
                saleMessage = "Error: Verifica tu conexión"
            }
        }
    }

    fun updateCustomerPhone(phone: String) {
        selectedCustomer?.let { customer ->
            viewModelScope.launch {
                try {
                    val updated = customer.copy(phone = phone)
                    customerRepository.updateCustomer(updated)
                    selectedCustomer = updated
                    saleMessage = "Teléfono actualizado"
                } catch (e: Exception) {
                    saleMessage = "Error al actualizar teléfono"
                }
            }
        }
    }

    val total: Double
        get() = cart.sumOf { it.product.sellingPrice * it.quantity }

    val totalProfit: Double
        get() = cart.sumOf { (it.product.sellingPrice - it.product.cost) * it.quantity }

    val change: Double
        get() {
            val received = receivedAmount.toDoubleOrNull() ?: 0.0
            return if (received > total) received - total else 0.0
        }

    val paidAmount: Double
        get() = when (paymentMethod) {
            PaymentMethod.EFECTIVO -> {
                val received = receivedAmount.toDoubleOrNull() ?: 0.0
                if (received > total) total else received
            }
            PaymentMethod.YAPE -> receivedAmount.toDoubleOrNull() ?: total
            PaymentMethod.OTRO -> 0.0
        }

    val pendingBalance: Double
        get() = total - paidAmount

    fun addProduct(product: Product, quantity: Double) {
        val existing = cart.find { it.product.id == product.id }
        if (existing != null) {
            if (existing.quantity + quantity <= product.stock) {
                val index = cart.indexOf(existing)
                cart[index] = existing.copy(quantity = existing.quantity + quantity)
            } else {
                saleMessage = "Stock insuficiente para ${product.name}"
            }
        } else {
            if (quantity <= product.stock) {
                cart.add(CartItem(product, quantity))
            } else {
                saleMessage = "Stock insuficiente para ${product.name}"
            }
        }
    }

    fun updateQuantity(item: CartItem, delta: Double) {
        val index = cart.indexOf(item)
        if (index != -1) {
            val newQty = item.quantity + delta
            if (newQty <= 0) {
                cart.removeAt(index)
            } else if (newQty <= item.product.stock) {
                cart[index] = item.copy(quantity = newQty)
            } else {
                saleMessage = "Stock insuficiente"
            }
        }
    }

    fun registerSale(onSuccess: () -> Unit) {
        if (cart.isEmpty()) {
            saleMessage = "Agrega al menos un producto"
            return
        }

        val finalPaid = if (paymentMethod == PaymentMethod.OTRO) 0.0 else paidAmount
        val finalPending = total - finalPaid

        if (finalPending > 0 && selectedCustomer == null) {
            saleMessage = "Identifica al cliente para ventas con saldo pendiente"
            return
        }

        val status = when {
            finalPaid >= total -> SaleStatus.PAGADA
            finalPaid > 0 -> SaleStatus.PARCIAL
            else -> SaleStatus.PENDIENTE
        }

        val sale = Sale(
            customerId = selectedCustomer?.id,
            customerName = selectedCustomer?.name,
            totalAmount = total,
            paidAmount = finalPaid,
            pendingBalance = finalPending,
            totalProfit = totalProfit,
            status = status,
            timestamp = System.currentTimeMillis()
        )

        val items = cart.map { item ->
            SaleItem(
                productId = item.product.id,
                productName = item.product.name,
                quantity = item.quantity,
                sellingPriceAtSale = item.product.sellingPrice,
                costAtSale = item.product.cost,
                subtotal = item.product.sellingPrice * item.quantity,
                profit = (item.product.sellingPrice - item.product.cost) * item.quantity
            )
        }

        val payment = if (finalPaid > 0) {
            Payment(
                customerId = selectedCustomer?.id,
                amount = finalPaid,
                paymentMethod = paymentMethod,
                timestamp = System.currentTimeMillis()
            )
        } else null

        viewModelScope.launch {
            try {
                SaleRepository.saveSale(sale, items, payment)
                clearForm()
                saleMessage = "Venta registrada con éxito"
                onSuccess()
            } catch (e: Exception) {
                saleMessage = "Error al registrar venta: ${e.message}"
            }
        }
    }

    private fun clearForm() {
        cart.clear()
        selectedCustomer = null
        receivedAmount = ""
        paymentMethod = PaymentMethod.EFECTIVO
        isPartialPayment = false
    }
}

data class CartItem(
    val product: Product,
    val quantity: Double
)
