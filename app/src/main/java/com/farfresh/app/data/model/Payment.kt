package com.farfresh.app.data.model

data class Payment(
    val id: String = "",
    val saleId: String = "",
    val customerId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val amount: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    val note: String? = null
)
