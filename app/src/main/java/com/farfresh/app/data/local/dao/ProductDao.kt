package com.farfresh.app.data.local.dao

import androidx.room.*
import com.farfresh.app.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActive(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity)

    @Query("UPDATE products SET stock = stock - :quantity WHERE id = :productId")
    suspend fun deductStock(productId: String, quantity: Double)

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}
