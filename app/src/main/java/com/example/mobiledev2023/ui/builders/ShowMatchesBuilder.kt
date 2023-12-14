package com.example.mobiledev2023.ui.builders

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.mobiledev2023.R
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DateFormatSymbols

data class Match(
    val reservationID: String,
    val teamAPlayer1: String,
    val teamAPlayer2: String,
    val teamBPlayer1: String,
    val teamBPlayer2: String
)

class ShowMatchesBuilder(private val context: Context, private val db: FirebaseFirestore) {

    fun buildCard(title: String): View {
        val cardLayout = LayoutInflater.from(context).inflate(R.layout.card, null)
        val titleTextView = cardLayout.findViewById<TextView>(R.id.text_card)
        titleTextView.text = title
        val cardContainer = cardLayout.findViewById<LinearLayout>(R.id.card_container)

        fetchMatchesFromFirestore { matches ->
            matches.forEach { match ->
                val matchContainer = LinearLayout(context)
                matchContainer.orientation = LinearLayout.VERTICAL

                var isMatchTile = true
                var currentView: View? = null

                fun toggleViews() {
                    isMatchTile = !isMatchTile
                    matchContainer.removeAllViews()
                    currentView = if (isMatchTile) {
                        createMatchTileView(match)
                    } else {
                        createTileView(match)
                    }
                    matchContainer.addView(currentView)

                    currentView?.setOnClickListener {
                        toggleViews()
                    }
                }

                toggleViews()

                currentView?.setOnClickListener {
                    toggleViews()
                }

                cardContainer.addView(matchContainer)
            }
        }

        return cardLayout
    }

    private fun createMatchTileView(match: Match): View {
        val inflater = LayoutInflater.from(context)
        val matchTileContent = inflater.inflate(R.layout.match_tile, null)
        val tileTitleTextView = matchTileContent.findViewById<TextView>(R.id.text_tile_title)
        val tileTimeTextView = matchTileContent.findViewById<TextView>(R.id.text_time)
        val playerA1TextView = matchTileContent.findViewById<TextView>(R.id.text_player_a_1)
        val playerA2TextView = matchTileContent.findViewById<TextView>(R.id.text_player_a_2)
        val playerB1TextView = matchTileContent.findViewById<TextView>(R.id.text_player_b_1)
        val playerB2TextView = matchTileContent.findViewById<TextView>(R.id.text_player_b_2)

        fetchCourtAndUserDetails(match) { courtName, teamAPlayer1Name, teamAPlayer2Name, teamBPlayer1Name, teamBPlayer2Name ->
            // Use the court and player details as needed
            tileTitleTextView.text = courtName
            playerA1TextView.text = teamAPlayer1Name
            playerA2TextView.text = teamAPlayer2Name
            playerB1TextView.text = teamBPlayer1Name
            playerB2TextView.text = teamBPlayer2Name

            fetchDateTime(match.reservationID) { dateTime ->
                val formattedDateTime = formatDateTime(dateTime)
                if (formattedDateTime.isNotEmpty()) {
                    // Set the formatted date and time into the appropriate TextView
                    tileTimeTextView.text = formattedDateTime
                }
            }
        }

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(30, 30, 30, 30) // Adjust margin values as needed
        matchTileContent.layoutParams = layoutParams

        return matchTileContent
    }

    private fun fetchCourtAndUserDetails(
        match: Match,
        onDataFetched: (String, String, String, String, String) -> Unit
    ) {
        val reservationID = match.reservationID

        db.collection("reservations").document(reservationID)
            .get()
            .addOnSuccessListener { reservationDocument ->
                val courtID = reservationDocument.getString("courtID")
                if (courtID != null) {
                    db.collection("courts").document(courtID)
                        .get()
                        .addOnSuccessListener { courtDocument ->
                            val courtName = courtDocument.getString("name") ?: ""

                            fetchUserName(match.teamAPlayer1) { teamAPlayer1Name ->
                                fetchUserName(match.teamAPlayer2) { teamAPlayer2Name ->
                                    fetchUserName(match.teamBPlayer1) { teamBPlayer1Name ->
                                        fetchUserName(match.teamBPlayer2) { teamBPlayer2Name ->
                                            onDataFetched(
                                                courtName,
                                                teamAPlayer1Name,
                                                teamAPlayer2Name,
                                                teamBPlayer1Name,
                                                teamBPlayer2Name
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("FetchCourtError", "Error fetching court: $exception")
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FetchReservationError", "Error fetching reservation: $exception")
            }
    }

    private fun fetchUserName(userID: String?, onUserNameFetched: (String) -> Unit) {
        if (!userID.isNullOrEmpty()) {
            db.collection("users").document(userID)
                .get()
                .addOnSuccessListener { userDocument ->
                    val firstName = userDocument.getString("first_name") ?: ""
                    val lastName = userDocument.getString("last_name") ?: ""
                    val userName = if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                        "$firstName $lastName"
                    } else {
                        "" // Set name to "undefined" if either first or last name is empty
                    }
                    onUserNameFetched(userName)
                }
                .addOnFailureListener {
                    onUserNameFetched("") // Set name to "undefined" on failure to fetch
                }
        } else {
            onUserNameFetched("Empty") // Set name to "undefined" if userID is null or empty
        }
    }

    private fun createTileView(match: Match): View {
        val inflater = LayoutInflater.from(context)
        val tileContent = inflater.inflate(R.layout.tile, null)
        val tileTitleTextView = tileContent.findViewById<TextView>(R.id.text_tile_title)

        fetchCourtAndUserDetails(match) { courtName, _, _, _, _ ->
            fetchDateTime(match.reservationID) { dateTime ->
                val formattedDateTime = formatDateTime(dateTime)
                if (formattedDateTime.isNotEmpty()) {
                    // Set the formatted date and time into the appropriate TextView
                    tileTitleTextView.text = "$courtName - $formattedDateTime"
                }
            }
        }

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(30, 30, 30, 30)
        tileContent.layoutParams = layoutParams


        return tileContent
    }

    private fun fetchMatchesFromFirestore(onMatchesFetched: (List<Match>) -> Unit) {
        val matches = mutableListOf<Match>()

        db.collection("matches")
            .get()
            .addOnSuccessListener { documents ->
                val matchFetchTasks = mutableListOf<Task<Match>>()

                for (document in documents) {
                    val task = TaskCompletionSource<Match>()
                    constructMatchFromDocument(document) { match ->
                        matches.add(match)
                        task.setResult(match)
                    }
                    matchFetchTasks.add(task.task)
                }

                Tasks.whenAllComplete(matchFetchTasks)
                    .addOnSuccessListener {
                        onMatchesFetched(matches)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FetchMatchesError", "Error fetching matches: $exception")
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("FetchMatchesError", "Error fetching matches: $exception")
            }
    }

    private fun constructMatchFromDocument(
        document: DocumentSnapshot,
        onMatchFetched: (Match) -> Unit
    ) {
        val reservationID = document.getString("reservationID")
        val teamAPlayer1ID = document.getString("team_a_1")
        val teamAPlayer2ID = document.getString("team_a_2")
        val teamBPlayer1ID = document.getString("team_b_1")
        val teamBPlayer2ID = document.getString("team_b_2")

        val match = Match(
            reservationID = reservationID ?: "",
            teamAPlayer1 = teamAPlayer1ID ?: "",
            teamAPlayer2 = teamAPlayer2ID ?: "",
            teamBPlayer1 = teamBPlayer1ID ?: "",
            teamBPlayer2 = teamBPlayer2ID ?: ""
        )

        onMatchFetched(match)
    }

    private fun fetchDateTime(reservationID: String, onDateTimeFetched: (String) -> Unit) {
        db.collection("reservations").document(reservationID)
            .get()
            .addOnSuccessListener { reservationDocument ->
                val dateTime = reservationDocument.getString("time") ?: ""
                onDateTimeFetched(dateTime)
            }
            .addOnFailureListener { exception ->
                Log.e("FetchDateTimeError", "Error fetching date and time: $exception")
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

}
