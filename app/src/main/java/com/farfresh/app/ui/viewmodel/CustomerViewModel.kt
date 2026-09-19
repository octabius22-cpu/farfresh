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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class CustomerViewModel : ViewModel() {

    private val customerRepository = CustomerRepository()
    
    init {
        // Sincronización incremental al iniciar
        viewModelScope.launch {
            customerRepository.syncCustomers()
        }
    }

    var searchQuery by mutableStateOf("")
    
    // Búsqueda en tiempo real sobre Room (0 lecturas Firestore)
    val customers = snapshotFlow { searchQuery }
        .debounce(300)
        .flatMapLatest { query ->
            if (query.length >= 3) {
                customerRepository.searchCustomers(query)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var foundCustomer by mutableStateOf<Customer?>(null)
    var isSearching by mutableStateOf(false)
    var customerMessage by mutableStateOf<String?>(null)

    fun searchCustomer(id: String) {
        if (id.isBlank()) return
        
        isSearching = true
        viewModelScope.launch {
            try {
                val customer = customerRepository.getCustomerById(id)
                foundCustomer = customer
                if (customer == null) {
                    customerMessage = "No se encontró ningún cliente con el código $id"
                }
            } catch (e: Exception) {
                customerMessage = "Error al buscar cliente"
            } finally {
                isSearching = false
            }
        }
    }

    fun addCustomer(name: String, phone: String?, dni: String = "") {
        if (name.isBlank()) {
            customerMessage = "El nombre es obligatorio"
            return
        }
        viewModelScope.launch {
            val customer = Customer(
                id = if (dni.isNotBlank()) dni else UUID.randomUUID().toString(),
                name = name,
                phone = phone,
                dni = dni,
                createdAt = System.currentTimeMillis()
            )
            customerRepository.addCustomer(customer)
            customerMessage = "Cliente guardado con éxito"
        }
    }

    fun updateCustomer(customer: Customer) {
        if (customer.name.isBlank()) {
            customerMessage = "El nombre es obligatorio"
            return
        }
        viewModelScope.launch {
            customerRepository.updateCustomer(customer)
            customerMessage = "Cliente actualizado con éxito"
        }
    }

    fun getCustomerSales(customerId: String): Flow<List<Sale>> {
        return SaleRepository.getSales().map { sales ->
            sales.filter { it.customerId == customerId }
                .sortedByDescending { it.timestamp }
        }
    }

    fun getCustomerPendingBalance(customerId: String): Flow<Double> {
        return getCustomerSales(customerId).map { sales ->
            sales.sumOf { it.pendingBalance }
        }
    }

    fun getItemsForMultipleSales(saleIds: List<String>): Flow<List<SaleItem>> {
        return SaleRepository.getSaleItems().map { items ->
            items.filter { it.saleId in saleIds }
        }
    }
}
