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
import com.google.firebase.firestore.FirebaseFirestore

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
                    val currentUser = mAuth.currentUser
                    currentUser?.let {
                        val userId = it.uid
                        val user = hashMapOf(
                            "first_name" to findViewById<EditText>(R.id.register_editTextFirstName).text.toString().trim(),
                            "last_name" to findViewById<EditText>(R.id.register_editTextLastName).text.toString().trim(),
                            "pref_best_hand" to "",
                            "pref_court_position" to "",
                            "pref_match_type" to "",
                            "pref_time_to_play" to "",
                            "stat_matches" to 0
                        )

                        // Save user details to Firestore
                        val db = FirebaseFirestore.getInstance()
                        db.collection("users").document(userId)
                            .set(user)
                            .addOnSuccessListener {
                                // Firestore save successful
                                Toast.makeText(this, "User data saved to Firestore", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                // Firestore save failed
                                Toast.makeText(this, "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
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
