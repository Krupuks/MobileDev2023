package com.example.mobiledev2023.ui.match

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.example.mobiledev2023.R
import com.example.mobiledev2023.ui.builders.MatchCreatingBuilder
import com.google.firebase.firestore.FirebaseFirestore

class MatchFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val db = FirebaseFirestore.getInstance()

        val view = inflater.inflate(R.layout.fragment_match, container, false)
        val constraintLayout = view.findViewById<ConstraintLayout>(R.id.fragment_match_layout)

        // Access the "courts" collection
        val cardBuilder = MatchCreatingBuilder(requireContext(), db)
        val dynamicCard = cardBuilder.buildCard(
            "Create a Match",
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
