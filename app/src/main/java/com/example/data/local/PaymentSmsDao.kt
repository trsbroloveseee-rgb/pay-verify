package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentSmsDao {

    @Query("SELECT * FROM payment_sms_queue ORDER BY receivedAt DESC")
    fun observeAllPayments(): Flow<List<PaymentSmsEntity>>

    @Query("SELECT * FROM payment_sms_queue WHERE status != 'SYNCED' ORDER BY receivedAt ASC")
    suspend fun getPendingPayments(): List<PaymentSmsEntity>

    @Query("SELECT COUNT(*) FROM payment_sms_queue WHERE status != 'SYNCED'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM payment_sms_queue WHERE status = 'SYNCED'")
    fun observeSyncedCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payment_sms_queue WHERE status = 'SYNCED'")
    fun observeTotalSyncedVolume(): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPaymentSms(entity: PaymentSmsEntity): Long

    @Query(
        """
        UPDATE payment_sms_queue 
        SET status = 'SYNCED',
            syncAttempts = syncAttempts + 1,
            lastHttpCode = :httpCode,
            lastResponseMessage = :responseMessage,
            hmacSignature = :hmacSignature,
            syncedAt = :syncedAt
        WHERE id = :id
        """
    )
    suspend fun markAsSynced(
        id: Long,
        httpCode: Int,
        responseMessage: String,
        hmacSignature: String,
        syncedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE payment_sms_queue 
        SET status = 'UNSYNCED',
            syncAttempts = syncAttempts + 1,
            lastHttpCode = :httpCode,
            lastResponseMessage = :errorMessage,
            hmacSignature = :hmacSignature
        WHERE id = :id
        """
    )
    suspend fun recordSyncFailure(
        id: Long,
        httpCode: Int?,
        errorMessage: String,
        hmacSignature: String
    )

    @Query(
        """
        UPDATE payment_sms_queue 
        SET status = 'UNSYNCED',
            lastResponseMessage = 'Queued for manual retry'
        WHERE id = :id
        """
    )
    suspend fun resetToPending(id: Long)

    @Query("DELETE FROM payment_sms_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM payment_sms_queue WHERE status = 'SYNCED'")
    suspend fun deleteSyncedPayments()
}
