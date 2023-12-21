package com.example.mobiledev2023.ui.builders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import com.example.mobiledev2023.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DateFormatSymbols

class MyReservationsBuilder(private val context: Context, private val db: FirebaseFirestore) {

    private lateinit var cardContainer: LinearLayout
    private var courtSpinner: Spinner = Spinner(context)
    private var reservations = mutableListOf<Pair<String, String>>() // Pair of reservationInfo and reservationID
    private var selectedReservationID: String = ""

    fun buildCard(
        title: String,
        context: Context
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
        tileTitleTextView.visibility = View.GONE

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

        fetchUserReservations(currentUserID, tileContent)
        return cardLayout
    }


    private fun fetchUserReservations(currentUserID: String?, tileContent: View) {
        db.collection("reservations")
            .whereEqualTo("userID", currentUserID)
            .whereEqualTo("matchID", "")
            .get()
            .addOnSuccessListener { documents ->
                reservations.clear()

                if (documents.isEmpty) {
                    val emptyTextView = TextView(context)
                    emptyTextView.text = "Looks a bit empty in here."
                    emptyTextView.textSize = 15f
                    emptyTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    emptyTextView.setPadding(60, 0, 60, 60)
                    cardContainer.addView(emptyTextView)

                    // Hide the entire tile when there are no reservations
                    tileContent.visibility = View.GONE
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

}

