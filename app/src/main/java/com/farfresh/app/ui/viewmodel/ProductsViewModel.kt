package com.farfresh.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farfresh.app.data.model.Product
import com.farfresh.app.data.repository.ProductRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductsViewModel : ViewModel() {
    private val productRepository = ProductRepository()

    var isSyncing by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var productMessage by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            isSyncing = true
            productRepository.syncProducts()
            isSyncing = false
        }
    }

    val products = productRepository.getProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addProduct(product: Product) {
        viewModelScope.launch {
            try {
                productRepository.addProduct(product)
                productMessage = "Producto guardado"
            } catch (e: Exception) {
                productMessage = "Error: ${e.message}"
            }
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            try {
                productRepository.updateProduct(product)
                productMessage = "Producto actualizado"
            } catch (e: Exception) {
                productMessage = "Error: ${e.message}"
            }
        }
    }
}
