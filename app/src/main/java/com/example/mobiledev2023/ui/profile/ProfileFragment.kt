package com.example.mobiledev2023.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mobiledev2023.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        val currentUserUID = currentUser?.uid

        Log.d("ProfileFragment", "Current User UID: $currentUserUID")

        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        if (currentUserUID != null) {
            db.collection("users").document(currentUserUID)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userData = document.data
                        displayUserData(view, userData)
                        // Log success
                        Log.d("ProfileFragment", "Data retrieval succeeded: $userData")
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle failures
                    // Log failure
                    Log.e("ProfileFragment", "Data retrieval failed", exception)
                }
        }

        val leftHandTextView = view.findViewById<TextView>(R.id.text_left_hand)
        val rightHandTextView = view.findViewById<TextView>(R.id.text_right_hand)
        val bothHandsTextView = view.findViewById<TextView>(R.id.text_both_hands)

        leftHandTextView.setOnClickListener {
            updateBestHand(currentUserUID, "Left")
        }

        rightHandTextView.setOnClickListener {
            updateBestHand(currentUserUID, "Right")
        }

        bothHandsTextView.setOnClickListener {
            updateBestHand(currentUserUID, "Both")
        }

        return view
    }

    private fun displayUserData(view: View, userData: Map<String, Any>?) {
        val bestHand = userData?.get("pref_best_hand").toString()

        // Show name
        view.findViewById<TextView>(R.id.text_name)?.text =
            userData?.get("first_name").toString() + " " + userData?.get("last_name").toString()

        // Show best hand
        val leftHandTextView = view.findViewById<TextView>(R.id.text_left_hand)
        val rightHandTextView = view.findViewById<TextView>(R.id.text_right_hand)
        val bothHandsTextView = view.findViewById<TextView>(R.id.text_both_hands)

        // Reset background color for all hands
        leftHandTextView.setBackgroundResource(android.R.color.transparent)
        rightHandTextView.setBackgroundResource(android.R.color.transparent)
        bothHandsTextView.setBackgroundResource(android.R.color.transparent)

        // Set background color for the current best hand
        when (bestHand) {
            "Left" -> leftHandTextView.setBackgroundResource(R.color.purple_200)
            "Right" -> rightHandTextView.setBackgroundResource(R.color.purple_200)
            "Both" -> bothHandsTextView.setBackgroundResource(R.color.purple_200)
        }
    }


    private fun updateBestHand(userId: String?, hand: String) {
        if (userId != null) {
            val userRef = db.collection("users").document(userId)
            userRef.update("pref_best_hand", hand)
                .addOnSuccessListener {
                    Log.d("ProfileFragment", "Best hand updated: $hand")
                }
                .addOnFailureListener { exception ->
                    Log.e("ProfileFragment", "Failed to update best hand", exception)
                }
        }
    }
}
