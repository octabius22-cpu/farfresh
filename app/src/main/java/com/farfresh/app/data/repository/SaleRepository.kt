package com.farfresh.app.data.repository

import android.util.Log
import com.farfresh.app.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object SaleRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val salesCollection = firestore.collection("sales")
    private val saleItemsCollection = firestore.collection("saleItems")
    private val paymentsCollection = firestore.collection("payments")
    private val productsCollection = firestore.collection("products")

    // Estos flujos se pueden exponer para el Dashboard y Ventas
    fun getSales(limit: Int? = null): Flow<List<Sale>> = callbackFlow {
        val query = if (limit != null) salesCollection.orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(limit.toLong())
                    else salesCollection.orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
        
        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("SaleRepository", "Error fetching sales", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                try {
                    val sales = snapshot.toObjects(Sale::class.java)
                    trySend(sales)
                } catch (e: Exception) {
                    Log.e("SaleRepository", "Error mapping sales", e)
                }
            }
        }
        awaitClose { subscription.remove() }
    }

    fun getSaleItems(): Flow<List<SaleItem>> = callbackFlow {
        val subscription = saleItemsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("SaleRepository", "Error fetching items", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                try {
                    val items = snapshot.toObjects(SaleItem::class.java)
                    trySend(items)
                } catch (e: Exception) {
                    Log.e("SaleRepository", "Error mapping items", e)
                }
            }
        }
        awaitClose { subscription.remove() }
    }

    fun getPayments(): Flow<List<Payment>> = callbackFlow {
        val subscription = paymentsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("SaleRepository", "Error fetching payments", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                try {
                    val payments = snapshot.toObjects(Payment::class.java)
                    trySend(payments)
                } catch (e: Exception) {
                    Log.e("SaleRepository", "Error mapping payments", e)
                }
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun saveSale(sale: Sale, items: List<SaleItem>, payment: Payment?) {
        firestore.runTransaction { transaction ->
            // 1. Preparar IDs y referencias
            val saleRef = salesCollection.document()
            val saleId = saleRef.id
            val timestamp = System.currentTimeMillis()

            // 2. Descontar stock y preparar movimientos para cada producto
            items.forEach { item ->
                val productRef = productsCollection.document(item.productId)
                val productDoc = transaction.get(productRef)
                val product = productDoc.toObject(Product::class.java)
                    ?: throw Exception("Producto ${item.productName} no encontrado")

                if (product.stock < item.quantity) {
                    throw Exception("No hay suficiente stock de ${product.name}. Disponible: ${product.stock}")
                }

                val stockBefore = product.stock
                val stockAfter = stockBefore - item.quantity

                // Actualizar producto
                transaction.update(productRef, "stock", stockAfter)

                // Crear movimiento de stock
                val movementRef = firestore.collection("stockMovements").document()
                val movement = StockMovement(
                    id = movementRef.id,
                    productId = item.productId,
                    productName = item.productName,
                    type = StockMovementType.SALIDA,
                    quantity = item.quantity,
                    timestamp = timestamp,
                    reason = "Venta",
                    stockBefore = stockBefore,
                    stockAfter = stockAfter,
                    referenceId = saleId
                )
                transaction.set(movementRef, movement)
            }

            // 3. Guardar Venta
            transaction.set(saleRef, sale.copy(id = saleId, timestamp = timestamp))

            // 4. Guardar Items de la venta
            items.forEach { item ->
                val itemRef = saleItemsCollection.document()
                transaction.set(itemRef, item.copy(id = itemRef.id, saleId = saleId))
            }

            // 5. Guardar Pago inicial si existe
            payment?.let { p ->
                val paymentRef = paymentsCollection.document()
                transaction.set(paymentRef, p.copy(id = paymentRef.id, saleId = saleId, timestamp = timestamp))
            }

            null // Éxito
        }.await()
    }

    suspend fun registerPayment(payment: Payment) {
        firestore.runTransaction { transaction ->
            val saleRef = salesCollection.document(payment.saleId)
            val saleDoc = transaction.get(saleRef)
            val sale = saleDoc.toObject(Sale::class.java)
                ?: throw Exception("Venta no encontrada")

            val newPaidAmount = sale.paidAmount + payment.amount
            val newPendingBalance = sale.totalAmount - newPaidAmount
            val newStatus = if (newPendingBalance <= 0.0) SaleStatus.PAGADA else SaleStatus.PARCIAL
            
            val updatedSale = sale.copy(
                paidAmount = newPaidAmount,
                pendingBalance = if (newPendingBalance < 0) 0.0 else newPendingBalance,
                status = newStatus
            )

            val paymentRef = paymentsCollection.document()
            transaction.set(paymentRef, payment.copy(id = paymentRef.id))
            transaction.set(saleRef, updatedSale)
            null // Return from lambda
        }.await()
    }

    // Métodos para compatibilidad con ViewModels actuales que esperan flows
    // Nota: Para no romper los ViewModels, expondremos estos flows como StateFlow si es posible,
    // pero Firestore snapshots ya son reactivos.
    // Los ViewModels deberán usar .collectAsStateWithLifecycle()
    
    // Para no romper la interfaz 'object SaleRepository', mantendremos las propiedades
    // pero ahora vendrán de Firestore. Sin embargo, StateFlow requiere un valor inicial.
    // Usaremos flows directamente en los ViewModels.
}
