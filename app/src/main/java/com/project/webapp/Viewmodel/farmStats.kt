package com.project.webapp.Viewmodel

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
data class WeeklySalesData(val weekRange: String, val quantity: Int, val totalSalesAmount: Double)

// --- Enhanced Color Palette ---
val PrimaryGreen = Color(0xFF2E7D32)
val DarkGreen = Color(0xFF1B5E20)
val LightGreen = Color(0xFFE8F5E8)
val AccentGreen = Color(0xFF4CAF50)
val AccentYellow = Color(0xFFFFEB3B)
val GoldAccent = Color(0xFFFFC107)
val Background = Color(0xFFF8FDF8)
val CardBackground = Color(0xFFFFFFFF)
val BarFill = Color(0xFF66BB6A)
val BarBackground = Color(0xFFE8F5E8)
val TextSecondary = Color(0xFF757575)

@Composable
fun FarmStatisticsScreen(viewModel: FarmStatsViewModel = viewModel()) {
    val productCount by viewModel.productCount
    val weeklySales = viewModel.weeklySales

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section
        Text(
            text = "Farm Statistics",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            ),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Stats Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Total Products",
                value = productCount.toString(),
                icon = Icons.Default.Inventory,
                iconColor = AccentGreen,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Weekly Analytics",
                value = "${weeklySales.size}",
                icon = Icons.Default.Assessment,
                iconColor = GoldAccent,
                modifier = Modifier.weight(1f)
            )
        }

        // Weekly Sales Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(AccentGreen.copy(alpha = 0.2f), AccentGreen.copy(alpha = 0.1f))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Weekly Sales Performance",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = DarkGreen
                        )
                    )
                }

                if (weeklySales.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No sales data available",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "Start selling to see your analytics here",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                } else {
                    WeeklySalesChart(weeklySales)
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(iconColor.copy(alpha = 0.2f), iconColor.copy(alpha = 0.1f))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                ),
                fontSize = 24.sp
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            )

            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@Composable
fun WeeklySalesChart(data: List<WeeklySalesData>) {
    val maxQuantity = data.maxOfOrNull { it.quantity } ?: 1
    val maxSales = data.maxOfOrNull { it.totalSalesAmount } ?: 1.0
    val totalQuantity = data.sumOf { it.quantity }
    val totalSales = data.sumOf { it.totalSalesAmount }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Summary Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "Total Quantity",
                value = totalQuantity.toString(),
                subtitle = "items sold",
                backgroundColor = AccentGreen.copy(alpha = 0.1f),
                textColor = AccentGreen,
                modifier = Modifier.weight(1f)
            )

            SummaryCard(
                title = "Total Revenue",
                value = "₱${"%.0f".format(totalSales)}",
                subtitle = "earnings",
                backgroundColor = GoldAccent.copy(alpha = 0.1f),
                textColor = GoldAccent.copy(red = 0.8f),
                modifier = Modifier.weight(1f)
            )
        }

        Divider(
            color = LightGreen,
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Weekly Data
        data.forEach { weekData ->
            WeeklyDataItem(
                weekData = weekData,
                maxQuantity = maxQuantity,
                maxSales = maxSales
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    subtitle: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = textColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = textColor.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
fun WeeklyDataItem(
    weekData: WeeklySalesData,
    maxQuantity: Int,
    maxSales: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = weekData.weekRange,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = DarkGreen
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${weekData.quantity} sold",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AccentGreen,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quantity Bar with Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Inventory,
                contentDescription = null,
                tint = AccentGreen,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Quantity: ${weekData.quantity}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = DarkGreen,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        ProgressBar(
            progress = (weekData.quantity / maxQuantity.toFloat()).coerceIn(0f, 1f),
            backgroundColor = BarBackground,
            fillColor = BarFill
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sales Bar with Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AttachMoney,
                contentDescription = null,
                tint = GoldAccent.copy(red = 0.8f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Revenue: ₱${"%.2f".format(weekData.totalSalesAmount)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = DarkGreen,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        ProgressBar(
            progress = (weekData.totalSalesAmount / maxSales).toFloat().coerceIn(0f, 1f),
            backgroundColor = BarBackground,
            fillColor = GoldAccent
        )
    }
}

@Composable
fun ProgressBar(
    progress: Float,
    backgroundColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(fillColor.copy(alpha = 0.8f), fillColor)
                    )
                )
        )
    }
}