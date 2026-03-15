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

package com.health.openscale.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Represents an AI-generated insight for a user.
 * Insights are generated based on measurement trends and user goals.
 */
@Entity(tableName = "Insight")
data class Insight(
    @PrimaryKey
    val id: String,
    /** The insight title - short summary */
    val title: String,
    /** The full insight message */
    val message: String,
    /** Trend assessment: "improving", "stable", or "needs_attention" */
    val trend: String,
    /** Confidence level (0-100) based on data quality */
    val confidence: Int,
    /** When this insight was generated */
    val createdAt: Instant = Instant.now()
)
