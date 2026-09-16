package com.farfresh.app.data.model

data class SaleItem(
    val id: String = "",
    val saleId: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantity: Double = 0.0,
    val sellingPriceAtSale: Double = 0.0,
    val costAtSale: Double = 0.0,
    val subtotal: Double = 0.0,
    val profit: Double = 0.0
)
