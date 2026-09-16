package com.farfresh.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farfresh.app.data.model.StockMovement
import com.farfresh.app.data.model.StockMovementType
import com.farfresh.app.data.repository.StockRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel : ViewModel() {
    private val stockRepository = StockRepository()

    var inventoryMessage by mutableStateOf<String?>(null)
    var isProcessing by mutableStateOf(false)

    fun getMovements(productId: String? = null): StateFlow<List<StockMovement>> {
        return stockRepository.getMovements(productId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun registerMovement(
        productId: String,
        productName: String,
        amount: Double,
        type: StockMovementType,
        reason: String? = null
    ) {
        if (amount <= 0 && type != StockMovementType.AJUSTE) {
            inventoryMessage = "La cantidad debe ser mayor a cero"
            return
        }

        isProcessing = true
        viewModelScope.launch {
            try {
                stockRepository.registerManualMovement(productId, productName, amount, type, reason)
                inventoryMessage = "Movimiento registrado con éxito"
            } catch (e: Exception) {
                inventoryMessage = "Error: ${e.message}"
            } finally {
                isProcessing = false
            }
        }
    }
}
