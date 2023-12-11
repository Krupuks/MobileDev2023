// CardBuilder.kt
package com.example.mobiledev2023.ui.builders

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.mobiledev2023.R
import com.google.firebase.firestore.FirebaseFirestore

class PreferenceBuilder(private val context: Context) {

    private var selectedOptionsMap: MutableMap<String, TextView?> = mutableMapOf()

    fun buildCard(
        title: String,
        tileOptions: Map<String, List<String>>,
        currentUserUID: String?,
        userPreferences: MutableMap<String, Any>
    ): View {
        val cardLayout = LayoutInflater.from(context).inflate(R.layout.card, null)
        val titleTextView = cardLayout.findViewById<TextView>(R.id.text_card)
        titleTextView.text = title

        val cardContainer = cardLayout.findViewById<LinearLayout>(R.id.card_container)

        for ((tileTitle, options) in tileOptions) {
            val transformedTileTitle = tileTitle.replace(" ", "_").lowercase() // Transform title

            val inflater = LayoutInflater.from(context)
            val tileContent = inflater.inflate(R.layout.tile, null)
            val tileTitleTextView = tileContent.findViewById<TextView>(R.id.text_tile_title)
            tileTitleTextView.text = tileTitle

            val tileRow = tileContent.findViewById<LinearLayout>(R.id.tile_row)
            for (option in options) {
                val optionTextView = TextView(context)
                optionTextView.text = option
                optionTextView.setPadding(15, 15, 15, 15)
                optionTextView.setTextColor(ContextCompat.getColor(context, R.color.teal_900))
                optionTextView.gravity = Gravity.CENTER
                optionTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.toFloat())
                optionTextView.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                tileRow.addView(optionTextView)

                if (userPreferences[transformedTileTitle] == option) {
                    optionTextView.setBackgroundResource(R.drawable.rounded_bg3)
                    selectedOptionsMap[transformedTileTitle] = optionTextView
                }

                optionTextView.setOnClickListener {
                    handleOptionClick(optionTextView, transformedTileTitle, currentUserUID)
                }
            }

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(30, 30, 30, 30)
            tileContent.layoutParams = layoutParams

            val tileId = View.generateViewId()
            tileContent.id = tileId

            cardContainer.addView(tileContent)
        }

        return cardLayout
    }

    private fun handleOptionClick(selectedTextView: TextView, tileTitle: String, currentUserUID: String?) {
        val previousSelected = selectedOptionsMap[tileTitle]
        previousSelected?.setBackgroundResource(android.R.color.transparent)
        selectedTextView.setBackgroundResource(R.drawable.rounded_bg3)
        selectedOptionsMap[tileTitle] = selectedTextView

        updatePreferenceInFirestore(tileTitle, selectedTextView.text.toString(), currentUserUID)
    }

    private fun updatePreferenceInFirestore(tileTitle: String, selectedOption: String, currentUserUID: String?) {
        currentUserUID ?: return

        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(currentUserUID)
            .update(tileTitle, selectedOption)
            .addOnSuccessListener {
                // Handle success if needed
            }
            .addOnFailureListener { _ ->
                // Handle failure or log the error
            }
    }
}
