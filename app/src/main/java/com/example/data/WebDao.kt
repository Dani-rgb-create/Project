package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WebDao {
    // Projects
    @Query("SELECT * FROM web_projects ORDER BY lastAnalyzed DESC")
    fun getAllProjectsFlow(): Flow<List<WebProject>>

    @Query("SELECT * FROM web_projects WHERE id = :id")
    suspend fun getProjectById(id: Int): WebProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: WebProject): Long

    @Update
    suspend fun updateProject(project: WebProject)

    @Delete
    suspend fun deleteProject(project: WebProject)

    @Query("DELETE FROM web_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)

    // Backups
    @Query("SELECT * FROM project_backups WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getBackupsForProjectFlow(projectId: Int): Flow<List<ProjectBackup>>

    @Query("SELECT * FROM project_backups WHERE projectId = :projectId ORDER BY timestamp DESC")
    suspend fun getBackupsForProject(projectId: Int): List<ProjectBackup>

    @Query("SELECT * FROM project_backups WHERE id = :id")
    suspend fun getBackupById(id: Int): ProjectBackup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: ProjectBackup): Long

    @Update
    suspend fun updateBackup(backup: ProjectBackup)

    @Query("DELETE FROM project_backups WHERE id = :id")
    suspend fun deleteBackupById(id: Int)

    @Query("DELETE FROM project_backups WHERE projectId = :projectId")
    suspend fun clearBackupsForProject(projectId: Int)
}
