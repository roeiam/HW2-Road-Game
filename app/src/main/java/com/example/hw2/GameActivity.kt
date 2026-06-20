package com.example.hw2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class GameActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        private const val ROWS = 8
        private const val COLS = 5

        private const val EMPTY = 0
        private const val OBSTACLE = 1
        private const val COIN = 2

        private const val BUTTONS_FAST_DELAY = 450L
        private const val BUTTONS_SLOW_DELAY = 800L

        private const val SENSOR_THRESHOLD = 3.5f
        private const val SENSOR_MOVE_DELAY = 350L
        private const val STATUS_MESSAGE_DELAY = 1100L

        private const val SENSOR_FAST_DELAY = 400L
        private const val SENSOR_NORMAL_DELAY = 650L
        private const val SENSOR_SLOW_DELAY = 900L
        private const val SENSOR_SPEED_THRESHOLD = 2.2f
        private const val SENSOR_SPEED_CHANGE_DELAY = 700L

        private const val SENSOR_GEAR_SLOW = 0
        private const val SENSOR_GEAR_NORMAL = 1
        private const val SENSOR_GEAR_FAST = 2

        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var gameTxtLives: TextView
    private lateinit var gameTxtCoins: TextView
    private lateinit var gameTxtDistance: TextView
    private lateinit var gameTxtSpeed: TextView
    private lateinit var gameTxtStatus: TextView
    private lateinit var gameTxtGameOverTitle: TextView
    private lateinit var gameTxtGameOverScore: TextView
    private lateinit var gameTxtGameOverHint: TextView

    private lateinit var gameBtnLeft: Button
    private lateinit var gameBtnRight: Button
    private lateinit var gameBtnNewGame: Button
    private lateinit var gameBtnMenu: Button
    private lateinit var gameBtnHighScores: Button

    private lateinit var gameLayoutButtons: LinearLayout
    private lateinit var gameLayoutGameOver: LinearLayout

    private lateinit var boardViews: Array<Array<ImageView>>
    private val gameMatrix = Array(ROWS) { IntArray(COLS) }

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var soundPool: SoundPool
    private var crashSoundId = 0
    private var coinSoundId = 0

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastSensorMoveTime = 0L
    private var lastSensorSpeedChangeTime = 0L
    private var sensorSpeedGear = SENSOR_GEAR_NORMAL

    private lateinit var locationManager: LocationManager
    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
    private var hasRealLocation = false

    private var playerCol = 2
    private var lives = 3
    private var coins = 0
    private var distance = 0
    private var gameMode = MainActivity.MODE_BUTTONS_SLOW
    private var gameDelay = BUTTONS_SLOW_DELAY
    private var isGameRunning = false
    private var isScoreSaved = false
    private var lastSavedScoreDate: String? = null

    private val gameRunnable = object : Runnable {
        override fun run() {
            if (isGameRunning) {
                gameTick()
                handler.postDelayed(this, gameDelay)
            }
        }
    }

    private val clearStatusRunnable = Runnable {
        if (isGameRunning) {
            updateStatus("")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        gameMode = intent.getStringExtra(MainActivity.EXTRA_GAME_MODE)
            ?: MainActivity.MODE_BUTTONS_SLOW

        setupSounds()
        setupSensors()
        setupLocation()
        findViews()
        initViews()
        startGame()
    }

    override fun onResume() {
        super.onResume()

        if (gameMode == MainActivity.MODE_SENSORS) {
            accelerometer?.let {
                sensorManager.registerListener(
                    this,
                    it,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }
        }

        updateCurrentLocationIfPossible()
    }

    override fun onPause() {
        super.onPause()

        if (gameMode == MainActivity.MODE_SENSORS) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopGame()
        soundPool.release()
    }

    private fun setupSounds() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        crashSoundId = soundPool.load(this, R.raw.crash_hit, 1)
        coinSoundId = soundPool.load(this, R.raw.coin_kaching, 1)
    }

    private fun setupSensors() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private fun setupLocation() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (hasLocationPermission()) {
            updateCurrentLocationIfPossible()
        } else {
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

    private fun hasLocationPermission(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    @SuppressLint("MissingPermission")
    private fun updateCurrentLocationIfPossible(): Boolean {
        if (!hasLocationPermission()) {
            hasRealLocation = false
            return false
        }

        return try {
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                hasRealLocation = false
                return false
            }

            val gpsLocation =
                if (isGpsEnabled) {
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                } else {
                    null
                }

            val networkLocation =
                if (isNetworkEnabled) {
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                } else {
                    null
                }

            val bestLocation = chooseBetterLocation(gpsLocation, networkLocation)

            if (bestLocation != null) {
                currentLatitude = bestLocation.latitude
                currentLongitude = bestLocation.longitude
                hasRealLocation = true
                true
            } else {
                hasRealLocation = false
                false
            }
        } catch (exception: Exception) {
            hasRealLocation = false
            false
        }
    }

    private fun chooseBetterLocation(gpsLocation: Location?, networkLocation: Location?): Location? {
        return when {
            gpsLocation != null && networkLocation != null -> {
                if (gpsLocation.time >= networkLocation.time) gpsLocation else networkLocation
            }

            gpsLocation != null -> gpsLocation
            networkLocation != null -> networkLocation
            else -> null
        }
    }

    private fun findViews() {
        gameTxtLives = findViewById(R.id.game_TXT_lives)
        gameTxtCoins = findViewById(R.id.game_TXT_coins)
        gameTxtDistance = findViewById(R.id.game_TXT_distance)
        gameTxtSpeed = findViewById(R.id.game_TXT_speed)
        gameTxtStatus = findViewById(R.id.game_TXT_status)
        gameTxtGameOverTitle = findViewById(R.id.game_TXT_game_over_title)
        gameTxtGameOverScore = findViewById(R.id.game_TXT_game_over_score)
        gameTxtGameOverHint = findViewById(R.id.game_TXT_game_over_hint)

        gameBtnLeft = findViewById(R.id.game_BTN_left)
        gameBtnRight = findViewById(R.id.game_BTN_right)
        gameBtnNewGame = findViewById(R.id.game_BTN_new_game)
        gameBtnMenu = findViewById(R.id.game_BTN_menu)
        gameBtnHighScores = findViewById(R.id.game_BTN_high_scores)

        gameLayoutButtons = findViewById(R.id.game_LAYOUT_buttons)
        gameLayoutGameOver = findViewById(R.id.game_LAYOUT_game_over)

        boardViews = arrayOf(
            arrayOf(
                findViewById(R.id.game_IMG_00),
                findViewById(R.id.game_IMG_01),
                findViewById(R.id.game_IMG_02),
                findViewById(R.id.game_IMG_03),
                findViewById(R.id.game_IMG_04)
            ),
            arrayOf(
                findViewById(R.id.game_IMG_10),
                findViewById(R.id.game_IMG_11),
                findViewById(R.id.game_IMG_12),
                findViewById(R.id.game_IMG_13),
                findViewById(R.id.game_IMG_14)
            ),
            arrayOf(
                findViewById(R.id.game_IMG_20),
                findViewById(R.id.game_IMG_21),
                findViewById(R.id.game_IMG_22),
                findViewById(R.id.game_IMG_23),
                findViewById(R.id.game_IMG_24)
            ),
            arrayOf(
                findViewById(R.id.game_IMG_30),
                findViewById(R.id.game_IMG_31),
                findViewById(R.id.game_IMG_32),
                findViewById(R.id.game_IMG_33),
                findViewById(R.id.game_IMG_34)
            ),
            arrayOf(
                findViewById(R.id.game_IMG_40),
                findViewById(R.id.game_IMG_41),
                findViewById(R.id.game_IMG_42),
                findViewById(R.id.game_IMG_43),
                findViewById(R.id.game_IMG_44)
            ),
            arrayOf(
                findViewById(R.id.game_IMG_50),
                findViewById(R.id.game_IMG_51),
                findViewById(R.id.game_IMG_52),
                findViewById(R.id.game_IMG_53),
                findViewById(R.id.game_IMG_54)
            ),
            arrayOf(
                findViewById(R.id.game_IMG_60),
                findViewById(R.id.game_IMG_61),
                findViewById(R.id.game_IMG_62),
                findViewById(R.id.game_IMG_63),
                findViewById(R.id.game_IMG_64)
            ),
            arrayOf(
                findViewById(R.id.game_IMG_70),
                findViewById(R.id.game_IMG_71),
                findViewById(R.id.game_IMG_72),
                findViewById(R.id.game_IMG_73),
                findViewById(R.id.game_IMG_74)
            )
        )
    }

    private fun initViews() {
        sensorSpeedGear = SENSOR_GEAR_NORMAL
        gameDelay = getInitialDelay()

        if (gameMode == MainActivity.MODE_SENSORS) {
            gameLayoutButtons.visibility = View.GONE
        } else {
            gameLayoutButtons.visibility = View.VISIBLE
        }

        gameLayoutGameOver.visibility = View.GONE

        gameBtnLeft.setOnClickListener {
            moveLeft()
        }

        gameBtnRight.setOnClickListener {
            moveRight()
        }

        gameBtnNewGame.setOnClickListener {
            restartGame()
        }

        gameBtnMenu.setOnClickListener {
            openMenu()
        }

        gameBtnHighScores.setOnClickListener {
            openHighScores()
        }

        updateStatus("")
        updateSpeedText(getCurrentSpeedText())
        updateTopBar()
        renderBoard()
    }

    private fun getInitialDelay(): Long {
        return when (gameMode) {
            MainActivity.MODE_BUTTONS_FAST -> BUTTONS_FAST_DELAY
            MainActivity.MODE_BUTTONS_SLOW -> BUTTONS_SLOW_DELAY
            MainActivity.MODE_SENSORS -> SENSOR_NORMAL_DELAY
            else -> BUTTONS_SLOW_DELAY
        }
    }

    private fun startGame() {
        isGameRunning = true
        handler.postDelayed(gameRunnable, gameDelay)
    }

    private fun stopGame() {
        isGameRunning = false
        handler.removeCallbacks(gameRunnable)
        handler.removeCallbacks(clearStatusRunnable)
    }

    private fun restartGame() {
        stopGame()

        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                gameMatrix[row][col] = EMPTY
            }
        }

        playerCol = 2
        lives = 3
        coins = 0
        distance = 0
        isScoreSaved = false
        lastSavedScoreDate = null
        lastSensorMoveTime = 0L
        lastSensorSpeedChangeTime = 0L
        sensorSpeedGear = SENSOR_GEAR_NORMAL
        gameDelay = getInitialDelay()

        gameLayoutGameOver.visibility = View.GONE

        updateStatus("New game started")
        updateSpeedText(getCurrentSpeedText())
        scheduleStatusReset()
        updateTopBar()
        renderBoard()
        startGame()
    }

    private fun openMenu() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun openHighScores() {
        val intent = Intent(this, RecordsActivity::class.java)
        lastSavedScoreDate?.let {
            intent.putExtra(RecordsActivity.EXTRA_SELECTED_SCORE_DATE, it)
        }
        startActivity(intent)
    }

    private fun gameTick() {
        moveObjectsDown()
        spawnNewObject()
        checkCollision()
        distance++
        updateTopBar()
        renderBoard()
    }

    private fun moveObjectsDown() {
        for (row in ROWS - 1 downTo 1) {
            for (col in 0 until COLS) {
                gameMatrix[row][col] = gameMatrix[row - 1][col]
            }
        }

        for (col in 0 until COLS) {
            gameMatrix[0][col] = EMPTY
        }
    }

    private fun spawnNewObject() {
        val randomNumber = Random.nextInt(100)

        when {
            randomNumber < 55 -> {
                val col = Random.nextInt(COLS)
                gameMatrix[0][col] = OBSTACLE
            }

            randomNumber < 75 -> {
                val col = Random.nextInt(COLS)
                gameMatrix[0][col] = COIN
            }
        }
    }

    private fun checkCollision() {
        when (gameMatrix[ROWS - 1][playerCol]) {
            OBSTACLE -> {
                lives--
                gameMatrix[ROWS - 1][playerCol] = EMPTY
                playCrashSound()
                showTemporaryStatus("Crash!")

                if (lives <= 0) {
                    gameOver()
                }
            }

            COIN -> {
                coins++
                gameMatrix[ROWS - 1][playerCol] = EMPTY
                playCoinSound()
                showTemporaryStatus("Coin collected!")
            }
        }
    }

    private fun playCrashSound() {
        soundPool.play(crashSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    private fun playCoinSound() {
        soundPool.play(coinSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    private fun gameOver() {
        stopGame()
        saveCurrentScore()

        val finalScore = calculateScore()

        updateStatus("Game Over!")
        gameTxtGameOverTitle.text = "Game Over"
        gameTxtGameOverScore.text = "Score: $finalScore"
        gameTxtGameOverHint.text = "See your rank on the scoreboard"

        gameLayoutGameOver.visibility = View.VISIBLE
    }

    private fun saveCurrentScore() {
        if (isScoreSaved) {
            return
        }

        isScoreSaved = true

        updateCurrentLocationIfPossible()
        val locationForScore = getLocationForScore()
        val scoreDate = getCurrentDate()

        val scoreRecord = ScoreRecord(
            score = calculateScore(),
            distance = distance,
            coins = coins,
            latitude = locationForScore.first,
            longitude = locationForScore.second,
            date = scoreDate
        )

        ScoreManager.saveScore(this, scoreRecord)
        lastSavedScoreDate = scoreDate
    }

    private fun getLocationForScore(): Pair<Double, Double> {
        if (hasRealLocation) {
            return Pair(currentLatitude, currentLongitude)
        }

        return getRandomIsraelLocation()
    }

    private fun getRandomIsraelLocation(): Pair<Double, Double> {
        val israelLocations = listOf(
            Pair(32.0853, 34.7818), // Tel Aviv
            Pair(31.7683, 35.2137), // Jerusalem
            Pair(32.7940, 34.9896), // Haifa
            Pair(31.2518, 34.7913), // Be'er Sheva
            Pair(32.3215, 34.8532), // Netanya
            Pair(31.9730, 34.7925), // Rishon LeZion
            Pair(31.8948, 34.8113), // Rehovot
            Pair(32.0840, 34.8878), // Petah Tikva
            Pair(32.1782, 34.9076), // Kfar Saba
            Pair(32.1663, 34.8433), // Herzliya
            Pair(32.0231, 34.7503), // Holon
            Pair(32.0158, 34.7874), // Bat Yam
            Pair(31.8044, 34.6553), // Ashdod
            Pair(31.6688, 34.5743), // Ashkelon
            Pair(32.6996, 35.3035), // Nazareth
            Pair(32.9236, 35.2933), // Karmiel
            Pair(32.9281, 35.0765), // Acre
            Pair(33.2073, 35.5708), // Kiryat Shmona
            Pair(32.9656, 35.4950), // Safed
            Pair(32.7922, 35.5312), // Tiberias
            Pair(32.6091, 35.2891), // Afula
            Pair(32.4340, 34.9196), // Hadera
            Pair(32.1848, 34.8713), // Ra'anana
            Pair(31.9061, 35.0078), // Modi'in
            Pair(31.4215, 34.5969), // Netivot
            Pair(31.5250, 34.5950), // Sderot
            Pair(31.0461, 34.8516), // Dimona
            Pair(30.6102, 34.8019), // Mitzpe Ramon
            Pair(29.5577, 34.9519)  // Eilat
        )

        return israelLocations.random()
    }

    private fun calculateScore(): Int {
        return distance + coins * 10
    }

    private fun getCurrentDate(): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun moveLeft() {
        if (!isGameRunning) {
            return
        }

        if (playerCol > 0) {
            playerCol--
            checkCollision()
            updateTopBar()
            renderBoard()
        }
    }

    private fun moveRight() {
        if (!isGameRunning) {
            return
        }

        if (playerCol < COLS - 1) {
            playerCol++
            checkCollision()
            updateTopBar()
            renderBoard()
        }
    }

    private fun showTemporaryStatus(message: String) {
        updateStatus(message)
        scheduleStatusReset()
    }

    private fun scheduleStatusReset() {
        handler.removeCallbacks(clearStatusRunnable)
        handler.postDelayed(clearStatusRunnable, STATUS_MESSAGE_DELAY)
    }

    private fun updateStatus(message: String) {
        gameTxtStatus.text = message
    }

    private fun updateSpeedText(speedText: String) {
        gameTxtSpeed.text = speedText
    }

    private fun getCurrentSpeedText(): String {
        return when (gameMode) {
            MainActivity.MODE_BUTTONS_FAST -> "FAST"
            MainActivity.MODE_BUTTONS_SLOW -> "SLOW"
            MainActivity.MODE_SENSORS -> {
                when (sensorSpeedGear) {
                    SENSOR_GEAR_SLOW -> "SLOW"
                    SENSOR_GEAR_FAST -> "FAST"
                    else -> "NORMAL"
                }
            }

            else -> "NORMAL"
        }
    }

    private fun updateSensorDelayByGear() {
        gameDelay = when (sensorSpeedGear) {
            SENSOR_GEAR_SLOW -> SENSOR_SLOW_DELAY
            SENSOR_GEAR_FAST -> SENSOR_FAST_DELAY
            else -> SENSOR_NORMAL_DELAY
        }

        updateSpeedText(getCurrentSpeedText())
    }

    private fun updateTopBar() {
        gameTxtLives.text = "Lives: $lives"
        gameTxtCoins.text = "Coins: $coins"
        gameTxtDistance.text = "Distance: $distance"
    }

    private fun renderBoard() {
        clearBoardViews()

        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                when (gameMatrix[row][col]) {
                    OBSTACLE -> {
                        boardViews[row][col].setImageResource(R.drawable.ic_obstacle)
                        boardViews[row][col].visibility = View.VISIBLE
                    }

                    COIN -> {
                        boardViews[row][col].setImageResource(R.drawable.ic_coin)
                        boardViews[row][col].visibility = View.VISIBLE
                    }
                }
            }
        }

        boardViews[ROWS - 1][playerCol].setImageResource(R.drawable.ic_car)
        boardViews[ROWS - 1][playerCol].visibility = View.VISIBLE
    }

    private fun clearBoardViews() {
        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                boardViews[row][col].setImageDrawable(null)
                boardViews[row][col].visibility = View.INVISIBLE
            }
        }
    }

    private fun updateSensorSpeed(y: Float) {
        if (gameMode != MainActivity.MODE_SENSORS || !isGameRunning) {
            return
        }

        val currentTime = System.currentTimeMillis()

        if (currentTime - lastSensorSpeedChangeTime < SENSOR_SPEED_CHANGE_DELAY) {
            return
        }

        if (y < -SENSOR_SPEED_THRESHOLD) {
            if (sensorSpeedGear < SENSOR_GEAR_FAST) {
                sensorSpeedGear++
                updateSensorDelayByGear()
                lastSensorSpeedChangeTime = currentTime
            }
        } else if (y > SENSOR_SPEED_THRESHOLD) {
            if (sensorSpeedGear > SENSOR_GEAR_SLOW) {
                sensorSpeedGear--
                updateSensorDelayByGear()
                lastSensorSpeedChangeTime = currentTime
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (gameMode != MainActivity.MODE_SENSORS || !isGameRunning) {
            return
        }

        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) {
            return
        }

        val x = event.values[0]
        val y = event.values[1]

        updateSensorSpeed(y)

        val currentTime = System.currentTimeMillis()

        if (currentTime - lastSensorMoveTime < SENSOR_MOVE_DELAY) {
            return
        }

        if (x > SENSOR_THRESHOLD) {
            moveLeft()
            lastSensorMoveTime = currentTime
        } else if (x < -SENSOR_THRESHOLD) {
            moveRight()
            lastSensorMoveTime = currentTime
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this game.
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                updateCurrentLocationIfPossible()
            } else {
                hasRealLocation = false
                showTemporaryStatus("Location off: random Israel locations")
            }
        }
    }
}