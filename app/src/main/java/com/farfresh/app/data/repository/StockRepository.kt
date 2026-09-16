package com.farfresh.app.data.repository

import android.util.Log
import com.farfresh.app.data.model.Product
import com.farfresh.app.data.model.StockMovement
import com.farfresh.app.data.model.StockMovementType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class StockRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val movementsCollection = firestore.collection("stockMovements")
    private val productsCollection = firestore.collection("products")

    fun getMovements(productId: String? = null): Flow<List<StockMovement>> = callbackFlow {
        var query: Query = movementsCollection.orderBy("timestamp", Query.Direction.DESCENDING)
        
        if (productId != null) {
            query = query.whereEqualTo("productId", productId)
        }

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("StockRepository", "Error fetching movements", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                try {
                    val movements = snapshot.toObjects(StockMovement::class.java)
                    trySend(movements)
                } catch (e: Exception) {
                    Log.e("StockRepository", "Error mapping movements", e)
                }
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun registerManualMovement(
        productId: String,
        productName: String,
        amount: Double,
        type: StockMovementType,
        reason: String?
    ) {
        firestore.runTransaction { transaction ->
            val productRef = productsCollection.document(productId)
            val productDoc = transaction.get(productRef)
            val product = productDoc.toObject(Product::class.java)
                ?: throw Exception("Producto no encontrado")

            val stockBefore = product.stock
            val stockAfter = when (type) {
                StockMovementType.ENTRADA -> stockBefore + amount
                StockMovementType.SALIDA -> {
                    if (stockBefore < amount) throw Exception("Stock insuficiente. Disponible: $stockBefore")
                    stockBefore - amount
                }
                StockMovementType.AJUSTE -> amount // En ajuste, 'amount' es el nuevo stock físico
            }

            val adjustedQuantity = if (type == StockMovementType.AJUSTE) {
                stockAfter - stockBefore
            } else {
                amount
            }

            val movementRef = movementsCollection.document()
            val movement = StockMovement(
                id = movementRef.id,
                productId = productId,
                productName = productName,
                type = type,
                quantity = adjustedQuantity,
                timestamp = System.currentTimeMillis(),
                reason = reason,
                stockBefore = stockBefore,
                stockAfter = stockAfter
            )

            transaction.set(movementRef, movement)
            transaction.update(productRef, "stock", stockAfter)
            null
        }.await()
    }
}
