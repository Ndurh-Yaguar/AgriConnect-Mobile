package com.example.agriconnect

import com.google.firebase.Timestamp

// User data model - stored in users collection
data class User(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val emailVerified: Boolean = false,
    val isSeller: Boolean = false,
    val isAdmin: Boolean = false,  // Added admin flag
    val createdAt: Timestamp? = null
)

// Farm data model - stored in farms collection
data class Farm(
    val name: String = "",
    val ownerId: String = "",
    val address: String? = null,
    val description: String? = null,
    val createdAt: Timestamp? = null
)

// Product data model - stored in products collection
data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val grade: String = "",
    val price: Double = 0.0,
    val unit: String = "",
    val availableQuantity: Int = 0,
    val farmId: String = "",
    val farmName: String = "",
    val ownerId: String = "",
    val ownerEmail: String = "",
    val productImageUrl: String = "",
    val certificateUrl: String = "",
    val status: String = "pending", // pending, approved, rejected
    val createdAt: Timestamp? = null,
    val approvedAt: Timestamp? = null,
    val approvedBy: String? = null,
    val rejectionReason: String? = null
)

// Order data model - stored in orders collection
data class Order(
    val orderId: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val farmId: String = "",
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: String = "pending", // pending, confirmed, shipped, delivered, cancelled
    val deliveryAddress: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val pricePerUnit: Double = 0.0,
    val subtotal: Double = 0.0,
val updatedAt: Timestamp? = null
)

data class AdminStats(
    val totalProducts: Int = 0,
    val pendingProducts: Int = 0,
    val approvedProducts: Int = 0,
    val rejectedProducts: Int = 0,
    val totalUsers: Int = 0,
    val totalFarms: Int = 0
)
