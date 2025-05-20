package com.github.droidworksstudio.mlauncher.helper.receivers

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class WeatherReceiver {

    private val gson = Gson()

    // Simple in-memory cache (replace with more robust caching if needed)
    private var cachedWeather: WeatherResponse? = null

    suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val urlStr =
                    "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=weather_code,temperature_2m"
                val response = URL(urlStr).readText()
                val weather = gson.fromJson(response, WeatherResponse::class.java)
                cachedWeather = weather // update cache
            } catch (e: Exception) {
                e.printStackTrace()
            }
            cachedWeather
        }
    }

    fun getWeatherEmoji(weatherCode: Int): String {
        return when (weatherCode) {
            0 -> "☀️" // Clear sky
            1 -> "🌤️" // Mainly clear
            2 -> "⛅" // Partly cloudy
            3 -> "☁️" // Overcast
            45, 48 -> "🌫️" // Fog

            51, 53, 55, 56, 57 -> "🌦️" // Drizzle / light rain
            61, 63, 65, 66, 67 -> "🌧️" // Rain
            80, 81, 82 -> "🌧️" // Rain showers

            71, 73, 75, 77, 85, 86 -> "❄️" // Snow
            95, 96, 99 -> "⛈️" // Thunderstorm

            else -> "❔" // Unknown
        }
    }
}

// 🔧 Full response wrapper
data class WeatherResponse(
    @SerializedName("current")
    val currentWeather: CurrentWeather,

    @SerializedName("current_units")
    val currentUnits: CurrentUnits
)

data class CurrentWeather(
    @SerializedName("temperature_2m")
    val temperature: Double,

    @SerializedName("weather_code")
    val weatherCode: Int,
)

data class CurrentUnits(
    @SerializedName("temperature_2m")
    val temperatureUnit: String,

    @SerializedName("weather_code")
    val weatherCodeUnit: String,
)
