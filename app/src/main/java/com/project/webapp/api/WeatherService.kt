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
import com.project.webapp.pages.getCityFromCoordinates
import kotlin.math.*


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



