package com.farfresh.app.data.local.dao

import androidx.room.*
import com.farfresh.app.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActive(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE dni = :dni LIMIT 1")
    suspend fun getByDni(dni: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR id LIKE :query || '%' OR dni LIKE :query || '%'")
    fun search(query: String): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity)

    @Query("DELETE FROM customers")
    suspend fun deleteAll()
}
