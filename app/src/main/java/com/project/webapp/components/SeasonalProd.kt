package com.project.webapp.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.project.webapp.R
import com.project.webapp.dashboards.ProductCard
import com.project.webapp.dashboards.fetchProducts
import com.project.webapp.datas.Product
import java.util.*

// Season-specific product mappings for Philippines
object PhilippineSeasons {
    // Dry Season (Summer): March to May
    val DRY_SEASON_MONTHS = listOf(3, 4, 5)

    // Wet Season (Rainy): June to November
    val WET_SEASON_MONTHS = listOf(6, 7, 8, 9, 10, 11)

    // Cool Dry Season: December to February
    val COOL_DRY_MONTHS = listOf(12, 1, 2)

    // Products that thrive in dry/summer season
    val DRY_SEASON_PRODUCTS = listOf(
        "mango", "watermelon", "pineapple", "banana", "papaya",
        "melon", "cucumber", "tomato", "eggplant", "okra",
        "squash", "bitter gourd", "string beans", "sweet potato",
        "corn", "peanut", "mongo", "garlic", "onion"
    )

    // Products that thrive in wet/rainy season
    val WET_SEASON_PRODUCTS = listOf(
        "rice", "taro", "gabi", "kangkong", "pechay",
        "lettuce", "cabbage", "broccoli", "cauliflower",
        "green beans", "snap peas", "ginger", "turmeric",
        "lemongrass", "spinach", "bok choy", "radish",
        "carrots", "beets", "mushroom"
    )
}

enum class Season(
    val displayName: String,
    val icon: Int,
    val gradient: List<Color>,
    val months: List<Int>
) {
    DRY_SEASON(
        "Summer Season",
        R.drawable.arrowup, // Replace with sun icon
        listOf(Color(0xFFFFB74D), Color(0xFFFF9800)),
        PhilippineSeasons.DRY_SEASON_MONTHS
    ),
    WET_SEASON(
        "Rainy Season",
        R.drawable.arrowdown, // Replace with rain/cloud icon
        listOf(Color(0xFF64B5F6), Color(0xFF1976D2)),
        PhilippineSeasons.WET_SEASON_MONTHS
    ),
    COOL_DRY(
        "Cool Season",
        R.drawable.arrowup, // Replace with cool weather icon
        listOf(Color(0xFF81C784), Color(0xFF4CAF50)),
        PhilippineSeasons.COOL_DRY_MONTHS
    )
}

@Composable
fun SeasonalProductsSection(
    navController: NavController,
    firestore: FirebaseFirestore,
    storage: FirebaseStorage
) {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch products from Firestore
    DisposableEffect(Unit) {
        val listenerRegistration = fetchProducts(firestore) { fetchedProducts ->
            products = fetchedProducts
            isLoading = false
        }

        onDispose {
            listenerRegistration.remove()
        }
    }

    val currentSeason = remember { getCurrentSeason() }

    var selectedSeason by remember { mutableStateOf(currentSeason) }
    val displayProducts = remember(products, selectedSeason) {
        filterSeasonalProducts(products, selectedSeason)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Show loading state
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF0DA54B))
            }
            return@Column
        }

        // Header with season indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Seasonal Products",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )

                Text(
                    text = "Fresh picks for ${selectedSeason.displayName.lowercase()}",
                    fontSize = 14.sp,
                    color = Color(0xFF757575),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Season selector tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(Season.values()) { season ->
                SeasonTab(
                    season = season,
                    isSelected = selectedSeason == season,
                    isCurrent = season == currentSeason,
                    onClick = { selectedSeason = season }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Products display
        AnimatedVisibility(
            visible = displayProducts.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayProducts.take(10)) { product ->
                    Box(modifier = Modifier.width(180.dp)) {
                        ProductCard(
                            product = product,
                            navController = navController,
                            firestore = firestore,
                            storage = storage
                        )
                    }
                }
            }
        }

        // Empty state
        if (displayProducts.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = selectedSeason.icon),
                            contentDescription = null,
                            tint = Color(0xFF9E9E9E),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No seasonal products available",
                            fontSize = 14.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonTab(
    season: Season,
    isSelected: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color.Transparent else Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isSelected) {
                        Modifier.background(
                            Brush.horizontalGradient(season.gradient)
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = season.icon),
                    contentDescription = season.displayName,
                    tint = if (isSelected) Color.White else Color(0xFF757575),
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = season.displayName,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else Color(0xFF757575)
                )

                // Current season badge
                if (isCurrent && !isSelected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(season.gradient.first())
                    )
                }
            }
        }
    }
}

// Helper function to determine current season based on month
fun getCurrentSeason(): Season {
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

    return when (currentMonth) {
        in PhilippineSeasons.DRY_SEASON_MONTHS -> Season.DRY_SEASON
        in PhilippineSeasons.WET_SEASON_MONTHS -> Season.WET_SEASON
        in PhilippineSeasons.COOL_DRY_MONTHS -> Season.COOL_DRY
        else -> Season.DRY_SEASON
    }
}

// Filter products based on season
fun filterSeasonalProducts(products: List<Product>, season: Season): List<Product> {
    val seasonalKeywords = when (season) {
        Season.DRY_SEASON -> PhilippineSeasons.DRY_SEASON_PRODUCTS
        Season.WET_SEASON -> PhilippineSeasons.WET_SEASON_PRODUCTS
        Season.COOL_DRY -> PhilippineSeasons.WET_SEASON_PRODUCTS // Cool season similar to wet
    }

    return products.filter { product ->
        seasonalKeywords.any { keyword ->
            product.name.contains(keyword, ignoreCase = true) ||
                    product.description.contains(keyword, ignoreCase = true) ||
                    product.category.contains(keyword, ignoreCase = true)
        }
    }.sortedByDescending { it.quantity } // Prioritize products with more stock
}