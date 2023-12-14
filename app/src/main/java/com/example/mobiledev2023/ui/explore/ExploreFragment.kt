package com.example.mobiledev2023.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.example.mobiledev2023.R
import com.example.mobiledev2023.ui.builders.ShowMatchesBuilder
import com.google.firebase.firestore.FirebaseFirestore

class ExploreFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val db = FirebaseFirestore.getInstance()

        val view = inflater.inflate(R.layout.fragment_explore, container, false)
        val constraintLayout = view.findViewById<ConstraintLayout>(R.id.fragment_explore_layout)

        val cardBuilder = ShowMatchesBuilder(requireContext(), db)
        val dynamicCard = cardBuilder.buildCard("Available Matches")

        val params = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        dynamicCard.layoutParams = params

        constraintLayout.addView(dynamicCard)

        return view
    }
}
