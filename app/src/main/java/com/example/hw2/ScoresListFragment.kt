package com.example.hw2

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class ScoresListFragment : Fragment() {

    interface OnScoreClickedListener {
        fun onScoreClicked(scoreRecord: ScoreRecord)
    }

    companion object {
        private const val ARG_SELECTED_SCORE_DATE = "ARG_SELECTED_SCORE_DATE"

        fun newInstance(selectedScoreDate: String?): ScoresListFragment {
            val fragment = ScoresListFragment()
            val bundle = Bundle()
            bundle.putString(ARG_SELECTED_SCORE_DATE, selectedScoreDate)
            fragment.arguments = bundle
            return fragment
        }
    }

    private var listener: OnScoreClickedListener? = null
    private lateinit var scoresLayoutList: LinearLayout

    private var selectedScoreDate: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is OnScoreClickedListener) {
            listener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedScoreDate = arguments?.getString(ARG_SELECTED_SCORE_DATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scores_list, container, false)

        scoresLayoutList = view.findViewById(R.id.scores_LAYOUT_list)
        loadScores()

        return view
    }

    fun selectScore(scoreDate: String) {
        selectedScoreDate = scoreDate

        if (::scoresLayoutList.isInitialized) {
            loadScores()
        }
    }

    private fun loadScores() {
        val scores = ScoreManager.getScores(requireContext())

        scoresLayoutList.removeAllViews()

        if (scores.isEmpty()) {
            selectedScoreDate = null
            addEmptyMessage()
            return
        }

        if (selectedScoreDate == null) {
            selectedScoreDate = scores.first().date
        }

        for ((index, scoreRecord) in scores.withIndex()) {
            addScoreView(index + 1, scoreRecord)
        }
    }

    private fun addEmptyMessage() {
        val textView = TextView(requireContext())

        textView.text = "No scores yet"
        textView.textSize = 18f
        textView.setTextColor(0xFF263238.toInt())
        textView.gravity = Gravity.CENTER
        textView.setPadding(12, 24, 12, 24)

        scoresLayoutList.addView(textView)
    }

    private fun addScoreView(position: Int, scoreRecord: ScoreRecord) {
        val textView = TextView(requireContext())
        val isSelected = scoreRecord.date == selectedScoreDate

        val scoreText = buildScoreText(position, scoreRecord)

        textView.text = if (isSelected) {
            "▶ $scoreText"
        } else {
            scoreText
        }

        textView.textSize = 16f
        textView.setTextColor(0xFF263238.toInt())
        textView.setPadding(16, 14, 16, 14)

        if (isSelected) {
            textView.setBackgroundColor(0xFFFFF59D.toInt())
        } else {
            textView.setBackgroundColor(0xFFECEFF1.toInt())
        }

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        layoutParams.setMargins(0, 0, 0, 8)
        textView.layoutParams = layoutParams

        textView.setOnClickListener {
            selectedScoreDate = scoreRecord.date
            loadScores()
            listener?.onScoreClicked(scoreRecord)
        }

        scoresLayoutList.addView(textView)
    }

    private fun buildScoreText(position: Int, scoreRecord: ScoreRecord): String {
        return "#$position  Score: ${scoreRecord.score} | Distance: ${scoreRecord.distance} | Coins: ${scoreRecord.coins}\n${scoreRecord.date}"
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}