package com.example.machilink.exception

open class TransferException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

class InsufficientBalanceException(message: String) :
    TransferException(message)

class SecurityException(message: String, cause: Throwable? = null) :
    TransferException(message, cause)

class BalanceException(message: String, cause: Throwable? = null) :
    TransferException(message, cause)

class NetworkException(message: String, cause: Throwable? = null) :
    TransferException(message, cause)