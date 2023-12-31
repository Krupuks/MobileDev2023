package com.example.mobiledev2023.ui.builders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import com.example.mobiledev2023.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DateFormatSymbols

class MatchCreatingBuilder(private val context: Context, private val db: FirebaseFirestore) {

    private lateinit var cardContainer: LinearLayout
    private var courtSpinner: Spinner = Spinner(context)
    private var reservations = mutableListOf<Pair<String, String>>() // Pair of reservationInfo and reservationID
    private var selectedReservationID: String = ""

    fun buildCard(
        title: String,
        context: Context,
        fragmentView: View
    ): View {
        val auth = FirebaseAuth.getInstance()
        val currentUserID = auth.currentUser?.uid

        val cardLayout = LayoutInflater.from(context).inflate(R.layout.card, null)
        cardContainer = cardLayout.findViewById(R.id.card_container)
        val titleTextView = cardContainer.findViewById<TextView>(R.id.text_card)
        titleTextView.text = title


        val layoutParamsCard = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParamsCard.setMargins(0, 30, 0, 0)
        cardLayout.layoutParams = layoutParamsCard

        val tileContent = LayoutInflater.from(context).inflate(R.layout.tile, null)
        val tileTitleTextView = tileContent.findViewById<TextView>(R.id.text_tile_title)
        tileTitleTextView.text = "Select one of your Court Reservations"

        val tileRow = tileContent.findViewById<LinearLayout>(R.id.tile_row)
        tileRow.orientation = LinearLayout.VERTICAL
        tileRow.addView(courtSpinner)

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(30, 30, 30, 30)
        tileContent.layoutParams = layoutParams
        cardContainer.addView(tileContent)

        val newCreateButton = Button(context)
        newCreateButton.text = "Create"
        newCreateButton.tag = "createButton"
        newCreateButton.setBackgroundResource(R.drawable.rounded_bg3)

        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        buttonParams.setMargins(30, 30, 30, 30)
        newCreateButton.layoutParams = buttonParams

        newCreateButton.setOnClickListener {
            if (selectedReservationID.isNotEmpty()) {
                createMatch(currentUserID, selectedReservationID, fragmentView)
            } else {
                // Handle case when no court is selected
                Toast.makeText(context, "Please select a court", Toast.LENGTH_SHORT).show()
            }
        }
        cardContainer.addView(newCreateButton)

        fetchUserReservations(currentUserID, newCreateButton)
        return cardLayout
    }

    private fun fetchUserReservations(currentUserID: String?, newCreateButton: Button) {
        db.collection("reservations")
            .whereEqualTo("userID", currentUserID)
            .whereEqualTo("matchID", "")
            .get()
            .addOnSuccessListener { documents ->
                reservations.clear()

                if (documents.isEmpty) {
                    // If there are no reservations, display the empty text
                    val emptyTextView = TextView(context)
                    emptyTextView.text = "Looks like you don't have any available reservations for your new match"
                    emptyTextView.textSize = 15f
                    emptyTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    emptyTextView.setPadding(60, 0, 60, 60)
                    cardContainer.addView(emptyTextView)

                    // Hide the dropdown menu and button
                    courtSpinner.visibility = View.GONE
                    newCreateButton.visibility = View.GONE
                } else {
                    for (document in documents) {
                        val courtID = document.getString("courtID") ?: ""
                        val dateTimeString = document.getString("time") ?: ""
                        val reservationID = document.id // Retrieve the reservation ID
                        val matchID = document.getString("matchID") ?: ""

                        db.collection("courts")
                            .document(courtID)
                            .get()
                            .addOnSuccessListener { courtDocument ->
                                val courtName = courtDocument.getString("name") ?: ""
                                val formattedDateTime = formatDateTime(dateTimeString)
                                val reservationInfo = "$courtName - $formattedDateTime"
                                reservations.add(reservationInfo to reservationID)

                                populateDropdown()
                            }
                    }
                }
            }
    }



    private fun formatDate(dateString: String): String {
        val parts = dateString.split("-")
        if (parts.size == 3) {
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()

            val monthString = DateFormatSymbols().months[month - 1].substring(0, 3) // Get abbreviated month name

            return "$day $monthString $year"
        }
        return ""
    }

    private fun formatDateTime(dateTimeString: String): String {
        // Split the date and time, consider both parts for display
        val parts = dateTimeString.split(" ")
        if (parts.size == 2) {
            val dateString = formatDate(parts[0]) // Format date
            val timeString = parts[1] // Extract time

            return "$dateString $timeString"
        }
        return ""
    }

    private fun populateDropdown() {
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, reservations.map { it.first })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        courtSpinner.adapter = adapter

        courtSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Get the selected reservation's ID from reservations list
                selectedReservationID = reservations[position].second
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle case when nothing is selected
            }
        }
    }

    private fun createMatch(currentUserID: String?, selectedReservationID: String, fragmentView: View) {
        val matchesCollection = db.collection("matches")
        val newMatch = hashMapOf(
            "reservationID" to selectedReservationID,
            "team_a_1" to currentUserID,
            "team_a_2" to "",
            "team_b_1" to "",
            "team_b_2" to "",
            // Add other match-related data as needed
        )

        matchesCollection.add(newMatch)
            .addOnSuccessListener { documentReference ->
                val matchID = documentReference.id
                println("Match created with ID: $matchID")
                updateReservationWithMatchID(selectedReservationID, matchID)
            }
            .addOnFailureListener { e ->
                println("Error creating match: $e")
            }

        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.navigation_match, true) // This will clear the back stack up to home
            .build()
        // Trigger navigation to the DashboardFragment
        Navigation.findNavController(fragmentView)
            .navigate(R.id.action_match_to_explore, null, navOptions)
    }

    private fun updateReservationWithMatchID(reservationID: String, matchID: String) {
        db.collection("reservations")
            .document(reservationID)
            .update("matchID", matchID)
            .addOnSuccessListener {
                println("Reservation updated with match ID")
            }
            .addOnFailureListener { e ->
                println("Error updating reservation: $e")
            }
    }
}

