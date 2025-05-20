package com.project.webapp.Viewmodel

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.compose.runtime.getValue
import androidx.compose.runtime.State
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.Dispatchers

class FarmStatsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _productCount = mutableStateOf(0)
    val productCount: State<Int> = _productCount

    private val _weeklySales = mutableStateListOf<WeeklySalesData>()
    val weeklySales: List<WeeklySalesData> = _weeklySales

    init {
        fetchProductCount()
        fetchWeeklySales()
    }

    private fun fetchProductCount() {
        db.collection("products")
            .whereEqualTo("ownerId", auth.currentUser?.uid)
            .get()
            .addOnSuccessListener { result ->
                _productCount.value = result.size()
            }
            .addOnFailureListener { e ->
                Log.e("FarmStatsVM", "Error fetching product count: ${e.message}")
            }
    }

    private fun fetchWeeklySales() {
        val cal = Calendar.getInstance().apply {
            time = Date()
            add(Calendar.WEEK_OF_YEAR, -6)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTimestamp = Timestamp(cal.time)
        Log.d("FarmStatsVM", "Fetching transactions since: $startTimestamp")

        db.collection("transactions")
            .whereEqualTo("status", "Completed")
            .whereGreaterThan("timestamp", startTimestamp)
            .get()
            .addOnSuccessListener { result ->
                viewModelScope.launch(Dispatchers.Main) {
                    val userProducts = getUserProducts()
                    val userProductIds = userProducts.map { it.id }.toSet()
                    val productNames = userProducts.map { it.name }.toSet()
                    val weeklyQuantityMap = mutableMapOf<Long, Int>()
                    val weeklySalesAmountMap = mutableMapOf<Long, Double>()

                    Log.d("FarmStatsVM", "Fetched ${result.size()} transactions")

                    for (doc in result) {
                        val timestampObj = doc.getTimestamp("timestamp")
                        if (timestampObj == null) {
                            Log.w("FarmStatsVM", "Transaction missing timestamp: ${doc.id}")
                            continue
                        }
                        val weekStart = getWeekStartMillis(timestampObj.toDate().time)
                        Log.d("FarmStatsVM", "Transaction ${doc.id} timestamp weekStart: $weekStart")

                        // --- Cart checkout path ---
                        val items = doc.get("items") as? List<Map<String, Any>>
                        if (items != null) {
                            for (item in items) {
                                val productId = item["productId"] as? String ?: continue
                                if (productId !in userProductIds) continue

                                val quantity = parseQuantity(item["quantity"])
                                val price = parsePrice(item["price"])
                                Log.d("FarmStatsVM", "Item productId=$productId quantity=$quantity price=$price")

                                weeklyQuantityMap[weekStart] = (weeklyQuantityMap[weekStart] ?: 0) + quantity
                                weeklySalesAmountMap[weekStart] = (weeklySalesAmountMap[weekStart] ?: 0.0) + (price * quantity)
                            }
                            continue
                        }

                        // --- Direct-buy path ---
                        val itemName = doc.getString("item")
                        val quantity = (doc.getLong("quantity") ?: 0L).toInt()
                        val price = doc.getDouble("price") ?: 0.0

                        if (itemName != null && itemName in productNames) {
                            weeklyQuantityMap[weekStart] = (weeklyQuantityMap[weekStart] ?: 0) + quantity
                            weeklySalesAmountMap[weekStart] = (weeklySalesAmountMap[weekStart] ?: 0.0) + (price * quantity)
                        }
                    }

                    val sortedWeeklySales = weeklyQuantityMap.entries.sortedBy { it.key }.map {
                        val salesAmount = weeklySalesAmountMap[it.key] ?: 0.0
                        WeeklySalesData(formatWeekRange(it.key), it.value, salesAmount)
                    }

                    _weeklySales.clear()
                    _weeklySales.addAll(sortedWeeklySales)

                    Log.d("FarmStatsVM", "Weekly sales updated: $sortedWeeklySales")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FarmStatsVM", "Failed to fetch weekly sales: ${e.message}")
            }
    }

    // Helper to parse price safely
    private fun parsePrice(value: Any?): Double {
        return when (value) {
            is Double -> value
            is Long -> value.toDouble()
            is Int -> value.toDouble()
            else -> 0.0
        }
    }

    private fun parseQuantity(value: Any?): Int {
        return when(value) {
            is Long -> value.toInt()
            is Double -> value.toInt()
            is Int -> value
            else -> 0
        }
    }

    private suspend fun getUserProducts(): List<Product> = suspendCoroutine { cont ->
        db.collection("products")
            .whereEqualTo("ownerId", auth.currentUser?.uid)
            .get()
            .addOnSuccessListener { result ->
                val products = result.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    Product(name, doc.id)
                }
                cont.resume(products)
            }
            .addOnFailureListener {
                cont.resume(emptyList())
            }
    }

    private fun getWeekStartMillis(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            time = Date(timestamp)
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun formatWeekRange(startMillis: Long): String {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startMillis
        }
        val start = cal.time

        cal.add(Calendar.DAY_OF_WEEK, 6)
        val end = cal.time

        val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
        return "${formatter.format(start)} - ${formatter.format(end)}"
    }
}


// --- Data Classes ---
data class Product(val name: String, val id: String = "")
data class WeeklySalesData(val weekRange: String, val quantity: Int,  val totalSalesAmount: Double)

// --- Composable UI ---
val PrimaryGreen = Color(0xFF2E7D32)
val LightGreen = Color(0xFFC8E6C9)
val AccentYellow = Color(0xFFFFF59D)
val Background = Color(0xFFF1F8E9)
val BarFill = Color(0xFF66BB6A)
val BarBackground = Color(0xFFD7EDD4)

@Composable
fun FarmStatisticsScreen(viewModel: FarmStatsViewModel = viewModel()) {
    val productCount by viewModel.productCount
    val weeklySales = viewModel.weeklySales

    Column(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth()
            .background(Background, shape = RoundedCornerShape(16.dp))
            .padding(24.dp)
    ) {
        Text(
            "Total Products Posted",
            style = MaterialTheme.typography.titleMedium.copy(color = PrimaryGreen)
        )
        Text(
            "$productCount",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryGreen
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Divider(color = PrimaryGreen.copy(alpha = 0.3f), thickness = 1.dp)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Weekly Product Sales",
            style = MaterialTheme.typography.titleMedium.copy(color = PrimaryGreen),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (weeklySales.isEmpty()) {
            Text(
                "No sales data available",
                style = MaterialTheme.typography.bodyMedium.copy(color = PrimaryGreen.copy(alpha = 0.7f)),
                modifier = Modifier.padding(top = 20.dp)
            )
        } else {
            WeeklySalesChart(weeklySales)
        }
    }
}

@Composable
fun WeeklySalesChart(data: List<WeeklySalesData>) {
    val maxQuantity = data.maxOfOrNull { it.quantity } ?: 1
    val maxSales = data.maxOfOrNull { it.totalSalesAmount } ?: 1.0

    Column(modifier = Modifier.fillMaxWidth()) {
        data.forEach { weekData ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = weekData.weekRange,
                    style = MaterialTheme.typography.bodySmall.copy(color = PrimaryGreen),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Quantity bar
                Text("Quantity Sold: ${weekData.quantity}", color = PrimaryGreen)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BarBackground)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((weekData.quantity / maxQuantity.toFloat()).coerceIn(0f, 1f))
                            .height(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BarFill)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sales amount bar
                Text("Total Sales: ₱${"%.2f".format(weekData.totalSalesAmount)}", color = PrimaryGreen)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BarBackground)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((weekData.totalSalesAmount / maxSales).toFloat().coerceIn(0f, 1f))
                            .height(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentYellow)
                    )
                }
            }
        }
    }
}


