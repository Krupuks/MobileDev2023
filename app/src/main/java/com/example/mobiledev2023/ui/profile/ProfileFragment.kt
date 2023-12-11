package com.example.mobiledev2023.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mobiledev2023.R
import com.example.mobiledev2023.ui.builders.PreferenceBuilder
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

        val currentUserUID = auth.currentUser?.uid

        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        val profileContainer = view.findViewById<LinearLayout>(R.id.profile_container)

        // Fetch user properties from Firestore
        if (currentUserUID != null) {
            db.collection("users").document(currentUserUID)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userProperties = document.data?.toMutableMap() ?: mutableMapOf()
                        // Remove unnecessary data or keys not needed for preferences display
                        displayUserProperties(profileContainer, userProperties, currentUserUID)
                        Log.d("ProfileFragment", "User preferences retrieved: $userProperties")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ProfileFragment", "Failed to retrieve user preferences", exception)
                }
        }

        return view
    }

    private fun displayUserProperties(
        profileContainer: LinearLayout,
        userPreferences: MutableMap<String, Any>,
        currentUserUID: String
    ) {
        //NAME

        // Retrieve first name and last name from userPreferences map
        val firstName = userPreferences["first_name"] as? String ?: ""
        val lastName = userPreferences["last_name"] as? String ?: ""

        // Access the TextView in your layout file where you want to display the name
        val nameTextView = profileContainer.findViewById<TextView>(R.id.text_name)
        nameTextView.text = "$firstName $lastName"


        //PREFERENCES
        val tileOptions = mapOf(
            "Best hand" to listOf("Left", "Right", "Both"),
            "Court position" to listOf("Forehand", "Backhand", "Both"),
            "Match type" to listOf("Competitive", "Friendly", "Both"),
            "Time to play" to listOf("Morning", "Noon", "Evening")
        )
        val dynamicCard = PreferenceBuilder(requireContext()).buildCard(
            "Player Preferences",
            tileOptions,
            currentUserUID,
            userPreferences
        )
        profileContainer.addView(dynamicCard)


    }
}