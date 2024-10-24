package com.example.machilink.ui.transfer

sealed class TransferState {
    object Initial : TransferState()
    object Preparing : TransferState()
    data class TransferComplete(val amount: Int) : TransferState()
    data class Error(val message: String) : TransferState()
}