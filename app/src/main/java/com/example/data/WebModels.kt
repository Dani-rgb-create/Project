package com.example.data

import androidx.room.*

@Entity(tableName = "web_projects")
data class WebProject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val folderName: String, // name of directory inside filesDir/projects/
    val healthScore: Int = 100,
    val lastAnalyzed: Long = 0L
)

@Entity(
    tableName = "project_backups",
    foreignKeys = [
        ForeignKey(
            entity = WebProject::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class ProjectBackup(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val relativeFilePath: String, // e.g., "index.html"
    val contentBefore: String,
    val contentAfter: String, // proposed, or overwritten with newer content
    val backupName: String, // describe the edit, e.g., "AI SEO optimization"
    val timestamp: Long = System.currentTimeMillis(),
    val isReverted: Boolean = false
)
