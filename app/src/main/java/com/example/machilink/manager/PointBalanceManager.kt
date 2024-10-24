package com.example.machilink.manager

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.example.machilink.exception.*

class PointBalanceManager {
    private val db = FirebaseFirestore.getInstance()
    private val usersRef = db.collection("users")

    // 新規ユーザー作成時に初期ポイントを設定
    suspend fun initializeUserPoints(userId: String, initialPoints: Int = 100) {
        withContext(Dispatchers.IO) {
            try {
                val userDoc = usersRef.document(userId)
                val snapshot = userDoc.get().await()
                if (!snapshot.exists()) {
                    val userData = hashMapOf(
                        "points" to initialPoints,
                        "createdAt" to System.currentTimeMillis()
                    )
                    userDoc.set(userData).await()
                }
            } catch (e: Exception) {
                throw BalanceException("Failed to initialize user points", e)
            }
        }
    }

    suspend fun getBalance(userId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = usersRef.document(userId).get().await()
                snapshot.getLong("points")?.toInt() ?: 0
            } catch (e: Exception) {
                throw BalanceException("Failed to get balance", e)
            }
        }
    }

    suspend fun processTransfer(senderId: String, receiverId: String, amount: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                db.runTransaction { transaction ->
                    val senderDoc = usersRef.document(senderId)
                    val receiverDoc = usersRef.document(receiverId)

                    val senderBalance = transaction.get(senderDoc)
                        .getLong("points")?.toInt() ?: 0

                    if (senderBalance < amount) {
                        throw InsufficientBalanceException("Insufficient balance")
                    }

                    val receiverBalance = transaction.get(receiverDoc)
                        .getLong("points")?.toInt() ?: 0

                    transaction.update(senderDoc, "points", senderBalance - amount)
                    transaction.update(receiverDoc, "points", receiverBalance + (amount * 2))

                    true
                }.await()
            } catch (e: Exception) {
                throw TransferException("Transfer failed", e)
            }
        }
    }
}