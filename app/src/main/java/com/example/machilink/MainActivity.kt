package com.example.machilink

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.example.machilink.R

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Firebase Auth の初期化
        auth = FirebaseAuth.getInstance()

        // ログアウトボタンの設定
        val logoutButton: Button = findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // ログイン画面に戻る
        }

        // ポイント譲渡ボタンの設定
        val transferPointsButton: Button = findViewById(R.id.transferPointsButton)
        transferPointsButton.setOnClickListener {
            val intent = Intent(this, TransferPointsActivity::class.java)
            startActivity(intent)
        }
    }
}