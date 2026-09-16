package com.farfresh.app.data.repository

import android.util.Log
import com.farfresh.app.data.model.Customer
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CustomerRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val customersCollection = firestore.collection("customers")

    fun getCustomers(): Flow<List<Customer>> = callbackFlow {
        val subscription = customersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("CustomerRepository", "Error fetching customers", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                try {
                    val customers = snapshot.toObjects(Customer::class.java)
                    trySend(customers)
                } catch (e: Exception) {
                    Log.e("CustomerRepository", "Error mapping customers", e)
                }
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun addCustomer(customer: Customer) {
        val docRef = customersCollection.document()
        val newCustomer = customer.copy(id = docRef.id)
        docRef.set(newCustomer).await()
    }

    suspend fun updateCustomer(customer: Customer) {
        customersCollection.document(customer.id).set(customer).await()
    }

    suspend fun getCustomerById(id: String): Customer? {
        return try {
            val document = customersCollection.document(id).get().await()
            if (document.exists()) {
                document.toObject(Customer::class.java)
            } else {
                Log.d("CustomerRepository", "No se encontró el documento con ID: $id")
                null
            }
        } catch (e: Exception) {
            Log.e("CustomerRepository", "Error al obtener cliente por ID: $id", e)
            null
        }
    }
}
