package com.example.hw2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GAME_MODE = "EXTRA_GAME_MODE"

        const val MODE_BUTTONS_SLOW = "MODE_BUTTONS_SLOW"
        const val MODE_BUTTONS_FAST = "MODE_BUTTONS_FAST"
        const val MODE_SENSORS = "MODE_SENSORS"

        private const val LOCATION_PERMISSION_REQUEST_CODE = 2001
    }

    private lateinit var mainBtnSlow: Button
    private lateinit var mainBtnFast: Button
    private lateinit var mainBtnSensors: Button
    private lateinit var mainBtnRecords: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestLocationPermissionIfNeeded()

        findViews()
        initViews()
    }

    private fun requestLocationPermissionIfNeeded() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted && !coarseLocationGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun findViews() {
        mainBtnSlow = findViewById(R.id.main_BTN_slow)
        mainBtnFast = findViewById(R.id.main_BTN_fast)
        mainBtnSensors = findViewById(R.id.main_BTN_sensors)
        mainBtnRecords = findViewById(R.id.main_BTN_records)
    }

    private fun initViews() {
        mainBtnSlow.setOnClickListener {
            openGame(MODE_BUTTONS_SLOW)
        }

        mainBtnFast.setOnClickListener {
            openGame(MODE_BUTTONS_FAST)
        }

        mainBtnSensors.setOnClickListener {
            openGame(MODE_SENSORS)
        }

        mainBtnRecords.setOnClickListener {
            val intent = Intent(this, RecordsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun openGame(mode: String) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra(EXTRA_GAME_MODE, mode)
        startActivity(intent)
    }
}