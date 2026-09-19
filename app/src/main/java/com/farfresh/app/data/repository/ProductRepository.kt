package com.farfresh.app.data.repository

import android.content.Context
import android.util.Log
import com.farfresh.app.FarFreshApplication
import com.farfresh.app.data.local.entity.ProductEntity
import com.farfresh.app.data.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ProductRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val productsCollection = firestore.collection("products")
    
    private val db = FarFreshApplication.instance.database
    private val productDao = db.productDao()
    private val prefs = FarFreshApplication.instance.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    /**
     * Retorna productos desde Room (0 lecturas Firestore).
     */
    fun getProducts(): Flow<List<Product>> {
        return productDao.getAllActive().map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * Sincronización Incremental de Productos mejorada.
     */
    suspend fun syncProducts() {
        try {
            val lastSync = prefs.getLong("last_product_sync", 0L)
            
            // Si es la primera vez, traemos TODO. Si no, solo los cambios.
            val query = if (lastSync == 0L) {
                productsCollection // Sin filtros para asegurar que traiga productos antiguos
            } else {
                productsCollection.whereGreaterThan("updatedAt", lastSync)
            }

            val snapshot = query.get().await()
            if (!snapshot.isEmpty) {
                val remoteProducts = snapshot.toObjects(Product::class.java)
                
                // Mapear asegurando que los IDs y timestamps existan
                val entities = remoteProducts.mapIndexed { index, product ->
                    val docId = snapshot.documents[index].id
                    product.copy(
                        id = docId,
                        updatedAt = if (product.updatedAt == 0L) System.currentTimeMillis() else product.updatedAt
                    ).toEntity()
                }
                
                productDao.insertAll(entities)
                
                // Actualizar el puntero de sincronización
                val maxUpdatedAt = entities.maxOfOrNull { it.updatedAt } ?: System.currentTimeMillis()
                prefs.edit().putLong("last_product_sync", maxUpdatedAt).apply()
                Log.d("ProductRepository", "Sincronizados ${entities.size} productos")
            }
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error syncing products: ${e.message}", e)
        }
    }

    suspend fun addProduct(product: Product) {
        val docRef = productsCollection.document()
        val timestamp = System.currentTimeMillis()
        val newProduct = product.copy(id = docRef.id, updatedAt = timestamp)
        
        // 1. Firestore (se encola si no hay señal)
        docRef.set(newProduct)
        
        // 2. Room (inmediato para UI)
        productDao.insert(newProduct.toEntity())
    }

    suspend fun updateProduct(product: Product) {
        val timestamp = System.currentTimeMillis()
        val updatedProduct = product.copy(updatedAt = timestamp)
        
        // 1. Firestore
        productsCollection.document(product.id).set(updatedProduct)
        
        // 2. Room
        productDao.insert(updatedProduct.toEntity())
    }

    suspend fun getProductById(id: String): Product? {
        val local = productDao.getById(id)
        if (local != null) return local.toModel()

        return try {
            val doc = productsCollection.document(id).get().await()
            val remote = doc.toObject(Product::class.java)
            remote?.let { productDao.insert(it.toEntity()) }
            remote
        } catch (e: Exception) {
            null
        }
    }

    // Mappers
    private fun ProductEntity.toModel() = Product(
        id = id, name = name, category = category, unit = unit,
        volumeMl = volumeMl, sellingPrice = sellingPrice, cost = cost,
        stock = stock, imageUrl = imageUrl, isActive = isActive, updatedAt = updatedAt
    )

    private fun Product.toEntity() = ProductEntity(
        id = id, name = name, category = category, unit = unit,
        volumeMl = volumeMl, sellingPrice = sellingPrice, cost = cost,
        stock = stock, imageUrl = imageUrl, isActive = isActive, updatedAt = updatedAt
    )
}
