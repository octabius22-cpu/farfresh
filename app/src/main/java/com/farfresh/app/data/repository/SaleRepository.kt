package com.farfresh.app.data.repository

import com.farfresh.app.FarFreshApplication
import com.farfresh.app.data.model.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object SaleRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val salesCollection = firestore.collection("sales")
    private val saleItemsCollection = firestore.collection("saleItems")
    private val paymentsCollection = firestore.collection("payments")
    private val productsCollection = firestore.collection("products")
    
    private val db = FarFreshApplication.instance.database
    private val productDao = db.productDao()

    fun getSales(limit: Int = 100, startTime: Long? = null): Flow<List<Sale>> = callbackFlow {
        var query = salesCollection.orderBy("timestamp", Query.Direction.DESCENDING).limit(limit.toLong())
        if (startTime != null) query = query.whereGreaterThanOrEqualTo("timestamp", startTime)
        
        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) trySend(snapshot.toObjects(Sale::class.java))
        }
        awaitClose { subscription.remove() }
    }

    fun getSalesByRange(start: Long, end: Long): Flow<List<Sale>> = callbackFlow {
        val query = salesCollection
            .whereGreaterThanOrEqualTo("timestamp", start)
            .whereLessThanOrEqualTo("timestamp", end)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) trySend(snapshot.toObjects(Sale::class.java))
        }
        awaitClose { subscription.remove() }
    }

    fun getSaleItemsByRange(start: Long, end: Long): Flow<List<SaleItem>> = callbackFlow {
        val subscription = saleItemsCollection.limit(1000).addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) trySend(snapshot.toObjects(SaleItem::class.java))
        }
        awaitClose { subscription.remove() }
    }

    fun getPaymentsByRange(start: Long, end: Long): Flow<List<Payment>> = callbackFlow {
        val query = paymentsCollection
            .whereGreaterThanOrEqualTo("timestamp", start)
            .whereLessThanOrEqualTo("timestamp", end)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) trySend(snapshot.toObjects(Payment::class.java))
        }
        awaitClose { subscription.remove() }
    }

    fun getPendingSales(): Flow<List<Sale>> = callbackFlow {
        val query = salesCollection.whereGreaterThan("pendingBalance", 0.0)
        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) trySend(snapshot.toObjects(Sale::class.java))
        }
        awaitClose { subscription.remove() }
    }

    fun getSaleItems(limit: Int = 500): Flow<List<SaleItem>> = callbackFlow {
        val subscription = saleItemsCollection.limit(limit.toLong()).addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) trySend(snapshot.toObjects(SaleItem::class.java))
        }
        awaitClose { subscription.remove() }
    }

    fun getPayments(limit: Int = 200): Flow<List<Payment>> = callbackFlow {
        val subscription = paymentsCollection.orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) trySend(snapshot.toObjects(Payment::class.java))
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveSale(sale: Sale, items: List<SaleItem>, payment: Payment?) {
        val batch = firestore.batch()
        val saleRef = salesCollection.document()
        val saleId = saleRef.id
        val timestamp = System.currentTimeMillis()

        items.forEach { item ->
            val productRef = productsCollection.document(item.productId)
            
            // Incremento atómico negativo (Funciona offline)
            batch.update(productRef, "stock", FieldValue.increment(-item.quantity))

            // Movimiento de stock (Firestore lo subirá cuando haya señal)
            val movementRef = firestore.collection("stockMovements").document()
            batch.set(movementRef, StockMovement(
                id = movementRef.id,
                productId = item.productId,
                productName = item.productName,
                type = StockMovementType.SALIDA,
                quantity = item.quantity,
                timestamp = timestamp,
                reason = "Venta",
                referenceId = saleId
            ))

            // ACTUALIZAR ROOM (Inmediato para la App local)
            productDao.deductStock(item.productId, item.quantity)
        }

        batch.set(saleRef, sale.copy(id = saleId, timestamp = timestamp))
        items.forEach { item ->
            val itemRef = saleItemsCollection.document()
            batch.set(itemRef, item.copy(id = itemRef.id, saleId = saleId))
        }
        payment?.let { p ->
            val paymentRef = paymentsCollection.document()
            batch.set(paymentRef, p.copy(id = paymentRef.id, saleId = saleId, timestamp = timestamp))
        }

        batch.commit()
    }

    suspend fun registerPayment(payment: Payment) {
        firestore.runTransaction { transaction ->
            val saleRef = salesCollection.document(payment.saleId)
            val saleDoc = transaction.get(saleRef)
            val sale = saleDoc.toObject(Sale::class.java) ?: throw Exception("Venta no encontrada")
            val newPaidAmount = sale.paidAmount + payment.amount
            val newPendingBalance = sale.totalAmount - newPaidAmount
            val updatedSale = sale.copy(
                paidAmount = newPaidAmount,
                pendingBalance = if (newPendingBalance < 0) 0.0 else newPendingBalance,
                status = if (newPendingBalance <= 0.0) SaleStatus.PAGADA else SaleStatus.PARCIAL
            )
            val paymentRef = paymentsCollection.document()
            transaction.set(paymentRef, payment.copy(id = paymentRef.id))
            transaction.set(saleRef, updatedSale)
            null
        }.await()
    }

    suspend fun deleteSale(saleId: String) {
        val items = saleItemsCollection.whereEqualTo("saleId", saleId).get().await().toObjects(SaleItem::class.java)
        val payments = paymentsCollection.whereEqualTo("saleId", saleId).get().await().toObjects(Payment::class.java)
        val movements = firestore.collection("stockMovements").whereEqualTo("referenceId", saleId).get().await()

        firestore.runTransaction { transaction ->
            items.forEach { item ->
                val productRef = productsCollection.document(item.productId)
                val productDoc = transaction.get(productRef)
                val product = productDoc.toObject(Product::class.java)
                if (product != null) {
                    val newStock = product.stock + item.quantity
                    transaction.update(productRef, "stock", newStock)
                }
                transaction.delete(saleItemsCollection.document(item.id))
            }
            movements.documents.forEach { doc -> transaction.delete(doc.reference) }
            payments.forEach { payment -> transaction.delete(paymentsCollection.document(payment.id)) }
            transaction.delete(salesCollection.document(saleId))
            null
        }.await()
    }
}
