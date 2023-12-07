package com.example.mobiledev2023.presentation

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.example.mobiledev2023.R
import com.google.firebase.firestore.FirebaseFirestore

class CardBuilder2(private val context: Context, private val db: FirebaseFirestore) {

    private var selectedOptionTextView: TextView? = null
    private var selectedCourt: String = ""
    private var selectedTime: String = ""
    private var timeSlots = mutableListOf<String>()
    private lateinit var cardContainer: LinearLayout
    private lateinit var courtSpinner: Spinner
    private lateinit var datePicker: DatePicker

    fun buildCard2(
        title: String,
        context: Context
    ): View {
        val cardLayout = LayoutInflater.from(context).inflate(R.layout.card, null)
        cardContainer = cardLayout.findViewById(R.id.card_container)
        val titleTextView = cardContainer.findViewById<TextView>(R.id.text_card)
        titleTextView.text = title

        courtSpinner = Spinner(context)
        datePicker = DatePicker(context)

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

                        fetchTimeSlotsForSelectedCourt(selectedCourt) { fetchedTimeSlots ->
                            timeSlots.clear()
                            timeSlots.addAll(fetchedTimeSlots)
                            rebuildUI()
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        // Handle case when nothing is selected
                    }
                }
            }
        return cardLayout
    }

    private fun rebuildUI() {
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
            val selectedDate = getSelectedDateFromDatePicker(datePicker)
            bookSelectedTimeSlot(selectedCourt, selectedDate, selectedTime)
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

    private fun fetchTimeSlotsForSelectedCourt(selectedCourtName: String, onTimeSlotsFetched: (List<String>) -> Unit) {
        db.collection("courts")
            .whereEqualTo("name", selectedCourtName)
            .get()
            .addOnSuccessListener { documents ->
                val timeSlots = mutableListOf<String>()

                for (document in documents) {
                    val slots = document.get("time_slots") as? List<*>
                    slots?.forEach { slot ->
                        val timeSlot = slot as? String
                        timeSlot?.let { timeSlots.add(it) }
                    }
                }
                onTimeSlotsFetched(timeSlots)
            }
    }

    private fun bookSelectedTimeSlot(selectedCourtName: String, selectedDate: String, selectedTime: String) {
        val selectedDateTimeString = "$selectedDate $selectedTime"

        db.collection("courts")
            .whereEqualTo("name", selectedCourtName)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val reservedTimeSlots = document.get("reserved_time_slots")

                    if (reservedTimeSlots is ArrayList<*>) {
                        val reservedTimeSlotsString = reservedTimeSlots.filterIsInstance<String>() as ArrayList<String>
                        reservedTimeSlotsString.add(selectedDateTimeString)

                        document.reference.update("reserved_time_slots", reservedTimeSlotsString)
                            .addOnSuccessListener {
                                Log.d("Firestore", "DateTime added to reserved_time_slots successfully.")
                            }
                            .addOnFailureListener { e ->
                                Log.w("Firestore", "Error adding datetime to reserved_time_slots", e)
                            }
                    } else {
                        val newReservedTimeSlots = ArrayList<String>()
                        newReservedTimeSlots.add(selectedDateTimeString)

                        document.reference.update("reserved_time_slots", newReservedTimeSlots)
                            .addOnSuccessListener {
                                Log.d("Firestore", "DateTime added to reserved_time_slots successfully.")
                            }
                            .addOnFailureListener { e ->
                                Log.w("Firestore", "Error adding datetime to reserved_time_slots", e)
                            }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents: ", exception)
            }
    }
}
