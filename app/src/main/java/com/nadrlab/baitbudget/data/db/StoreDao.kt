package com.nadrlab.baitbudget.data.db

import androidx.room.*
import com.nadrlab.baitbudget.data.model.Store
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {

    @Query("SELECT * FROM stores ORDER BY name ASC")
    fun getAllStores(): Flow<List<Store>>

    @Query("SELECT * FROM stores ORDER BY name ASC")
    suspend fun getAllStoresOnce(): List<Store>

    @Query("SELECT * FROM stores WHERE id = :id")
    suspend fun getStoreById(id: Long): Store?

    @Query("SELECT * FROM stores WHERE name = :name LIMIT 1")
    suspend fun getStoreByName(name: String): Store?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStore(store: Store): Long

    @Update
    suspend fun updateStore(store: Store)

    @Delete
    suspend fun deleteStore(store: Store)
}
