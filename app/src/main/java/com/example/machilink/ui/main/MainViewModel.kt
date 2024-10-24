package com.example.machilink.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.machilink.data.model.TransferHistory
import com.example.machilink.manager.PointBalanceManager
import com.example.machilink.manager.TransferHistoryManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val pointBalanceManager: PointBalanceManager,
    private val transferHistoryManager: TransferHistoryManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUIState>(MainUIState.Loading)
    val uiState: StateFlow<MainUIState> = _uiState.asStateFlow()

    private val _userPoints = MutableStateFlow<Int>(0)
    val userPoints: StateFlow<Int> = _userPoints.asStateFlow()

    private val _recentTransfers = MutableStateFlow<List<TransferHistory>>(emptyList())
    val recentTransfers: StateFlow<List<TransferHistory>> = _recentTransfers.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                _uiState.value = MainUIState.Loading
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw Exception("User not authenticated")

                // ポイント残高の取得
                val points = pointBalanceManager.getBalance(userId)
                _userPoints.value = points

                // 最近の取引履歴の取得
                transferHistoryManager.getTransferHistory(userId)
                    .collect { transfers ->
                        _recentTransfers.value = transfers
                    }

                _uiState.value = MainUIState.Success
            } catch (e: Exception) {
                _uiState.value = MainUIState.Error(e.message ?: "Failed to load user data")
            }
        }
    }

    fun refreshData() {
        loadUserData()
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        _uiState.value = MainUIState.SignedOut
    }

    sealed class MainUIState {
        object Loading : MainUIState()
        object Success : MainUIState()
        object SignedOut : MainUIState()
        data class Error(val message: String) : MainUIState()
    }

    // Factory for creating MainViewModel with dependencies
    class Factory(
        private val pointBalanceManager: PointBalanceManager,
        private val transferHistoryManager: TransferHistoryManager
    ) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(pointBalanceManager, transferHistoryManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}