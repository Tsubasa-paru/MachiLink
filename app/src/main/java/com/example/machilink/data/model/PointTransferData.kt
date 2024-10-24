package com.example.machilink.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PointTransferData(
    val senderId: String,
    val amount: Int,
    val timestamp: Long,
    val signature: String,
    val type: TransferType
)