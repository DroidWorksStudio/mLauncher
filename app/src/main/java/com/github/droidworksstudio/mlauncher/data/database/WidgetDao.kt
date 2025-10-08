package com.github.droidworksstudio.mlauncher.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.github.droidworksstudio.mlauncher.data.SavedWidgetEntity

@Dao
interface WidgetDao {
    @Query("SELECT * FROM widgets")
    suspend fun getAll(): List<SavedWidgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(widgets: List<SavedWidgetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(widget: SavedWidgetEntity)

    @Delete
    suspend fun delete(widget: SavedWidgetEntity)

    @Query("DELETE FROM widgets")
    suspend fun deleteAll()
}
