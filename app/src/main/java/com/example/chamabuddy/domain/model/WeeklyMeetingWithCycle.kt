package com.example.chamabuddy.domain.model

import androidx.room.Embedded
import androidx.room.Relation

data class WeeklyMeetingWithCycle(
    @Embedded val meeting: WeeklyMeeting,

    @Relation(
        parentColumn = "cycle_id",
        entityColumn = "cycle_id"
    )
    val cycle: Cycle
)
