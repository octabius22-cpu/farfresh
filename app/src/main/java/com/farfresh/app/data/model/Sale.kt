package com.farfresh.app.data.model

data class Sale(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val customerId: String? = null,
    val customerName: String? = null, // Denormalización para evitar joins pesados
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val pendingBalance: Double = 0.0,
    val totalProfit: Double = 0.0,
    val status: SaleStatus = SaleStatus.PENDIENTE,
    val createdAt: Long = System.currentTimeMillis()
)
