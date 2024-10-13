package com.example.machilink

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.machilink.R

class TransferPointsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_points)

        // ポイント譲渡のUIと処理
        val recipientEditText: EditText = findViewById(R.id.recipientEditText)
        val pointsEditText: EditText = findViewById(R.id.pointsEditText)
        val transferButton: Button = findViewById(R.id.transferButton)

        transferButton.setOnClickListener {
            val recipient = recipientEditText.text.toString()
            val points = pointsEditText.text.toString().toIntOrNull()

            if (recipient.isNotEmpty() && points != null && points > 0) {
                // ポイント譲渡の処理をここに追加
                Toast.makeText(this, "Transferred $points points to $recipient.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a valid recipient and point amount.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}