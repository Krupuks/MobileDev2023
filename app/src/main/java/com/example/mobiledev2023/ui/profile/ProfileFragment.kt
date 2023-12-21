package com.example.mobiledev2023.ui.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mobiledev2023.R
import com.example.mobiledev2023.login.LoginActivity
import com.example.mobiledev2023.ui.builders.MyReservationsBuilder
import com.example.mobiledev2023.ui.builders.PreferenceBuilder
import com.example.mobiledev2023.ui.builders.ShowMatchesBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val userID = auth.currentUser?.uid

        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        val profileContainer = view.findViewById<LinearLayout>(R.id.preference_container)
        val logoutButton = view.findViewById<Button>(R.id.button_log_out)

        // Fetch user properties from Firestore
        if (userID != null) {
            db.collection("users").document(userID)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userProperties = document.data?.toMutableMap() ?: mutableMapOf()
                        // Remove unnecessary data or keys not needed for preferences display
                        displayUserProperties(profileContainer, userProperties, userID, db)
                        Log.d("ProfileFragment", "User preferences retrieved: $userProperties")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ProfileFragment", "Failed to retrieve user preferences", exception)
                }
        }

        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            // Navigate back to the login screen
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }

        return view
    }

    private fun displayUserProperties(
        profileContainer: LinearLayout,
        userPreferences: MutableMap<String, Any>,
        currentUserUID: String,
        db: FirebaseFirestore
    ) {
        //NAME

        // Retrieve first name and last name from userPreferences map
        val firstName = userPreferences["first_name"] as? String ?: ""
        val lastName = userPreferences["last_name"] as? String ?: ""
        val initials = getInitials("$firstName $lastName")

        // Access the TextView in your layout file where you want to display the name
        val firstNameTextView = profileContainer.findViewById<TextView>(R.id.text_first_name)
        val lastNameTextView = profileContainer.findViewById<TextView>(R.id.text_last_name)
        val initialsTextView = profileContainer.findViewById<TextView>(R.id.text_icon)
        firstNameTextView.text = firstName
        lastNameTextView.text = lastName
        initialsTextView.text = initials


        //PREFERENCES
        val tileOptions = mapOf(
            "Best hand" to listOf("Left", "Right", "Both"),
            "Court position" to listOf("Left", "Right", "Both"),
            "Match type" to listOf("Competitive", "Friendly", "Both"),
            "Time to play" to listOf("Morning", "Noon", "Evening")
        )
        val dynamicCard = PreferenceBuilder(requireContext()).buildCard(
            "Player Preferences",
            tileOptions,
            currentUserUID,
            userPreferences
        )

        val dynamicCard2 = MyReservationsBuilder(requireContext(), db).buildCard(
            "My Court Reservations",
            requireContext()
        )

        val dynamicCard3 = ShowMatchesBuilder(requireContext(), db).buildCard("My Matches", true)

        profileContainer.addView(dynamicCard)
        profileContainer.addView(dynamicCard2)
        profileContainer.addView(dynamicCard3)

    }

    private fun getInitials(name: String): String {
        if (name == "Empty"){
            return "+"
        }
        return if (name.isNotEmpty()) {
            val words = name.split(" ")
            val initials = StringBuilder()

            for (word in words) {
                if (word.isNotBlank()) {
                    initials.append(word[0].uppercaseChar())
                }
            }

            initials.toString()
        } else {
            ""
        }
    }
}