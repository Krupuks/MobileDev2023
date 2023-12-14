package com.example.mobiledev2023.ui.builders

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.navigation.NavOptions
import androidx.navigation.Navigation.findNavController
import com.example.mobiledev2023.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class CourtBookingBuilder(private val context: Context, private val db: FirebaseFirestore) {

    private var selectedOptionTextView: TextView? = null
    private var selectedCourt: String = ""
    private var selectedTime: String = ""
    private var timeSlots = mutableListOf<String>()
    private lateinit var cardContainer: LinearLayout
    private lateinit var courtSpinner: Spinner
    private lateinit var datePicker: DatePicker

    fun buildCard(
        title: String,
        context: Context,
        fragmentView: View
    ): View {
        val auth = FirebaseAuth.getInstance()
        val userID = auth.currentUser?.uid
        val cardLayout = LayoutInflater.from(context).inflate(R.layout.card, null)
        cardContainer = cardLayout.findViewById(R.id.card_container)
        val titleTextView = cardContainer.findViewById<TextView>(R.id.text_card)
        titleTextView.text = title

        courtSpinner = Spinner(context)
        datePicker = DatePicker(context)

        datePicker.init(
            datePicker.year,
            datePicker.month,
            datePicker.dayOfMonth
        ) { view, year, monthOfYear, dayOfMonth ->
            // Fetch time slots for the selected court and the new date
            fetchTimeSlotsForSelectedCourt(selectedCourt, datePicker) { fetchedTimeSlots ->
                timeSlots.clear()
                timeSlots.addAll(fetchedTimeSlots)
                if (userID != null) {
                    rebuildUI(userID, fragmentView)
                }
            }
        }
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1) // Set the minimum date to tomorrow

        val minDate = calendar.timeInMillis
        datePicker.minDate = minDate

        var tileOptions = mapOf(
            "Select a Court" to courtSpinner,
            "Select a Date" to datePicker
        )
        for ((tileTitle, tileView) in tileOptions) {
            val tileContent = LayoutInflater.from(context).inflate(R.layout.tile, null)
            val tileTitleTextView = tileContent.findViewById<TextView>(R.id.text_tile_title)
            tileTitleTextView.text = tileTitle

            val tileRow = tileContent.findViewById<LinearLayout>(R.id.tile_row)
            tileRow.orientation = LinearLayout.VERTICAL

            when (tileView) {
                is View -> tileRow.addView(tileView)
                is List<*> -> {
                    for (option in tileView) {
                        if (option is String) {
                            val optionTextView = TextView(context)
                            optionTextView.text = option
                            optionTextView.setPadding(15, 15, 15, 15)
                            optionTextView.setTextColor(ContextCompat.getColor(context, R.color.teal_900))
                            optionTextView.gravity = Gravity.CENTER
                            optionTextView.setOnClickListener {
                                handleOptionClick(optionTextView)
                            }
                            tileRow.addView(optionTextView)

                            val layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            layoutParams.setMargins(0, 10, 0, 10)
                            optionTextView.layoutParams = layoutParams
                        }
                    }
                }
            }

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(30, 30, 30, 30)
            tileContent.layoutParams = layoutParams

            cardContainer.addView(tileContent)
        }

        db.collection("courts")
            .get()
            .addOnSuccessListener { documents ->
                val courtNames = mutableListOf<String>()

                for (document in documents) {
                    val courtName = document.getString("name")
                    courtName?.let {
                        courtNames.add(courtName)
                    }
                }

                val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, courtNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                courtSpinner.adapter = adapter

                courtSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        selectedCourt = courtSpinner.selectedItem?.toString() ?: ""

                        fetchTimeSlotsForSelectedCourt(selectedCourt,datePicker) { fetchedTimeSlots ->
                            timeSlots.clear()
                            timeSlots.addAll(fetchedTimeSlots)
                            if (userID != null) {
                                rebuildUI(userID, fragmentView)
                            }
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        // Handle case when nothing is selected
                    }
                }
            }
        return cardLayout
    }

    private fun rebuildUI(userID: String, fragmentView: View) {
        // Find the view representing the time slots
        val timeSlotsView = cardContainer.findViewWithTag<View>("timeSlotsView")

        // Remove the time slots view if it exists
        timeSlotsView?.let {
            cardContainer.removeView(timeSlotsView)
        }

        // Create a new view to represent the updated time slots
        val timeSlotsContent = LayoutInflater.from(context).inflate(R.layout.tile, null)
        timeSlotsContent.tag = "timeSlotsView" // Set a tag to find this view later if needed

        val timeSlotsRow = timeSlotsContent.findViewById<LinearLayout>(R.id.tile_row)
        timeSlotsRow.orientation = LinearLayout.VERTICAL

        // Set the tile title for time slots
        val timeSlotsTitleTextView = timeSlotsContent.findViewById<TextView>(R.id.text_tile_title)
        timeSlotsTitleTextView.text = "Select a Time Slot"

        // Logic to create the updated time slots view based on your data
        for (option in timeSlots) {
            val optionTextView = TextView(context)
            optionTextView.text = option
            optionTextView.setPadding(15, 15, 15, 15)
            optionTextView.setTextColor(ContextCompat.getColor(context, R.color.teal_900))
            optionTextView.gravity = Gravity.CENTER
            optionTextView.setOnClickListener {
                handleOptionClick(optionTextView)
            }
            timeSlotsRow.addView(optionTextView)

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 10, 0, 10)
            optionTextView.layoutParams = layoutParams
        }

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(30, 30, 30, 30)
        timeSlotsContent.layoutParams = layoutParams

        // Add the updated time slots view to the card container
        cardContainer.addView(timeSlotsContent)

        // Remove the existing book button if it exists
        val existingBookButton = cardContainer.findViewWithTag<Button>("bookButton")
        existingBookButton?.let {
            cardContainer.removeView(existingBookButton)
        }

        // Add a new bookButton at the end
        val newBookButton = Button(context)
        newBookButton.text = "Book"
        newBookButton.tag = "bookButton"

        newBookButton.setOnClickListener {
            if (selectedTime.isEmpty()) {
                Toast.makeText(context, "Please select a time slot", Toast.LENGTH_SHORT).show()
            }
            else{
                val selectedDate = getSelectedDateFromDatePicker(datePicker)
                bookSelectedTimeSlot(userID ,selectedCourt, selectedDate, selectedTime)

                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.navigation_court, true) // This will clear the back stack up to home
                    .build()
                // Trigger navigation to the DashboardFragment
                findNavController(fragmentView).navigate(R.id.action_court_to_match, null, navOptions)
            }
        }
        cardContainer.addView(newBookButton)
    }


    private fun getSelectedDateFromDatePicker(datePicker: DatePicker): String {
        val year = datePicker.year
        val month = datePicker.month + 1 // Month starts from 0, so add 1 to match the conventional month numbering
        val dayOfMonth = datePicker.dayOfMonth

        // Format the date as needed (e.g., "YYYY-MM-DD")
        return "$year-${String.format("%02d", month)}-${String.format("%02d", dayOfMonth)}"
    }

    private fun handleOptionClick(textView: TextView) {
        selectedOptionTextView?.setBackgroundResource(android.R.color.transparent)
        selectedOptionTextView = textView
        textView.setBackgroundResource(R.drawable.rounded_bg3)

        selectedTime = textView.text.toString()
    }

    private fun fetchTimeSlotsForSelectedCourt(selectedCourtName: String, datePicker: DatePicker, onTimeSlotsFetched: (List<String>) -> Unit) {
        // Get the selected date from the date picker
        val selectedDate = getSelectedDateFromDatePicker(datePicker)
        Log.d("SELECTED DATE", selectedDate)

        val reservedSlots = mutableListOf<String>()
        db.collection("reservations")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val reservationCourtID = document.getString("courtID")
                    val time = document.getString("time")
                    // Retrieve the court document from the courts collection based on courtID
                    if (reservationCourtID != null) {
                        db.collection("courts")
                            .document(reservationCourtID)
                            .get()
                            .addOnSuccessListener { courtDocument ->
                                val courtName = courtDocument.getString("name")

                                Log.d("time", time.toString())
                                Log.d("selectedDate", selectedDate.toString())

                                if (courtName != null && courtName == selectedCourtName && time != null && time.startsWith(selectedDate)) {
                                    reservedSlots.add(time)
                                }
                                Log.d("1RESERVATIONS ON DAY " + selectedDate, reservedSlots.toString())
                                db.collection("courts")
                                    .whereEqualTo("name", selectedCourtName)
                                    .get()
                                    .addOnSuccessListener { documents ->
                                        val timeSlots = mutableListOf<String>()
                                        for (document in documents) {

                                            //this has to stay the same
                                            val slots = document.get("time_slots") as? List<String>
                                            Log.d("ALL SLOTS", slots.toString())

                                            val selectedReservedSlots = mutableListOf<String>()
                                            if (reservedSlots != null) {
                                                for (reservedSlot in reservedSlots) {
                                                    if (reservedSlot.startsWith(selectedDate)) {
                                                        selectedReservedSlots.add(reservedSlot.split(" ")[1])
                                                    }
                                                }
                                            }
                                            Log.d("BOOKED SLOTS", selectedReservedSlots.toString())

                                            // Remove elements that occur in selectedReservedSlots from slots
                                            if (slots != null) {
                                                val mutableSlots = slots.toMutableList()
                                                mutableSlots.removeAll(selectedReservedSlots)
                                                timeSlots.addAll(mutableSlots)
                                                Log.d("AVAILABLE SLOTS", mutableSlots.toString())
                                            }
                                        }

                                        onTimeSlotsFetched(timeSlots)
                                    }
                            }
                    }
                }
            }

        db.collection("courts")
            .whereEqualTo("name", selectedCourtName)
            .get()
            .addOnSuccessListener { documents ->
                val timeSlots = mutableListOf<String>()
                for (document in documents) {

                    //this has to stay the same
                    val slots = document.get("time_slots") as? List<String>
                    Log.d("ALL SLOTS", slots.toString())

                    val selectedReservedSlots = mutableListOf<String>()
                    Log.d("2RESERVATIONS ON DAY " + selectedDate, reservedSlots.toString())
                    if (reservedSlots != null) {
                        for (reservedSlot in reservedSlots) {
                            if (reservedSlot.startsWith(selectedDate)) {
                                selectedReservedSlots.add(reservedSlot.split(" ")[1])
                            }
                        }
                    }
                    Log.d("BOOKED SLOTS", selectedReservedSlots.toString())

                    // Remove elements that occur in selectedReservedSlots from slots
                    if (slots != null) {
                        val mutableSlots = slots.toMutableList()
                        mutableSlots.removeAll(selectedReservedSlots)
                        timeSlots.addAll(mutableSlots)
                        Log.d("AVAILABLE SLOTS", mutableSlots.toString())
                    }
                }

                onTimeSlotsFetched(timeSlots)
            }
    }

    private fun bookSelectedTimeSlot(userID: String, selectedCourtName: String, selectedDate: String, selectedTime: String) {
        if (selectedTime.isEmpty()) {
            Toast.makeText(context, "Please select a time slot", Toast.LENGTH_SHORT).show()
            return // Do not proceed with the booking if no time slot is selected
        }

        val selectedDateTimeString = "$selectedDate $selectedTime"
        // Query the courts collection to get the court document
        db.collection("courts")
            .whereEqualTo("name", selectedCourtName)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val courtID = document.id

                    // Create a reservation document in the reservations collection
                    val reservationData = hashMapOf(
                        "userID" to userID,
                        "courtID" to courtID,
                        "time" to selectedDateTimeString,
                        "matchID" to ""
                    )

                    db.collection("reservations")
                        .add(reservationData)
                        .addOnSuccessListener { reservationDocRef ->
                            Log.d("Firestore", "Reservation document added with ID: ${reservationDocRef.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error adding reservation document", e)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents: ", exception)
            }
    }
}
