package com.farfresh.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["dni"]),
        Index(value = ["name"])
    ]
)
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String?,
    val dni: String,
    val isActive: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)
