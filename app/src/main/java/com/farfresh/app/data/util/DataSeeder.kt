package com.farfresh.app.data.util

import com.farfresh.app.data.model.Product
import com.farfresh.app.data.repository.ProductRepository
import kotlinx.coroutines.flow.first

object DataSeeder {
    suspend fun seedInitialProducts(repository: ProductRepository) {
        val currentProducts = repository.getProducts().first()
        
        if (currentProducts.isEmpty()) {
            val initialProducts = listOf(
                Product(
                    name = "Gaseosa Oro",
                    category = "Bebidas",
                    unit = "botella",
                    sellingPrice = 3.0,
                    cost = 2.0,
                    stock = 25.0
                ),
                Product(
                    name = "Sporade",
                    category = "Bebidas",
                    unit = "botella",
                    sellingPrice = 3.0,
                    cost = 2.0,
                    stock = 20.0
                ),
                Product(
                    name = "Cifrut",
                    category = "Bebidas",
                    unit = "botella",
                    sellingPrice = 2.5,
                    cost = 1.5,
                    stock = 30.0
                )
            )
            
            initialProducts.forEach { product ->
                repository.addProduct(product)
            }
        }
    }
}
