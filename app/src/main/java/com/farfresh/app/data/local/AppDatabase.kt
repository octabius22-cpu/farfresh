package com.farfresh.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.farfresh.app.data.local.dao.CustomerDao
import com.farfresh.app.data.local.dao.ProductDao
import com.farfresh.app.data.local.entity.CustomerEntity
import com.farfresh.app.data.local.entity.ProductEntity

@Database(entities = [CustomerEntity::class, ProductEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "farfresh_database"
                )
                    .fallbackToDestructiveMigration() // Añadido para facilitar la actualización de la DB
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
