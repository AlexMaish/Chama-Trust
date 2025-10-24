package com.example.chamabuddy.domain.model

data class SavingsProgress(
    val targetAmount: Int,
    val currentAmount: Int,
    val membersCompleted: Int,
    val totalMembers: Int
)