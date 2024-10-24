package com.example.machilink.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.machilink.manager.PointBalanceManager
import com.example.machilink.manager.SecurityManager
import com.example.machilink.manager.TransferHistoryManager

class PointTransferViewModelFactory(
    private val transferHistoryManager: TransferHistoryManager,
    private val pointBalanceManager: PointBalanceManager,
    private val securityManager: SecurityManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PointTransferViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PointTransferViewModel(
                transferHistoryManager,
                pointBalanceManager,
                securityManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}