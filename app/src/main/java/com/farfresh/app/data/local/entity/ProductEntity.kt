package com.farfresh.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val unit: String,
    val volumeMl: Int?,
    val sellingPrice: Double,
    val cost: Double,
    val stock: Double,
    val imageUrl: String?,
    val isActive: Boolean,
    val updatedAt: Long
)
