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
    val dateAwarded: Date = Date(),

    @ColumnInfo(name = "cycle_id")
    val cycleId: String
) {
    init {
        require(memberId.isNotBlank()) { "memberId cannot be blank" }
        require(cycleId.isNotBlank()) { "cycleId cannot be blank" }
        require(meetingId.isNotBlank()) { "meetingId cannot be blank" }
        require(amountReceived > 0) { "amountReceived must be positive" }
        require(paymentOrder > 0) { "paymentOrder must be positive" }
    }
}











//
//@Entity(
//    tableName = "beneficiaries",
//    foreignKeys = [
//
//        ForeignKey(
//            entity = Cycle::class,
//            parentColumns = ["cycle_id"],
//            childColumns = ["cycle_id"],
//            onDelete = ForeignKey.CASCADE
//        ),
//
//        ForeignKey(
//            entity = WeeklyMeeting::class,
//            parentColumns = ["meeting_id"],
//            childColumns = ["meeting_id"],
//            onDelete = ForeignKey.CASCADE
//        ),
//        ForeignKey(
//            entity = Member::class,
//            parentColumns = ["member_id"],
//            childColumns = ["member_id"],
//            onDelete = ForeignKey.CASCADE
//        )
//    ],
//    indices = [
//        Index(value = ["meeting_id"]),
//        Index(value = ["member_id"]),
//        Index(value = ["cycle_id"])
//   ]
//)
//data class Beneficiary(
//    @PrimaryKey
//    @ColumnInfo(name = "beneficiary_id")
//    val beneficiaryId: String,
//
//    @ColumnInfo(name = "meeting_id")
//    val meetingId: String,
//
//    @ColumnInfo(name = "member_id")
//    val memberId: String,
//
//    @ColumnInfo(name = "amount_received")
//    val amountReceived: Int,
//
//    @ColumnInfo(name = "payment_order")
//    val paymentOrder: Int,
//
//    @ColumnInfo(name = "date_awarded")
//    val dateAwarded: Date = System.currentTimeMillis(),
//
//    @ColumnInfo(name = "cycle_id")
//    val cycleId: String
//
//
//)

