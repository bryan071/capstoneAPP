import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.project.webapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import kotlin.collections.forEachIndexed

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WeatherSection(context: Context) {
    val fusedLocationProviderClient =
        remember { LocationServices.getFusedLocationProviderClient(context) }
    val scope = rememberCoroutineScope()
    val apiKey = "5b7853c5cd0b459f87a63035253003"

    var cityName by remember { mutableStateOf("Fetching location...") }
    var temperature by remember { mutableStateOf("Fetching...") }
    var weatherCondition by remember { mutableStateOf("Please wait...") }
    var weatherIcon by remember { mutableStateOf(R.drawable.sun) }
    var showDialog by remember { mutableStateOf(false) }
    var nearbyCities by remember { mutableStateOf(listOf<String>()) }
    var nearbyCityWeather by remember { mutableStateOf<Map<String, Pair<String, Int>>>(emptyMap()) }
    var nearbyCityForecast by remember {
        mutableStateOf<Map<String, List<Triple<String, Int, String>>>>(emptyMap())
    }
    var isLoading by remember { mutableStateOf(true) }

    val locationPermissionState =
        rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status.isGranted) {
            getUserLocation(fusedLocationProviderClient) { city, lat, lon, cities ->
                cityName = if (city.isNotBlank()) city else "$lat, $lon"
                nearbyCities = cities.filter { it != cityName }

                fetchWeather(cityName, apiKey) { temp, condition, icon, _ ->
                    temperature = "$temp"
                    weatherCondition = condition
                    weatherIcon = icon
                    isLoading = false
                }

                scope.launch {
                    val weatherData = mutableMapOf<String, Pair<String, Int>>()
                    val forecastData = mutableMapOf<String, List<Triple<String, Int, String>>>()

                    nearbyCities.forEach { city ->
                        fetchWeather(city, apiKey) { temp, condition, icon, forecast ->
                            weatherData[city] = "$temp - $condition" to icon
                            forecastData[city] =
                                forecast.map { Triple(it.first, it.second, it.third) }

                            scope.launch {
                                nearbyCityWeather = weatherData.toMap()
                                nearbyCityForecast = forecastData.toMap()
                            }
                        }
                    }
                }
            }
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Main Weather Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { showDialog = true },
            elevation = CardDefaults.elevatedCardElevation(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF43A047),
                                Color(0xFF1B5E20)
                            )
                        )
                    )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0x30FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = weatherIcon),
                                contentDescription = "Weather Icon",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(60.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        Column {
                            Text(
                                text = cityName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = temperature,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = weatherCondition,
                                fontSize = 16.sp,
                                color = Color(0xE6FFFFFF),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            elevation = CardDefaults.elevatedCardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.sun),
                    contentDescription = "Info Icon",
                    tint = Color(0xFF0DA54B),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Tap the weather card to see forecasts for nearby cities",
                    fontSize = 14.sp,
                    color = Color(0xFF0DA54B)
                )
            }
        }

        // Weather Detail Dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                properties = DialogProperties(usePlatformDefaultWidth = false),
                containerColor = Color(0xFFEDF7F0),
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Weather Forecast",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0DA54B)
                        )
                        Divider(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth(0.8f),
                            color = Color(0x330DA54B),
                            thickness = 2.dp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        // Current location card
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            elevation = CardDefaults.elevatedCardElevation(4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF0DA54B)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Your Location",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = weatherIcon),
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color.Unspecified
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = cityName,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "$temperature | $weatherCondition",
                                            fontSize = 16.sp,
                                            color = Color(0xE6FFFFFF)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Nearby Cities Forecast",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0DA54B),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Nearby cities forecast
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                        ) {
                            items(nearbyCities) { city ->
                                val (temp, icon) = nearbyCityWeather[city] ?: ("Fetching..." to R.drawable.sun)
                                val forecastList = nearbyCityForecast[city] ?: emptyList()

                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    elevation = CardDefaults.elevatedCardElevation(4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFCCE8D1)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        // City header with gradient background
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(
                                                            Color(0xFF43A047),
                                                            Color(0xFF1B5E20)
                                                        )
                                                    )
                                                )
                                                .padding(12.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = city,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = icon),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(40.dp),
                                                        tint = Color.Unspecified
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = temp,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Forecast days
                                        val dayLabels = listOf("Tomorrow", "In 2 Days", "In 3 Days")
                                        forecastList.forEachIndexed { index, (forecastTemp, forecastIcon, forecastCondition) ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = Color(0x300DA54B)
                                                )
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(8.dp)
                                                ) {
                                                    Text(
                                                        text = dayLabels.getOrNull(index) ?: "In ${index + 1} Days",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF1B5E20),
                                                        modifier = Modifier.width(80.dp)
                                                    )

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    Icon(
                                                        painter = painterResource(id = forecastIcon),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(24.dp),
                                                        tint = Color.Unspecified
                                                    )

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    Text(
                                                        text = forecastTemp,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1B5E20)
                                                    )

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    Text(
                                                        text = forecastCondition,
                                                        fontSize = 14.sp,
                                                        color = Color(0xFF1B5E20)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0DA54B),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Close",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            )
        }
    }
}

// The utility functions remain the same:
@SuppressLint("MissingPermission")
private fun getUserLocation(fusedLocationProviderClient: FusedLocationProviderClient, onLocationRetrieved: (String, Double, Double, List<String>) -> Unit) {
    try {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude
                val detectedCity = getCityFromCoordinates(lat, lon).orEmpty()
                val nearbyCities = getNearbyCities(lat, lon)

                onLocationRetrieved(detectedCity, lat, lon, nearbyCities)
            } else {
                Log.e("LocationError", "Location is null")
            }
        }.addOnFailureListener {
            Log.e("LocationError", "Failed to get location", it)
        }
    } catch (e: SecurityException) {
        Log.e("LocationError", "Permission denied", e)
    }
}

fun getCityFromCoordinates(lat: Double, lon: Double): String? {
    return when {
        // Metro Manila
        lat in 14.50..14.70 && lon in 120.90..121.00 -> "Manila"
        lat in 14.55..14.60 && lon in 121.00..121.10 -> "Makati"
        lat in 14.66..14.74 && lon in 121.00..121.12 -> "Quezon City"
        lat in 14.52..14.60 && lon in 121.00..121.08 -> "Pasig"
        lat in 14.52..14.58 && lon in 120.98..121.02 -> "Mandaluyong"
        lat in 14.43..14.55 && lon in 120.98..121.02 -> "Parañaque"
        lat in 14.47..14.55 && lon in 120.97..121.03 -> "Taguig"
        lat in 14.68..14.75 && lon in 120.98..121.06 -> "Caloocan"
        lat in 14.63..14.72 && lon in 120.98..121.06 -> "Valenzuela"
        lat in 14.57..14.65 && lon in 121.10..121.20 -> "Marikina"
        lat in 14.38..14.50 && lon in 120.88..121.00 -> "Las Piñas"
        lat in 14.47..14.55 && lon in 120.88..121.00 -> "Muntinlupa"

        // Bulacan
        lat in 14.40..14.50 && lon in 120.85..120.95 -> "Malolos"
        lat in 14.83..14.88 && lon in 120.80..120.90 -> "Meycauayan"
        lat in 14.75..14.80 && lon in 120.90..121.00 -> "San Jose del Monte"
        lat in 14.90..14.95 && lon in 120.85..120.95 -> "Marilao"
        lat in 14.88..14.93 && lon in 120.90..121.00 -> "Santa Maria"
        lat in 14.92..14.97 && lon in 120.95..121.05 -> "Norzagaray"
        lat in 14.85..14.90 && lon in 120.90..121.00 -> "Bocaue"
        lat in 14.82..14.87 && lon in 120.90..121.00 -> "Balagtas"
        lat in 14.85..14.90 && lon in 120.85..120.95 -> "Pandi"
        lat in 14.80..14.85 && lon in 120.85..120.95 -> "Plaridel"
        lat in 14.65..14.70 && lon in 120.90..121.00 -> "Baliuag"
        lat in 14.72..14.77 && lon in 120.93..121.03 -> "San Rafael"
        lat in 14.75..14.80 && lon in 120.85..120.95 -> "San Ildefonso"
        lat in 14.78..14.83 && lon in 120.92..121.02 -> "Hagonoy"
        lat in 14.87..14.92 && lon in 120.88..120.98 -> "Angat"
        lat in 14.70..14.75 && lon in 120.85..120.95 -> "Guiguinto"
        lat in 14.72..14.77 && lon in 120.87..120.97 -> "Pulilan"
        lat in 14.68..14.73 && lon in 120.80..120.90 -> "Calumpit"
        lat in 14.80..14.85 && lon in 120.78..120.88 -> "Doña Remedios Trinidad"

        // Other Cities
        lat in 14.50..14.70 && lon in 120.90..121.00 -> "Manila"
        lat in 14.55..14.60 && lon in 121.00..121.10 -> "Makati"
        lat in 37.40..37.45 && lon in -122.09..-122.07 -> "Mountain View"

        else -> null
    }
}

fun getNearbyCities(lat: Double, lon: Double): List<String> {
    val nearbyList = mutableListOf<String>()
    val currentCity = getCityFromCoordinates(lat, lon)

    val predefinedCities = listOf(
        "Manila", "Makati", "Quezon City", "Pasig", "Mandaluyong", "Parañaque", "Taguig",
        "Caloocan", "Valenzuela", "Marikina", "Las Piñas", "Muntinlupa", "Malolos", "Meycauayan",
        "San Jose del Monte", "Marilao", "Santa Maria", "Norzagaray", "Bocaue", "Balagtas",
        "Pandi", "Plaridel", "Baliuag", "San Rafael", "San Ildefonso", "Hagonoy", "Angat",
        "Guiguinto", "Pulilan", "Calumpit", "Doña Remedios Trinidad"
    )

    // Find the 3 closest cities based on predefined list
    predefinedCities.shuffled().take(10).forEach { city ->
        if (city != currentCity) nearbyList.add(city)
    }

    return nearbyList
}

fun fetchWeather(
    cityName: String,
    apiKey: String,
    onWeatherFetched: (String, String, Int, List<Triple<String, Int, String>>) -> Unit
) {
    if (cityName.isBlank()) {
        Log.e("WeatherError", "City parameter is missing!")
        return
    }

    Log.d("WeatherDebug", "Fetching forecast for city: $cityName")

    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://api.weatherapi.com/v1/forecast.json?key=$apiKey&q=$cityName&days=3")
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("WeatherError", "Failed to fetch forecast: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            response.body?.string()?.let { responseBody ->
                try {
                    val jsonObject = JSONObject(responseBody)

                    // Handle API errors
                    jsonObject.optJSONObject("error")?.let {
                        val errorMessage = it.optString("message", "Unknown API Error")
                        Log.e("WeatherError", "API Error: $errorMessage")
                        return
                    }

                    // Get current weather details
                    val current = jsonObject.getJSONObject("current")
                    val tempC = "${current.getDouble("temp_c")}°C"
                    val conditionObj = current.getJSONObject("condition")
                    val condition = conditionObj.getString("text")
                    val iconResId = getWeatherIconResource(condition)

                    Log.d("WeatherDebug", "Current Weather - $cityName: $condition ($tempC)")

                    // Fetch 3-day forecast
                    val forecastList = mutableListOf<Triple<String, Int, String>>()
                    val forecastDays = jsonObject.getJSONObject("forecast").getJSONArray("forecastday")

                    for (i in 0 until forecastDays.length()) {
                        val dayObj = forecastDays.getJSONObject(i).getJSONObject("day")
                        val avgTemp = "${dayObj.getDouble("avgtemp_c")}°C"
                        val forecastCondition = dayObj.getJSONObject("condition").getString("text")
                        val forecastIcon = getWeatherIconResource(forecastCondition)

                        Log.d("WeatherDebug", "Forecast Day $i: $forecastCondition ($avgTemp)")

                        forecastList.add(Triple(avgTemp, forecastIcon, forecastCondition))
                    }

                    // Execute on the main thread
                    CoroutineScope(Dispatchers.Main).launch {
                        onWeatherFetched(tempC, condition, iconResId, forecastList)
                        Log.d("WeatherDebug", "Updated UI with weather data for $cityName")
                    }

                } catch (e: JSONException) {
                    Log.e("WeatherError", "Error parsing weather data: ${e.message}")
                }
            }
        }
    })
}

fun getWeatherIconResource(condition: String): Int {
    val formattedCondition = condition.lowercase().trim()

    Log.d("WeatherCondition", "Processing condition: '$condition' -> Normalized: '$formattedCondition'")

    return when {
        listOf("thunder", "storm").any { it in formattedCondition } -> R.drawable.thunder // ⚡ Thunderstorm
        "drizzle" in formattedCondition -> R.drawable.drizzle // 🌧 Drizzle
        listOf("rain", "shower").any { it in formattedCondition } -> R.drawable.rainy // 🌧 Rainy
        listOf("cloud", "overcast").any { it in formattedCondition } -> R.drawable.cloudy // ☁ Cloudy
        listOf("clear", "sunny", "sun").any { it in formattedCondition } -> R.drawable.sun // ☀ Sunny
        else -> R.drawable.sun // Default (Sunny)
    }
}