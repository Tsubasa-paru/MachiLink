package com.example.machilink.data.model

data class TransferHistory(
    val id: String = "",
    val senderId: String,
    val receiverId: String,
    val amount: Int,
    val type: TransferType,
    val timestamp: Long,
    val status: TransferStatus,
    val method: TransferMethod
)