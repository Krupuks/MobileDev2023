package com.example.mobiledev2023.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobiledev2023.MainActivity
import com.example.mobiledev2023.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class RegisterActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        mAuth = FirebaseAuth.getInstance()

        val textViewToggleLogin = findViewById<TextView>(R.id.register_textViewToggle)
        textViewToggleLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        val buttonRegister = findViewById<Button>(R.id.register_button)
        buttonRegister.setOnClickListener {
            val firstName = findViewById<EditText>(R.id.register_editTextFirstName).text.toString().trim()
            val lastName = findViewById<EditText>(R.id.register_editTextLastName).text.toString().trim()
            val email = findViewById<EditText>(R.id.register_editTextEmail).text.toString().trim()
            val password = findViewById<EditText>(R.id.register_editTextPassword).text.toString().trim()
            val passwordCheck = findViewById<EditText>(R.id.register_editTextPasswordCheck).text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty() && password == passwordCheck) {
                registerUser(email, password)
            } else {
                Toast.makeText(this, "Please fill all fields and ensure passwords match", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(email: String, password: String) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registration successful
                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                    // Redirect to the desired activity after successful registration
                    startActivity(Intent(this, MainActivity::class.java))
                    finish() // Close this activity so user can't go back with back button
                } else {
                    // Registration failed
                    when (task.exception) {
                        is FirebaseAuthUserCollisionException -> {
                            // User already exists with this email
                            Toast.makeText(this, "User already exists with this email", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            // Other errors
                            Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }
}
