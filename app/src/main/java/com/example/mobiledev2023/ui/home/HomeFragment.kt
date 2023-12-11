package com.example.mobiledev2023.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.example.mobiledev2023.R
import com.example.mobiledev2023.ui.builders.CourtBookingBuilder
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val constraintLayout = view.findViewById<ConstraintLayout>(R.id.fragment_home_layout)

        // Access a Cloud Firestore instance
        val db = FirebaseFirestore.getInstance()

        // Access the "courts" collection
        val cardBuilder = CourtBookingBuilder(requireContext(), db)
        val dynamicCard = cardBuilder.buildCard2(
            "Court Booking",
            requireContext(),
            view
        )

        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        dynamicCard.layoutParams = params

        constraintLayout.addView(dynamicCard)
        // Handle errors while fetching data


        return view
    }
}
