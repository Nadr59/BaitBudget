package com.nadrlab.baitbudget.data.db

import androidx.room.*
import com.nadrlab.baitbudget.data.model.Report
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {

    @Query("SELECT * FROM reports ORDER BY date DESC")
    fun getAllReports(): Flow<List<Report>>

    @Query("SELECT * FROM reports WHERE userName = :userName ORDER BY date DESC")
    fun getReportsByUser(userName: String): Flow<List<Report>>

    @Query("SELECT * FROM reports WHERE isRead = 0")
    fun getUnreadReports(): Flow<List<Report>>

    @Query("SELECT COUNT(*) FROM reports WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("SELECT DISTINCT userName FROM reports ORDER BY userName")
    fun getAllUserNames(): Flow<List<String>>

    @Insert
    suspend fun insertReport(report: Report): Long

    @Delete
    suspend fun deleteReport(report: Report)

    @Query("UPDATE reports SET isRead = 1 WHERE id = :reportId")
    suspend fun markAsRead(reportId: Long)

    @Query("DELETE FROM reports WHERE userName = :userName")
    suspend fun deleteAllForUser(userName: String)
}
