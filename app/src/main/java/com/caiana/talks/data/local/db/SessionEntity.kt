package com.caiana.talks.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_profile_id", index = true) val userProfileId: Int,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long,
    val transcript: String = "",
    @ColumnInfo(name = "status") val status: String = "completed",
    @ColumnInfo(name = "mode") val mode: String = "single",
    @ColumnInfo(name = "vocabulary") val vocabulary: String = "",
    @ColumnInfo(name = "co_practice_group_id") val coPracticeGroupId: String? = null
)
