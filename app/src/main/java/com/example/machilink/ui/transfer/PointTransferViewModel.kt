package com.example.machilink.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.machilink.data.model.TransferHistory
import com.example.machilink.data.model.TransferType
import com.example.machilink.data.model.TransferStatus
import com.example.machilink.data.model.TransferMethod
import com.example.machilink.manager.TransferHistoryManager
import com.example.machilink.manager.PointBalanceManager
import com.example.machilink.manager.SecurityManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.machilink.ui.transfer.TransferState

class PointTransferViewModel(
    private val transferHistoryManager: TransferHistoryManager,
    private val pointBalanceManager: PointBalanceManager,
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _transferState = MutableStateFlow<TransferState>(TransferState.Initial)
    val transferState = _transferState.asStateFlow()

    private val _balance = MutableStateFlow<Int>(0)
    val balance = _balance.asStateFlow()

    private val _transferHistory = MutableStateFlow<List<TransferHistory>>(emptyList())
    val transferHistory = _transferHistory.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw Exception("User not authenticated")
                val points = pointBalanceManager.getBalance(userId)
                _balance.value = points
            } catch (e: Exception) {
                _transferState.value = TransferState.Error(e.message ?: "Failed to load user data")
            }
        }
    }

    fun initiateTransfer(receiverId: String, amount: Int, method: TransferMethod) {
        viewModelScope.launch {
            try {
                _transferState.value = TransferState.Preparing
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw Exception("User not authenticated")

                val success = pointBalanceManager.processTransfer(userId, receiverId, amount)
                if (success) {
                    val transfer = TransferHistory(
                        senderId = userId,
                        receiverId = receiverId,
                        amount = amount,
                        type = TransferType.NORMAL,
                        timestamp = System.currentTimeMillis(),
                        status = TransferStatus.SUCCESS,
                        method = method
                    )
                    transferHistoryManager.recordTransfer(transfer)
                    _transferState.value = TransferState.TransferComplete(amount)
                    loadUserData()
                }
            } catch (e: Exception) {
                _transferState.value = TransferState.Error(e.message ?: "Transfer failed")
            }
        }
    }
}