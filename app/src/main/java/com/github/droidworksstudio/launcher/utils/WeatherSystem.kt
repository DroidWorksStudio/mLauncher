package com.github.droidworksstudio.launcher.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.droidworksstudio.launcher.MainActivity
import com.github.droidworksstudio.launcher.settings.SharedPreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class WeatherSystem(private val context: Context) {

    private val sharedPreferenceManager = SharedPreferenceManager(context)
    private val stringUtils = StringUtils()

    suspend fun setGpsLocation(activity: MainActivity) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                1
            )
            return
        }

        locationManager.getCurrentLocation(
            LocationManager.GPS_PROVIDER, // Only GPS provider functions on my phone with CalyxOS, so that's what you get.
            null,
            ContextCompat.getMainExecutor(context)
        )
        { location: Location? ->
            if (location != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    sharedPreferenceManager.setWeatherLocation("latitude=${latitude}&longitude=${longitude}", "Latest GPS location")
                    activity.updateWeatherText()
                }
            }
        }

    }

    // Run within Dispatchers.IO from the outside (doesn't seem to refresh properly otherwise)
    fun getSearchedLocations(searchTerm: String?): MutableList<Map<String, String>> {
        val foundLocations = mutableListOf<Map<String, String>>()

        val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$searchTerm&count=50&language=en&format=json")
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"
            try {
                inputStream.bufferedReader().use {
                    val response = it.readText()
                    val jsonObject = JSONObject(response)
                    val resultArray = jsonObject.getJSONArray("results")

                    for (i in 0 until resultArray.length()) {
                        val resultObject: JSONObject = resultArray.getJSONObject(i)

                        foundLocations.add(
                            mapOf(
                                "name" to resultObject.getString("name"),
                                "latitude" to resultObject.getDouble("latitude").toString(),
                                "longitude" to resultObject.getDouble("longitude").toString(),
                                "country" to resultObject.optString("country", resultObject.optString("country_code", "")),
                                "region" to stringUtils.addEndTextIfNotEmpty(resultObject.optString("admin2", resultObject.optString("admin1", resultObject.optString("admin3", ""))), ", ")
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }
        return foundLocations
    }

    // Run with Dispatchers.IO from the outside
    fun getTemp(): String {

        val tempUnits = sharedPreferenceManager.getTempUnits()
        var currentWeather = ""

        val location = sharedPreferenceManager.getWeatherLocation()

        if (location != null) {
            if (location.isNotEmpty()) {
                val url =
                    URL("https://api.open-meteo.com/v1/forecast?$location&temperature_unit=${tempUnits}&current=temperature_2m,weather_code")
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"

                    try {
                        inputStream.bufferedReader().use {
                            val response = it.readText()

                            val jsonObject = JSONObject(response)

                            val currentData = jsonObject.getJSONObject("current")

                            var weatherType = ""

                            when (currentData.getInt("weather_code")) {
                                0, 1 -> {
                                    weatherType = "☀\uFE0E" // Sunny
                                }

                                2, 3, 45, 48 -> {
                                    weatherType = "☁\uFE0E" // Sunny
                                }

                                51, 53, 55, 56, 57, 61, 63, 65, 67, 80, 81, 82 -> {
                                    weatherType = "☂\uFE0E" // Rain
                                }

                                71, 73, 75, 77, 85, 86 -> {
                                    weatherType = "❄\uFE0E" // Snow
                                }

                                95, 96, 99 -> {
                                    weatherType = "⛈\uFE0E" // Thunder
                                }

                            }

                            currentWeather = "$weatherType ${currentData.getInt("temperature_2m")}"

                        }

                    } catch (_: Exception) {
                    }
                }
            }
        }

        return when (tempUnits) {
            "celsius" -> {
                stringUtils.addEndTextIfNotEmpty(currentWeather, "°C")
            }

            "fahrenheit" -> {
                stringUtils.addEndTextIfNotEmpty(currentWeather, "°F")
            }

            else -> {
                ""
            }
        }

    }
}