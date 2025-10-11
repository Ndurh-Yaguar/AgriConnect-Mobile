package com.example.agriconnect


data class Farm(
    val name: String = "",
    val ownerId: String = "",
    val address: String? = null,
    val description: String? = null,
    val createdAt: com.google.firebase.Timestamp? = null
)
