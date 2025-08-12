package com.example.chamabuddy.domain.Firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class MemberFire(
    val memberId: String = "",
    val name: String = "",
    val nickname: String? = null,
    val phoneNumber: String = "",
    val profilePicture: String? = null,

    @get:PropertyName("admin")
    @set:PropertyName("admin")
    var isAdmin: Boolean = false,

    @get:PropertyName("active")
    @set:PropertyName("active")
    var isActive: Boolean = true,

    val joinDate: Timestamp = Timestamp.now(),
    val userId: String? = null,
    val groupId: String = "",

    @get:PropertyName("owner")
    @set:PropertyName("owner")
    var isOwner: Boolean = false,

    val lastUpdated: Timestamp = Timestamp.now()
)