package com.example.hw2

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ScoreManager {

    private const val PREFS_NAME = "HW2_SCORE_PREFS"
    private const val KEY_SCORES = "KEY_SCORES"
    private const val MAX_SCORES = 10

    fun saveScore(context: Context, scoreRecord: ScoreRecord) {
        val scores = getScores(context).toMutableList()

        scores.add(scoreRecord)

        val topScores = scores
            .sortedByDescending { it.score }
            .take(MAX_SCORES)

        saveScoresList(context, topScores)
    }

    fun getScores(context: Context): List<ScoreRecord> {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = sharedPreferences.getString(KEY_SCORES, null) ?: return emptyList()

        val scores = mutableListOf<ScoreRecord>()

        try {
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)

                val scoreRecord = ScoreRecord(
                    score = jsonObject.getInt("score"),
                    distance = jsonObject.getInt("distance"),
                    coins = jsonObject.getInt("coins"),
                    latitude = jsonObject.getDouble("latitude"),
                    longitude = jsonObject.getDouble("longitude"),
                    date = jsonObject.getString("date")
                )

                scores.add(scoreRecord)
            }
        } catch (exception: Exception) {
            return emptyList()
        }

        return scores.sortedByDescending { it.score }
    }

    private fun saveScoresList(context: Context, scores: List<ScoreRecord>) {
        val jsonArray = JSONArray()

        for (score in scores) {
            val jsonObject = JSONObject()

            jsonObject.put("score", score.score)
            jsonObject.put("distance", score.distance)
            jsonObject.put("coins", score.coins)
            jsonObject.put("latitude", score.latitude)
            jsonObject.put("longitude", score.longitude)
            jsonObject.put("date", score.date)

            jsonArray.put(jsonObject)
        }

        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        sharedPreferences.edit()
            .putString(KEY_SCORES, jsonArray.toString())
            .apply()
    }
}