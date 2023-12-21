package com.example.mobiledev2023.ui.builders

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.mobiledev2023.R
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DateFormatSymbols

object FirestoreConstants {
    const val COLLECTION_MATCHES = "matches"
    const val COLLECTION_RESERVATIONS = "reservations"
    // ... other constants
}

data class Match(
    var documentID: String, // New property for storing the Firestore document ID
    var reservationID: String,
    var teamAPlayer1: String,
    var teamAPlayer2: String,
    var teamBPlayer1: String,
    var teamBPlayer2: String
)

class ShowMatchesBuilder(private val context: Context, private val db: FirebaseFirestore) {

    companion object {
        private const val MARGIN_VALUE = 30
    }

    fun buildCard(title: String, filteredOnUser: Boolean): View {
        val auth = FirebaseAuth.getInstance()
        val userID = auth.currentUser?.uid
        val cardLayout = LayoutInflater.from(context).inflate(R.layout.card, null)
        val titleTextView = cardLayout.findViewById<TextView>(R.id.text_card)
        titleTextView.text = title

        val layoutParamsCard = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParamsCard.setMargins(0, 30, 0, 0)
        cardLayout.layoutParams = layoutParamsCard

        val cardContainer = cardLayout.findViewById<LinearLayout>(R.id.card_container)

        fetchMatchesFromFirestore { matches ->
            val filteredMatches = if (filteredOnUser) {
                matches.filter { match ->
                    match.teamAPlayer1 == userID ||
                            match.teamAPlayer2 == userID ||
                            match.teamBPlayer1 == userID ||
                            match.teamBPlayer2 == userID
                }
            } else {
                matches
            }
            if (filteredMatches.isEmpty()) {
                val emptyTextView = TextView(context)
                emptyTextView.text = "Looks a bit empty in here"
                emptyTextView.textSize = 15f
                emptyTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                emptyTextView.setPadding(60, 0, 60, 60)
                cardContainer.addView(emptyTextView)
            } else {
                filteredMatches.forEach { match ->
                    val matchContainer = LinearLayout(context)
                    matchContainer.orientation = LinearLayout.VERTICAL

                    var isMatchTile = true
                    var currentView: View? = null

                    fun toggleViews() {
                        isMatchTile = !isMatchTile
                        matchContainer.removeAllViews()
                        currentView = if (isMatchTile) {
                            createMatchTileView(match, userID)
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
        }
        return cardLayout
    }


    private fun createMatchTileView(match: Match, userID: String?): View {
        val inflater = LayoutInflater.from(context)
        val matchTileContent = inflater.inflate(R.layout.match_tile, null)
        val tileTitleTextView = matchTileContent.findViewById<TextView>(R.id.text_tile_title)
        val tileTimeTextView = matchTileContent.findViewById<TextView>(R.id.text_time)
        val playerA1TextView = matchTileContent.findViewById<TextView>(R.id.text_player_a_1)
        val playerA2TextView = matchTileContent.findViewById<TextView>(R.id.text_player_a_2)
        val playerB1TextView = matchTileContent.findViewById<TextView>(R.id.text_player_b_1)
        val playerB2TextView = matchTileContent.findViewById<TextView>(R.id.text_player_b_2)
        val playerA1InitialsTextView = matchTileContent.findViewById<TextView>(R.id.text_icon_a_1)
        val playerA2InitialsTextView = matchTileContent.findViewById<TextView>(R.id.text_icon_a_2)
        val playerB1InitialsTextView = matchTileContent.findViewById<TextView>(R.id.text_icon_b_1)
        val playerB2InitialsTextView = matchTileContent.findViewById<TextView>(R.id.text_icon_b_2)

        setPlayerClickListener(playerA1TextView, playerA1InitialsTextView, "team_a_1", match, userID)
        setPlayerClickListener(playerA2TextView, playerA2InitialsTextView, "team_a_2", match, userID)
        setPlayerClickListener(playerB1TextView, playerB1InitialsTextView, "team_b_1", match, userID)
        setPlayerClickListener(playerB2TextView, playerB2InitialsTextView, "team_b_2", match, userID)

        fetchCourtAndUserDetails(match) { courtName, teamAPlayer1Name, teamAPlayer2Name, teamBPlayer1Name, teamBPlayer2Name ->
            tileTitleTextView.text = courtName
            playerA1TextView.text = teamAPlayer1Name
            playerA2TextView.text = teamAPlayer2Name
            playerB1TextView.text = teamBPlayer1Name
            playerB2TextView.text = teamBPlayer2Name

            playerA1InitialsTextView.text = getInitials(teamAPlayer1Name)
            playerA2InitialsTextView.text = getInitials(teamAPlayer2Name)
            playerB1InitialsTextView.text = getInitials(teamBPlayer1Name)
            playerB2InitialsTextView.text = getInitials(teamBPlayer2Name)

            fetchDateTime(match.reservationID) { dateTime ->
                val formattedDateTime = formatDateTime(dateTime)
                if (formattedDateTime.isNotEmpty()) {
                    tileTimeTextView.text = formattedDateTime
                }
            }
        }

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(MARGIN_VALUE, MARGIN_VALUE, MARGIN_VALUE, MARGIN_VALUE)
        matchTileContent.layoutParams = layoutParams

        return matchTileContent
    }

    private fun setPlayerClickListener(
        textView: TextView,
        initialsTextView: TextView,
        fieldName: String,
        match: Match,
        userID: String?
    ) {
        initialsTextView.setOnClickListener {
            val teamAPlayer1 = match.teamAPlayer1
            val teamAPlayer2 = match.teamAPlayer2
            val teamBPlayer1 = match.teamBPlayer1
            val teamBPlayer2 = match.teamBPlayer2

            fetchUserName(userID) { userName ->
                val currentUserName = userName

                val playersList = listOf(teamAPlayer1, teamAPlayer2, teamBPlayer1, teamBPlayer2)

                Log.d("JOINABLE", playersList.toString())

                if (playersList.contains(userID)) {
                    // User is already in this match
                    Toast.makeText(
                        context,
                        "You've already joined this match.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // User is not in this match, continue with the code
                    Log.d("JOINABLE", "CLICKED")
                    if (textView.text.toString() == "Empty") {
                        Log.d("JOINABLE", "JOINABLE")
                        updateFirestore(match.documentID, fieldName, userID)
                        updateMatch(match, fieldName, userID)

                        textView.text = currentUserName
                        initialsTextView.text = getInitials(currentUserName)

                        Log.d("UserIDDebug", "UserID: $userID") // Log the userID here
                    } else {
                        Log.d("JOINABLE", "NOT JOINABLE")
                    }
                }
            }
        }
    }



    private fun updateFirestore(id: String, fieldToUpdate: String, value: String?) {
        val matchesCollection = db.collection(FirestoreConstants.COLLECTION_MATCHES)
        matchesCollection.document(id)
            .update(fieldToUpdate, value)
            .addOnFailureListener { exception ->
                handleFirestoreError("UpdateFirestoreError", exception)
            }
    }
    fun updateMatch(match: Match, fieldName: String, userID: String?) {
        Log.d("MatchUpdate", "Before update: $match")
        Log.d("MatchUpdate",fieldName)
        when (fieldName) {
            "team_a_1" -> match.teamAPlayer1 = userID ?: ""
            "team_a_2" -> match.teamAPlayer2 = userID ?: ""
            "team_b_1" -> match.teamBPlayer1 = userID ?: ""
            "team_b_2" -> match.teamBPlayer2 = userID ?: ""
        }
        Log.d("MatchUpdate", "After update: $match")
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
        val documentID = document.id // Get the Firestore document ID
        val reservationID = document.getString("reservationID")
        val teamAPlayer1ID = document.getString("team_a_1")
        val teamAPlayer2ID = document.getString("team_a_2")
        val teamBPlayer1ID = document.getString("team_b_1")
        val teamBPlayer2ID = document.getString("team_b_2")

        val match = Match(
            documentID = documentID,
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

    private fun handleFirestoreError(tag: String, exception: Exception) {
        Log.e(tag, "Error performing Firestore operation: $exception")
        // Handle the error globally or as needed
    }

}
