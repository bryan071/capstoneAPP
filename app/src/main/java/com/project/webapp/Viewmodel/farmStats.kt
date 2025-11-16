package com.project.webapp.Viewmodel

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// --- Data Classes ---
data class Product(val name: String, val id: String = "")
data class SalesData(
    val label: String,
    val quantity: Int,
    val totalSalesAmount: Double,
    val orderCount: Int = 0
)

enum class TimeRange(val displayName: String) {
    DAILY("Daily (7 Days)"),
    WEEKLY("Weekly (8 Weeks)"),
    MONTHLY("Monthly (6 Months)")
}

class FarmStatsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _productCount = mutableStateOf(0)
    val productCount: State<Int> = _productCount

    private val _salesData = mutableStateListOf<SalesData>()
    val salesData: List<SalesData> = _salesData

    private val _timeRange = mutableStateOf(TimeRange.DAILY)
    val timeRange: State<TimeRange> = _timeRange

    private val _totalRevenue = mutableStateOf(0.0)
    val totalRevenue: State<Double> = _totalRevenue

    private val _totalOrders = mutableStateOf(0)
    val totalOrders: State<Int> = _totalOrders

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    init {
        fetchProductCount()
        fetchSales()
    }

    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
        fetchSales()
    }

    private fun fetchProductCount() {
        db.collection("products")
            .whereEqualTo("ownerId", auth.currentUser?.uid)
            .addSnapshotListener { result, error ->
                if (error != null) {
                    Log.e("FarmStatsVM", "Error fetching product count: ${error.message}")
                    return@addSnapshotListener
                }
                _productCount.value = result?.size() ?: 0
            }
    }

    private fun fetchSales() {
        _isLoading.value = true
        val startDate = getStartDate(_timeRange.value)

        Log.d("FarmStatsVM", "Fetching sales since: $startDate for range: ${_timeRange.value}")

        db.collection("orders")
            .whereEqualTo("sellerId", auth.currentUser?.uid)
            .whereIn("status", listOf("Completed", "COMPLETED", "Delivered", "DELIVERED"))
            .whereGreaterThanOrEqualTo("timestamp", Timestamp(startDate))
            .addSnapshotListener { ordersSnapshot, ordersError ->
                if (ordersError != null) {
                    Log.e("FarmStatsVM", "Orders fetch error: ${ordersError.message}")
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                viewModelScope.launch(Dispatchers.Main) {
                    val salesMap = mutableMapOf<String, MutableMap<String, Any>>()
                    var revenue = 0.0
                    var orderCount = 0

                    ordersSnapshot?.documents?.forEach { orderDoc ->
                        val timestamp = orderDoc.getTimestamp("timestamp")
                        if (timestamp == null) {
                            Log.w("FarmStatsVM", "Order missing timestamp: ${orderDoc.id}")
                            return@forEach
                        }

                        val periodKey = getPeriodKey(timestamp.toDate().time, _timeRange.value)

                        // Initialize period if not exists
                        if (!salesMap.containsKey(periodKey)) {
                            salesMap[periodKey] = mutableMapOf(
                                "quantity" to 0,
                                "amount" to 0.0,
                                "orders" to 0
                            )
                        }

                        val period = salesMap[periodKey]!!
                        orderCount++

                        // Fetch order items from subcollection
                        db.collection("orders")
                            .document(orderDoc.id)
                            .collection("order_items")
                            .get()
                            .addOnSuccessListener { itemsSnapshot ->
                                if (!itemsSnapshot.isEmpty) {
                                    // New system: order_items subcollection
                                    itemsSnapshot.documents.forEach { itemDoc ->
                                        val quantity = (itemDoc.get("quantity") as? Number)?.toInt() ?: 0
                                        val price = (itemDoc.get("price") as? Number)?.toDouble() ?: 0.0

                                        period["quantity"] = (period["quantity"] as Int) + quantity
                                        period["amount"] = (period["amount"] as Double) + (price * quantity)
                                        revenue += (price * quantity)
                                    }
                                    period["orders"] = (period["orders"] as Int) + 1
                                } else {
                                    // Fallback: Old system with items array
                                    val items = orderDoc.get("items") as? List<Map<String, Any>>
                                    if (!items.isNullOrEmpty()) {
                                        items.forEach { item ->
                                            val quantity = parseQuantity(item["quantity"])
                                            val price = parsePrice(item["price"])

                                            period["quantity"] = (period["quantity"] as Int) + quantity
                                            period["amount"] = (period["amount"] as Double) + (price * quantity)
                                            revenue += (price * quantity)
                                        }
                                        period["orders"] = (period["orders"] as Int) + 1
                                    } else {
                                        // Last fallback: Direct order fields
                                        val quantity = (orderDoc.getLong("quantity") ?: 0).toInt()
                                        val totalAmount = orderDoc.getDouble("totalAmount") ?: 0.0

                                        if (quantity > 0 && totalAmount > 0) {
                                            period["quantity"] = (period["quantity"] as Int) + quantity
                                            period["amount"] = (period["amount"] as Double) + totalAmount
                                            revenue += totalAmount
                                            period["orders"] = (period["orders"] as Int) + 1
                                        }
                                    }
                                }

                                // Update UI after processing all orders
                                updateSalesData(salesMap)
                                _totalRevenue.value = revenue
                                _totalOrders.value = orderCount
                                _isLoading.value = false
                            }
                            .addOnFailureListener { e ->
                                Log.e("FarmStatsVM", "Failed to fetch order items", e)
                                _isLoading.value = false
                            }
                    }

                    if (ordersSnapshot?.isEmpty == true) {
                        _salesData.clear()
                        _totalRevenue.value = 0.0
                        _totalOrders.value = 0
                        _isLoading.value = false
                    }
                }
            }
    }

    private fun updateSalesData(salesMap: Map<String, Map<String, Any>>) {
        val sortedSales = salesMap.entries
            .sortedBy { getPeriodTimestamp(it.key, _timeRange.value) }
            .map { entry ->
                SalesData(
                    label = formatPeriodLabel(entry.key, _timeRange.value),
                    quantity = entry.value["quantity"] as Int,
                    totalSalesAmount = entry.value["amount"] as Double,
                    orderCount = entry.value["orders"] as Int
                )
            }

        _salesData.clear()
        _salesData.addAll(sortedSales)

        Log.d("FarmStatsVM", "Updated sales data: ${sortedSales.size} periods")
    }

    private fun getStartDate(range: TimeRange): Date {
        val cal = Calendar.getInstance().apply {
            time = Date()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            when (range) {
                TimeRange.DAILY -> add(Calendar.DAY_OF_YEAR, -6) // Last 7 days
                TimeRange.WEEKLY -> add(Calendar.WEEK_OF_YEAR, -7) // Last 8 weeks
                TimeRange.MONTHLY -> add(Calendar.MONTH, -5) // Last 6 months
            }
        }
        return cal.time
    }

    private fun getPeriodKey(timestamp: Long, range: TimeRange): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return when (range) {
            TimeRange.DAILY -> {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
            TimeRange.WEEKLY -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis.toString()
            }
            TimeRange.MONTHLY -> {
                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}"
            }
        }
    }

    private fun getPeriodTimestamp(key: String, range: TimeRange): Long {
        return when (range) {
            TimeRange.DAILY -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(key)?.time ?: 0L
            TimeRange.WEEKLY -> key.toLongOrNull() ?: 0L
            TimeRange.MONTHLY -> {
                val parts = key.split("-")
                if (parts.size == 2) {
                    Calendar.getInstance().apply {
                        set(Calendar.YEAR, parts[0].toInt())
                        set(Calendar.MONTH, parts[1].toInt() - 1)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }.timeInMillis
                } else 0L
            }
        }
    }

    private fun formatPeriodLabel(key: String, range: TimeRange): String {
        return when (range) {
            TimeRange.DAILY -> {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(key)
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(date ?: Date())
            }
            TimeRange.WEEKLY -> {
                val startDate = Date(key.toLongOrNull() ?: 0L)
                val endDate = Calendar.getInstance().apply {
                    time = startDate
                    add(Calendar.DAY_OF_WEEK, 6)
                }.time
                val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
                "${formatter.format(startDate)} - ${formatter.format(endDate)}"
            }
            TimeRange.MONTHLY -> {
                val parts = key.split("-")
                if (parts.size == 2) {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, parts[0].toInt())
                        set(Calendar.MONTH, parts[1].toInt() - 1)
                    }
                    SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.time)
                } else key
            }
        }
    }

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
}

// --- Enhanced Color Palette ---
val PrimaryGreen = Color(0xFF2E7D32)
val DarkGreen = Color(0xFF1B5E20)
val LightGreen = Color(0xFFE8F5E8)
val AccentGreen = Color(0xFF4CAF50)
val GoldAccent = Color(0xFFFFC107)
val Background = Color(0xFFF8FDF8)
val CardBackground = Color(0xFFFFFFFF)
val BarFill = Color(0xFF66BB6A)
val BarBackground = Color(0xFFE8F5E8)
val TextSecondary = Color(0xFF757575)

@Composable
fun FarmStatisticsScreen(viewModel: FarmStatsViewModel = viewModel()) {
    val productCount by viewModel.productCount
    val salesData = viewModel.salesData
    val timeRange by viewModel.timeRange
    val totalRevenue by viewModel.totalRevenue
    val totalOrders by viewModel.totalOrders
    val isLoading by viewModel.isLoading

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
                title = "Products",
                value = productCount.toString(),
                icon = Icons.Default.Inventory,
                iconColor = AccentGreen,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Revenue",
                value = "₱${"%.0f".format(totalRevenue)}",
                subtitle = "${timeRange.displayName}",
                icon = Icons.Default.AttachMoney,
                iconColor = GoldAccent,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Orders",
                value = totalOrders.toString(),
                subtitle = "${timeRange.displayName}",
                icon = Icons.Default.ShoppingBag,
                iconColor = Color(0xFF2196F3),
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Avg. Order",
                value = if (totalOrders > 0) "₱${"%.0f".format(totalRevenue / totalOrders)}" else "₱0",
                subtitle = "per order",
                icon = Icons.Default.TrendingUp,
                iconColor = Color(0xFF9C27B0),
                modifier = Modifier.weight(1f)
            )
        }

        // Time Range Selector
        TimeRangeSelector(
            selectedRange = timeRange,
            onRangeSelected = { viewModel.setTimeRange(it) }
        )

        // Sales Chart
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
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Sales Performance",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = DarkGreen
                        )
                    )
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentGreen)
                    }
                } else if (salesData.isEmpty()) {
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
                    SalesChart(salesData)
                }
            }
        }
    }
}

@Composable
fun TimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeRange.values().forEach { range ->
                val isSelected = selectedRange == range
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onRangeSelected(range) },
                    color = if (isSelected) AccentGreen else Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = range.displayName.split("(")[0].trim(),
                            color = if (isSelected) Color.White else Color.DarkGray,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
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
        modifier = modifier.shadow(2.dp, RoundedCornerShape(12.dp)),
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
                fontSize = 20.sp
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
fun SalesChart(data: List<SalesData>) {
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

        // Period Data
        data.forEach { periodData ->
            PeriodDataItem(
                periodData = periodData,
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
fun PeriodDataItem(
    periodData: SalesData,
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
            Column {
                Text(
                    text = periodData.label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = DarkGreen
                    )
                )
                Text(
                    text = "${periodData.orderCount} order${if (periodData.orderCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }

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
                    text = "${periodData.quantity} sold",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AccentGreen,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quantity Bar
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
                text = "Quantity: ${periodData.quantity}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = DarkGreen,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        ProgressBar(
            progress = if (maxQuantity > 0) (periodData.quantity / maxQuantity.toFloat()).coerceIn(0f, 1f) else 0f,
            backgroundColor = BarBackground,
            fillColor = BarFill
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sales Bar
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
                text = "Revenue: ₱${"%.2f".format(periodData.totalSalesAmount)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = DarkGreen,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        ProgressBar(
            progress = if (maxSales > 0) (periodData.totalSalesAmount / maxSales).toFloat().coerceIn(0f, 1f) else 0f,
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
            .background(backgroundColor, RoundedCornerShape(4.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(8.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(fillColor.copy(alpha = 0.8f), fillColor)
                    ),
                    shape = RoundedCornerShape(4.dp)
                )
        )
    }
}