package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Query("SELECT * FROM track_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<TrackSessionEntity>>

    @Query("SELECT * FROM track_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): TrackSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TrackSessionEntity): Long

    @Query("DELETE FROM track_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("SELECT * FROM lap_records WHERE sessionId = :sessionId ORDER BY lapNumber ASC")
    fun getLapsForSession(sessionId: Long): Flow<List<LapEntity>>

    @Query("SELECT * FROM lap_records WHERE sessionId = :sessionId ORDER BY lapNumber ASC")
    suspend fun getLapsForSessionSync(sessionId: Long): List<LapEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLap(lap: LapEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaps(laps: List<LapEntity>)

    @Query("SELECT * FROM telemetry_samples WHERE sessionId = :sessionId AND (:lapNumber IS NULL OR lapNumber = :lapNumber) ORDER BY timeOffsetMs ASC")
    fun getTelemetryForSession(sessionId: Long, lapNumber: Int? = null): Flow<List<TelemetrySampleEntity>>

    @Query("SELECT * FROM telemetry_samples WHERE sessionId = :sessionId ORDER BY timeOffsetMs ASC")
    suspend fun getTelemetryForSessionSync(sessionId: Long): List<TelemetrySampleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelemetrySamples(samples: List<TelemetrySampleEntity>)

    @Transaction
    suspend fun saveFullSession(
        session: TrackSessionEntity,
        laps: List<LapEntity>,
        telemetry: List<TelemetrySampleEntity>
    ): Long {
        val sessionId = insertSession(session)
        val updatedLaps = laps.map { it.copy(sessionId = sessionId) }
        insertLaps(updatedLaps)
        val updatedTelemetry = telemetry.map { it.copy(sessionId = sessionId) }
        insertTelemetrySamples(updatedTelemetry)
        return sessionId
    }
}
