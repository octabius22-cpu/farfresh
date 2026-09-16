package com.farfresh.app.data.model

data class StockMovement(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val type: StockMovementType = StockMovementType.ENTRADA,
    val quantity: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String? = null,
    val stockBefore: Double = 0.0,
    val stockAfter: Double = 0.0,
    val referenceId: String? = null
)
