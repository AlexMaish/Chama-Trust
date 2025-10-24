package com.example.chamabuddy.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index
import java.util.Date
import java.util.UUID


@Entity(
    tableName = "beneficiaries",
    foreignKeys = [
        ForeignKey(
            entity = Cycle::class,
            parentColumns = ["cycle_id"],
            childColumns = ["cycle_id"],
            onDelete = ForeignKey.CASCADE
        ),

        ForeignKey(
            entity = Group::class,
            parentColumns = ["group_id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WeeklyMeeting::class,
            parentColumns = ["meeting_id"],
            childColumns = ["meeting_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Member::class,
            parentColumns = ["member_id"],
            childColumns = ["member_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["meeting_id"]),
        Index(value = ["member_id"]),
        Index(value = ["cycle_id"])
    ]
)
data class Beneficiary(
    @PrimaryKey
    @ColumnInfo(name = "beneficiary_id")
    val beneficiaryId: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "meeting_id")
    val meetingId: String,

    @ColumnInfo(name = "member_id")
    val memberId: String,

    @ColumnInfo(name = "amount_received")
    val amountReceived: Int,

    @ColumnInfo(name = "payment_order")
    val paymentOrder: Int,

    @ColumnInfo(name = "date_awarded")
    val dateAwarded: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "cycle_id")
    val cycleId: String,

    @ColumnInfo(name = "group_id")
    val groupId: String,

    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long = 0


) {
    init {
        require(memberId.isNotBlank()) { "memberId cannot be blank" }
        require(cycleId.isNotBlank()) { "cycleId cannot be blank" }
        require(meetingId.isNotBlank()) { "meetingId cannot be blank" }
        require(amountReceived > 0) { "amountReceived must be positive" }
        require(paymentOrder > 0) { "paymentOrder must be positive" }
    }
}



