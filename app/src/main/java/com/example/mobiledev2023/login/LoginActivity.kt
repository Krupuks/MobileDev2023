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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()

        val textViewToggleRegister = findViewById<TextView>(R.id.login_textViewToggle)
        textViewToggleRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        val buttonLogin = findViewById<Button>(R.id.login_button)
        buttonLogin.setOnClickListener {
            val username = findViewById<EditText>(R.id.login_editTextEmail).text.toString().trim()
            val password = findViewById<EditText>(R.id.login_editTextPassword).text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                loginUser(username, password)
            } else {
                Toast.makeText(this, "Username or password is empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginUser(username: String, password: String) {
        mAuth.signInWithEmailAndPassword(username, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login successful
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    // Redirect to the desired activity after successful login
                    startActivity(Intent(this, MainActivity::class.java))
                    finish() // Close this activity so user can't go back with back button
                } else {
                    // Login failed
                    when (task.exception) {
                        is FirebaseAuthInvalidUserException -> {
                            // User doesn't exist or email is incorrect
                            Toast.makeText(this, "User does not exist", Toast.LENGTH_SHORT).show()
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            // Incorrect password
                            Toast.makeText(this, "Invalid password", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            // Other errors
                            Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }
}
