package com.farfresh.app.data.repository

import android.content.Context
import android.util.Log
import com.farfresh.app.FarFreshApplication
import com.farfresh.app.data.local.entity.CustomerEntity
import com.farfresh.app.data.model.Customer
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CustomerRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val customersCollection = firestore.collection("customers")
    
    private val db = FarFreshApplication.instance.database
    private val customerDao = db.customerDao()
    private val prefs = FarFreshApplication.instance.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    /**
     * Retorna el flujo de clientes directamente desde la base de datos local (Room).
     * 0 lecturas de Firestore al llamar esta función.
     */
    fun getCustomers(): Flow<List<Customer>> {
        return customerDao.getAllActive().map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * Búsqueda optimizada localmente en Room.
     */
    fun searchCustomers(query: String): Flow<List<Customer>> {
        return customerDao.search(query).map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * Sincronización Incremental: Solo descarga lo nuevo o modificado desde la última vez.
     */
    suspend fun syncCustomers() {
        try {
            val lastSync = prefs.getLong("last_customer_sync", 0L)
            
            // Si es 0, es la primera vez (descarga inicial)
            // Si no, solo traemos lo que tenga updatedAt > lastSync
            val query = if (lastSync == 0L) {
                customersCollection.whereEqualTo("isActive", true)
            } else {
                customersCollection.whereGreaterThan("updatedAt", lastSync)
            }

            val snapshot = query.get().await()
            if (!snapshot.isEmpty) {
                val remoteCustomers = snapshot.toObjects(Customer::class.java)
                val entities = remoteCustomers.map { it.toEntity() }
                
                customerDao.insertAll(entities)
                
                // Guardar el nuevo timestamp máximo encontrado
                val maxUpdatedAt = remoteCustomers.maxOf { it.updatedAt }
                prefs.edit().putLong("last_customer_sync", maxUpdatedAt).apply()
                
                Log.d("CustomerRepository", "Sincronizados ${remoteCustomers.size} clientes")
            }
        } catch (e: Exception) {
            Log.e("CustomerRepository", "Error en sincronización", e)
        }
    }

    suspend fun addCustomer(customer: Customer) {
        val docRef = customersCollection.document()
        val timestamp = System.currentTimeMillis()
        val newCustomer = customer.copy(id = docRef.id, updatedAt = timestamp)
        
        // 1. Guardar en Firestore
        docRef.set(newCustomer).await()
        
        // 2. Guardar en Room inmediatamente para funcionamiento offline
        customerDao.insert(newCustomer.toEntity())
    }

    suspend fun updateCustomer(customer: Customer) {
        val timestamp = System.currentTimeMillis()
        val updatedCustomer = customer.copy(updatedAt = timestamp)
        
        // 1. Actualizar Firestore
        customersCollection.document(customer.id).set(updatedCustomer).await()
        
        // 2. Actualizar Room
        customerDao.insert(updatedCustomer.toEntity())
    }

    suspend fun getCustomerById(id: String): Customer? {
        // Primero intentamos local (0 lecturas Firestore)
        val local = customerDao.getById(id)
        if (local != null) return local.toModel()

        // Si no está local, buscamos en la nube por si acaso
        return try {
            val document = customersCollection.document(id).get().await()
            val remote = document.toObject(Customer::class.java)
            remote?.let {
                customerDao.insert(it.toEntity()) // Guardamos local para la próxima
            }
            remote
        } catch (e: Exception) {
            null
        }
    }

    // Funciones de extensión para convertir entre modelos
    private fun CustomerEntity.toModel() = Customer(
        id = id,
        name = name,
        phone = phone,
        dni = dni,
        isActive = isActive,
        updatedAt = updatedAt
    )

    private fun Customer.toEntity() = CustomerEntity(
        id = id,
        name = name,
        phone = phone,
        dni = dni,
        isActive = isActive,
        updatedAt = updatedAt
    )
}
