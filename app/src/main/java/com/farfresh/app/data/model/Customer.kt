package com.farfresh.app.data.model

import com.google.firebase.Timestamp

data class Customer(
    val id: String = "",
    val name: String = "",
    val phone: String? = null,
    val dni: String = "",
    val isActive: Boolean = true,
    val createdAt: Any? = null // Cambiado a Any para aceptar tanto Long como Timestamp
)
