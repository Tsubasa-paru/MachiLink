package com.example.machilink.manager

import com.example.machilink.data.model.TransferHistory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import com.example.machilink.exception.TransferException

class TransferHistoryManager {
    private val db = FirebaseFirestore.getInstance()
    private val transfersRef = db.collection("transfers")

    suspend fun recordTransfer(transfer: TransferHistory) {
        withContext(Dispatchers.IO) {
            try {
                transfersRef.add(transfer)
            } catch (e: Exception) {
                throw TransferException("Failed to record transfer", e)
            }
        }
    }

    fun getTransferHistory(userId: String): Flow<List<TransferHistory>> = callbackFlow {
        val listener = transfersRef
            .whereEqualTo("senderId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val transfers = snapshot?.toObjects(TransferHistory::class.java) ?: emptyList()
                trySend(transfers)
            }

        awaitClose { listener.remove() }
    }
}