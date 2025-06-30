package com.example.chamabuddy.domain.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class GroupWithMembers(
    @Embedded val group: Group,
    @Relation(
        parentColumn = "group_id",
        entityColumn = "group_id" // Direct relation without junction
    )
    val members: List<Member>
)