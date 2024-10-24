package com.example.machilink.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading
                auth.signInWithEmailAndPassword(email, password).await()
                _loginState.value = LoginState.Success(auth.currentUser)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Authentication failed")
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                _loginState.value = LoginState.Success(auth.currentUser)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Google sign-in failed")
            }
        }
    }

    fun createAccount(email: String, password: String) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading
                auth.createUserWithEmailAndPassword(email, password).await()
                _loginState.value = LoginState.Success(auth.currentUser)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Account creation failed")
            }
        }
    }

    fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _loginState.value = LoginState.Success(currentUser)
        }
    }

    sealed class LoginState {
        object Initial : LoginState()
        object Loading : LoginState()
        data class Success(val user: com.google.firebase.auth.FirebaseUser?) : LoginState()
        data class Error(val message: String) : LoginState()
    }
}