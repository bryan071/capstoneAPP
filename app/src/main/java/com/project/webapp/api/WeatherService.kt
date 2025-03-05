import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.*
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.project.webapp.R
import kotlin.math.*

// 🚀 Philippine Cities with Coordinates
val cityCoordinates = mapOf(
    "Manila" to Pair(14.5995, 120.9842),
    "Quezon City" to Pair(14.6760, 121.0437),
    "Cebu City" to Pair(10.3157, 123.8854),
    "Davao City" to Pair(7.1907, 125.4553),
    "Baguio" to Pair(16.4023, 120.5960),
    "Iloilo City" to Pair(10.7202, 122.5621),
    "Bacolod" to Pair(10.6765, 122.9511),
    "Cagayan de Oro" to Pair(8.4542, 124.6319),
    "Zamboanga City" to Pair(6.9214, 122.0790),
    "General Santos" to Pair(6.1128, 125.1716)
)

// 🌍 Find the Nearest City
fun getNearestCity(lat: Double, lon: Double): String {
    var nearestCity = "Unknown"
    var shortestDistance = Double.MAX_VALUE

    for ((city, coordinates) in cityCoordinates) {
        val cityLat = coordinates.first
        val cityLon = coordinates.second
        val distance = haversine(lat, lon, cityLat, cityLon)

        if (distance < shortestDistance) {
            shortestDistance = distance
            nearestCity = city
        }
    }

    return nearestCity
}

// 📏 Haversine Formula for Distance Calculation
fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371 // Radius of Earth in KM
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c // Distance in KM
}

// 🌦️ Retrofit API Service
interface WeatherApiService {
    @GET("current.json")
    suspend fun getWeather(
        @Query("key") apiKey: String,
        @Query("q") location: String // Uses city name instead of lat/lon
    ): WeatherApiResponse
}

// 🌤️ Data Models
data class WeatherApiResponse(val current: CurrentWeather)
data class CurrentWeather(val temp_c: Double, val condition: WeatherCondition)
data class WeatherCondition(val text: String, val icon: String)

// 🔗 Retrofit Instance
fun getWeatherApi(): WeatherApiService {
    return Retrofit.Builder()
        .baseUrl("https://api.weatherapi.com/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherApiService::class.java)
}

// 🌡️ Weather UI Component
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WeatherSection(context: Context) {
    val fusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val scope = rememberCoroutineScope()
    val apiKey = "865c1bf771394c12ad1122044250303"

    var cityName by remember { mutableStateOf("Fetching location...") }
    var temperature by remember { mutableStateOf("Fetching...") }
    var weatherCondition by remember { mutableStateOf("Please wait...") }
    var weatherIcon by remember { mutableStateOf(R.drawable.sun) }

    val locationPermissionState = rememberPermissionState(permission = android.Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status.isGranted) {
            getUserLocation(fusedLocationProviderClient) { city, lat, lon ->
                cityName = city // ✅ Update city name in UI
                scope.launch {
                    fetchWeather(city, apiKey) { temp, condition, icon ->
                        temperature = "$temp°C"
                        weatherCondition = condition
                        weatherIcon = icon
                    }
                }
            }
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(Color(0xFF4CAF50))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = weatherIcon),
                    contentDescription = "Weather Icon",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Weather Update", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(cityName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White) // ✅ Show city name
                    Text("Temperature: $temperature | $weatherCondition", fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}



// 🌍 Function to Get User Location
@SuppressLint("MissingPermission")
private fun getUserLocation(
    fusedLocationProviderClient: FusedLocationProviderClient,
    callback: (String, Double, Double) -> Unit
) {
    fusedLocationProviderClient.lastLocation
        .addOnSuccessListener { location: Location? ->
            location?.let {
                val nearestCity = getNearestCity(it.latitude, it.longitude) // Get the nearest city
                Log.d("LocationDebug", "Detected City: $nearestCity") // ✅ Log the city
                Log.d("LocationDebug", "Latitude: ${it.latitude}, Longitude: ${it.longitude}") // ✅ Log the coordinates
                callback(nearestCity, it.latitude, it.longitude)
            } ?: run {
                Log.d("LocationDebug", "Failed to fetch location, using default Manila") // ✅ Log default location
                callback("Manila", 14.5995, 120.9842)
            }
        }
        .addOnFailureListener { exception ->
            Log.e("LocationError", "Error fetching location: ${exception.message}") // ✅ Log errors
        }
}


// 🌧️ Function to Fetch Weather Data
suspend fun fetchWeather(city: String, apiKey: String, onResult: (String, String, Int) -> Unit) {
    val weatherApi = getWeatherApi()
    try {
        val response = weatherApi.getWeather(apiKey, city) // ✅ Use city name instead of lat/lon
        val temp = response.current.temp_c.toString()
        val condition = response.current.condition.text
        val icon = mapWeatherToIcon(condition)

        onResult(temp, condition, icon)
    } catch (e: Exception) {
        e.printStackTrace()
        onResult("N/A", "Failed to fetch data: ${e.message}", R.drawable.sun)
    }
}


// 🌥️ Function to Map Weather Conditions to Icons
private fun mapWeatherToIcon(condition: String): Int {
    return when (condition.lowercase()) {
        "clear" -> R.drawable.sun
        "clouds" -> R.drawable.cloudy
        "rain" -> R.drawable.rainy
        "drizzle" -> R.drawable.drizzle
        "thunderstorm" -> R.drawable.thunder
        else -> R.drawable.sun
    }
}
