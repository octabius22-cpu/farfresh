package com.farfresh.app.data.repository

import android.util.Log
import com.farfresh.app.data.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProductRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val productsCollection = firestore.collection("products")

    fun getProducts(): Flow<List<Product>> = callbackFlow {
        val subscription = productsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("ProductRepository", "Error fetching products", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                try {
                    val products = snapshot.toObjects(Product::class.java)
                    trySend(products)
                } catch (e: Exception) {
                    Log.e("ProductRepository", "Error mapping products", e)
                }
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun addProduct(product: Product) {
        val docRef = productsCollection.document()
        val newProduct = product.copy(id = docRef.id)
        docRef.set(newProduct).await()
    }

    suspend fun updateProduct(product: Product) {
        productsCollection.document(product.id).set(product).await()
    }

    suspend fun getProductById(id: String): Product? {
        return productsCollection.document(id).get().await().toObject(Product::class.java)
    }
}
