package com.example.hw2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class ScoresMapFragment : Fragment(), OnMapReadyCallback {

    companion object {
        private const val ARG_SELECTED_SCORE_DATE = "ARG_SELECTED_SCORE_DATE"

        private const val ISRAEL_LATITUDE = 31.7683
        private const val ISRAEL_LONGITUDE = 35.2137
        private const val EMPTY_MAP_ZOOM = 7f
        private const val SCORE_ZOOM = 15f

        fun newInstance(selectedScoreDate: String?): ScoresMapFragment {
            val fragment = ScoresMapFragment()
            val bundle = Bundle()
            bundle.putString(ARG_SELECTED_SCORE_DATE, selectedScoreDate)
            fragment.arguments = bundle
            return fragment
        }
    }

    private var googleMap: GoogleMap? = null
    private var pendingScoreRecord: ScoreRecord? = null
    private var selectedScoreDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedScoreDate = arguments?.getString(ARG_SELECTED_SCORE_DATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        val mapFragment = SupportMapFragment.newInstance()

        childFragmentManager.beginTransaction()
            .replace(R.id.map_FRAGMENT_container, mapFragment)
            .commit()

        mapFragment.getMapAsync(this)

        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.clear()

        val scores = ScoreManager.getScores(requireContext())

        val selectedScore = selectedScoreDate?.let { date ->
            scores.firstOrNull { it.date == date }
        }

        when {
            pendingScoreRecord != null -> {
                showScoreLocation(pendingScoreRecord!!)
                pendingScoreRecord = null
            }

            selectedScore != null -> {
                showScoreLocation(selectedScore)
            }

            scores.isNotEmpty() -> {
                showScoreLocation(scores.first())
            }

            else -> {
                showEmptyMap(map)
            }
        }
    }

    private fun showEmptyMap(map: GoogleMap) {
        val israelCenter = LatLng(ISRAEL_LATITUDE, ISRAEL_LONGITUDE)

        map.clear()
        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(israelCenter, EMPTY_MAP_ZOOM)
        )
    }

    fun showScoreLocation(scoreRecord: ScoreRecord) {
        val map = googleMap

        if (map == null) {
            pendingScoreRecord = scoreRecord
            return
        }

        selectedScoreDate = scoreRecord.date

        val scoreLocation = LatLng(scoreRecord.latitude, scoreRecord.longitude)

        map.clear()

        map.addMarker(
            MarkerOptions()
                .position(scoreLocation)
                .title("Score: ${scoreRecord.score}")
                .snippet("Distance: ${scoreRecord.distance}, Coins: ${scoreRecord.coins}")
        )

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(scoreLocation, SCORE_ZOOM)
        )
    }
}