package com.farfresh.app.data.model

data class Product(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val unit: String = "unidad",
    val volumeMl: Int? = null,
    val sellingPrice: Double = 0.0,
    val cost: Double = 0.0,
    val stock: Double = 0.0,
    val imageUrl: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
