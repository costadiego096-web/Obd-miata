package com.example.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "track_sessions")
data class TrackSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val circuitName: String,
    val timestamp: Long,
    val durationMs: Long,
    val totalLaps: Int,
    val bestLapMs: Long?,
    val topSpeedKmh: Float,
    val maxLatG: Float,
    val maxBrakingG: Float,
    val vehicleDesc: String
)

@Entity(
    tableName = "lap_records",
    foreignKeys = [
        ForeignKey(
            entity = TrackSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class LapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val lapNumber: Int,
    val lapTimeMs: Long,
    val topSpeedKmh: Float,
    val avgSpeedKmh: Float,
    val isBest: Boolean
)

@Entity(
    tableName = "telemetry_samples",
    foreignKeys = [
        ForeignKey(
            entity = TrackSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId", "lapNumber"])]
)
data class TelemetrySampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val lapNumber: Int,
    val timeOffsetMs: Long,
    val speedKmh: Float,
    val rpm: Float,
    val throttlePct: Float,
    val coolantC: Float,
    val gear: String,
    val latG: Float,
    val longG: Float,
    val timingDeg: Float
)
