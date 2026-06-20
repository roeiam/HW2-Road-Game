# Road Game (HW2)

## Student

Roei Amor - 211336607

## Course

User Interface Development

## Description

Road Game is an Android arcade road game developed in Kotlin as the second homework assignment.

The player controls a car on a five-lane road, avoids obstacles, collects coins, and tries to achieve the highest score.

The game supports both button controls and sensor controls.
It also includes a high scores screen with a top 10 score table and a Google Map that shows the location where each score was recorded.

The project includes the required HW2 features and also implements the bonus sensor speed feature.

---

## Main Features

* Main menu screen
* Buttons mode - slow speed
* Buttons mode - fast speed
* Sensor mode using the accelerometer
* Bonus feature: forward/backward tilt changes the game speed
* Three sensor speed gears: SLOW, NORMAL, FAST
* Five-lane road
* Long road board
* Visual matrix-based game board
* Obstacles on the road
* Coins on the road
* Lives counter
* Coins counter
* Distance counter / odometer
* Crash sound effect
* Coin collection sound effect
* Game over screen
* New Game button after game over
* Menu button after game over
* High Scores button after game over
* High scores screen with two fragments
* Top 10 high scores table
* Google Map showing score locations
* Clicking a score updates the map location
* Selected score is highlighted in the score table
* Custom app launcher icon
* Location support for score records
* Real phone location when location is available
* Random valid Israeli city/town location when location is unavailable

---

## Game Modes

### Buttons Mode - Slow

The player controls the car using left and right buttons.

The game speed is slower and easier.

In this mode, the speed indicator shows:

```text
SLOW
```

### Buttons Mode - Fast

The player controls the car using left and right buttons.

The game speed is faster and more challenging.

In this mode, the speed indicator shows:

```text
FAST
```

### Sensor Mode

The player controls the car by tilting the phone left and right using the accelerometer.

In this mode, the left and right buttons are hidden.

Sensor controls:

```text
Tilt left  -> move the car left
Tilt right -> move the car right
```

### Bonus: Sensor Speed Control

The bonus feature is implemented in Sensor Mode.

The player can tilt the phone forward and backward to control the game speed using three speed gears:

```text
SLOW -> NORMAL -> FAST
```

The sensor speed system works like gears:

```text
Tilt forward  -> increase speed by one gear
Tilt backward -> decrease speed by one gear
```

For example:

```text
SLOW + forward tilt   -> NORMAL
NORMAL + forward tilt -> FAST
FAST + backward tilt  -> NORMAL
NORMAL + backward tilt -> SLOW
```

A short cooldown is used between speed changes so that one tilt does not skip multiple gears at once.

The current speed is displayed on the game screen as:

```text
SLOW / NORMAL / FAST
```

---

## Screens

### Main Menu

The main menu contains:

* Buttons Mode - Slow
* Buttons Mode - Fast
* Sensor Mode
* High Scores

The menu also displays the app logo and the game title.

### Game Screen

The game screen contains:

* Lives counter
* Coins counter
* Distance counter
* Speed indicator
* Status message area
* Visual road board
* Player car
* Obstacles
* Coins
* Left and Right buttons in button modes

The goal is to avoid obstacles, collect coins, and survive as long as possible.

In button modes, the player moves using the Left and Right buttons.

In sensor mode, the buttons are hidden and the player controls the car using phone tilt.

### Game Over Screen

When the player loses all lives, the game stops and displays:

* Game Over title
* Final score
* New Game button
* Menu button
* High Scores button

The New Game button starts a new game in the same mode.

The Menu button returns to the main menu.

The High Scores button opens the high scores screen and shows the saved score.

### High Scores Screen

The high scores screen contains two fragments:

1. `ScoresListFragment`
   Displays the top 10 scores.

2. `ScoresMapFragment`
   Displays a Google Map with the location of the selected score.

Clicking a score in the table highlights the selected score and updates the marker on the map.

---

## Technologies Used

* Kotlin
* Android Studio
* XML Layouts
* Activities
* Fragments
* ImageView matrix
* SharedPreferences
* JSON
* SensorManager
* Accelerometer
* SoundPool
* Google Maps SDK for Android
* LocationManager

---

## Project Structure

```text
MainActivity.kt
- Main menu screen
- Opens the game in different modes
- Opens the high scores screen
- Requests location permission

GameActivity.kt
- Main game logic
- Visual matrix board
- Button controls
- Sensor controls
- Sensor speed bonus
- Coins and obstacles
- Sounds
- Score calculation
- Score saving
- Game over screen
- Menu navigation from game over

RecordsActivity.kt
- Hosts the high score fragments
- Connects score selection to map updates

ScoresListFragment.kt
- Displays the top 10 scores
- Highlights the selected score
- Sends selected score data to the activity

ScoresMapFragment.kt
- Displays Google Map
- Shows the selected score location on the map

ScoreRecord.kt
- Data class for score records

ScoreManager.kt
- Saves and loads scores using SharedPreferences
```

---

## Scoring Method

The final score is calculated using the distance and the number of collected coins.

```text
score = distance + coins * 10
```

The best 10 scores are saved locally on the device.

---

## Location Handling

When a score is saved, the app tries to save the current phone location.

If location permission is granted and a location is available, the score is saved with the real phone location.

If location permission is denied, GPS is disabled, or no location is available, the app saves the score with a random valid location in Israel.

The random fallback location is not a random coordinate rectangle.
Instead, the app chooses a random location from a list of real Israeli cities and towns, such as:

```text
Tel Aviv
Jerusalem
Haifa
Be'er Sheva
Netanya
Rishon LeZion
Rehovot
Petah Tikva
Kfar Saba
Herzliya
Holon
Bat Yam
Ashdod
Ashkelon
Nazareth
Karmiel
Acre
Kiryat Shmona
Safed
Tiberias
Afula
Hadera
Ra'anana
Modi'in
Netivot
Sderot
Dimona
Mitzpe Ramon
Eilat
```

This prevents the map marker from appearing in the sea or outside valid locations.

This also allows the high scores map to always show a valid marker, even if the phone location is turned off.

---

## Permissions

The app uses internet and location permissions.

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

Location permission is used only for saving the score location.

If the user does not allow location access, the game still works normally and uses a random valid Israeli city/town location for the score record.

---

## Google Maps

The high scores screen uses Google Maps SDK for Android.

The API key is configured in `AndroidManifest.xml` under:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY" />
```

For security, the real API key should be restricted in Google Cloud Console.

---

## Important Implementation Note

This project does not use custom drawing.

The game does **not** use:

* Canvas
* Paint
* onDraw
* SurfaceView
* Custom drawing
* Matrix animation
* Transformation animation
* ObjectAnimator
* translationX / translationY for game movement

The game board is implemented as a visual matrix of `ImageView` cells.

Each game tick updates the logical matrix and then updates the visible images in the board.

The player car stays on the bottom row, and the road objects move down through the matrix.

This approach follows the assignment requirements and avoids custom drawing or animation-based movement.

---

## Game Board Implementation

The road is represented using a logical matrix.

Each cell in the matrix can contain:

```text
EMPTY
OBSTACLE
COIN
```

The visual board is made from `ImageView` cells.

On each game tick:

1. Existing objects move one row down.
2. A new obstacle or coin may be created at the top row.
3. Collision is checked against the player car.
4. Counters are updated.
5. The visual matrix is rendered again.

This creates the road movement effect without using Canvas or animations.

---

## How to Run

1. Clone the repository.
2. Open the project in Android Studio.
3. Sync Gradle.
4. Make sure the Google Maps API key is configured in `AndroidManifest.xml`.
5. Connect an Android device or start an emulator.
6. Run the app.

---

## Demo Video

The demo video should show:

* App icon and app name
* Main menu
* Buttons Mode - Slow
* Buttons Mode - Fast
* Sensor Mode
* Left/right movement
* Sensor left/right tilt movement
* Bonus sensor speed control
* Speed indicator changing between SLOW, NORMAL, and FAST
* Obstacles
* Coins
* Crash sound
* Coin sound
* Distance counter
* Game over screen
* New Game button
* Menu button
* High Scores button
* High scores screen
* Score selection
* Map marker update
* Real location behavior when location is enabled
* Random Israeli city/town fallback when location is disabled

---

## Notes

* The repository should be public for submission.
* The GitHub link should be submitted together with the demo video.
* The game was developed as an Android Kotlin project for HW2.
