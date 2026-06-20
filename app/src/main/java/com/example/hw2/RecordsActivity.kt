package com.example.hw2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class RecordsActivity : AppCompatActivity(), ScoresListFragment.OnScoreClickedListener {

    companion object {
        const val EXTRA_SELECTED_SCORE_DATE = "EXTRA_SELECTED_SCORE_DATE"
    }

    private lateinit var recordsBtnMenu: Button
    private lateinit var scoresListFragment: ScoresListFragment
    private lateinit var scoresMapFragment: ScoresMapFragment

    private var selectedScoreDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_records)

        selectedScoreDate = intent.getStringExtra(EXTRA_SELECTED_SCORE_DATE)

        findViews()
        initViews()
        setupFragments()
    }

    private fun findViews() {
        recordsBtnMenu = findViewById(R.id.records_BTN_menu)
    }

    private fun initViews() {
        recordsBtnMenu.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun setupFragments() {
        scoresListFragment = ScoresListFragment.newInstance(selectedScoreDate)
        scoresMapFragment = ScoresMapFragment.newInstance(selectedScoreDate)

        supportFragmentManager.beginTransaction()
            .replace(R.id.records_FRAME_scores, scoresListFragment)
            .replace(R.id.records_FRAME_map, scoresMapFragment)
            .commit()
    }

    override fun onScoreClicked(scoreRecord: ScoreRecord) {
        selectedScoreDate = scoreRecord.date
        scoresListFragment.selectScore(scoreRecord.date)
        scoresMapFragment.showScoreLocation(scoreRecord)
    }
}