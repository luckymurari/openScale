/*
 * openScale
 * Copyright (C) 2025 olie.xdev <olie.xdev@googlemail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.health.openscale.core.database

import androidx.room.*
import com.health.openscale.core.data.Insight
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Insight entities.
 */
@Dao
interface InsightDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: Insight)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(insights: List<Insight>)
    
    @Update
    suspend fun update(insight: Insight)
    
    @Delete
    suspend fun delete(insight: Insight)
    
    @Query("DELETE FROM Insight WHERE id = :id")
    suspend fun deleteInsight(id: String)
    
    @Query("DELETE FROM Insight WHERE userId = :userId")
    suspend fun deleteInsightsByUserId(userId: Int): Int
    
    @Query("SELECT * FROM Insight WHERE userId = :userId ORDER BY createdAt DESC")
    fun getInsightsForUser(userId: Int): Flow<List<Insight>>
    
    @Query("SELECT * FROM Insight ORDER BY createdAt DESC")
    suspend fun getAllInsights(): List<Insight>
    
    @Query("SELECT * FROM Insight WHERE id = :id")
    suspend fun getInsightById(id: String): Insight?
    
    @Query("SELECT * FROM Insight WHERE userId = :userId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getMostRecentInsight(userId: Int): Insight?
    
    @Query("SELECT COUNT(*) FROM Insight WHERE userId = :userId")
    fun getInsightCountForUser(userId: Int): Flow<Int>
}
