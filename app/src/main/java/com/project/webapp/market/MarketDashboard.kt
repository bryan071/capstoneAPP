package com.project.webapp.market

import WeatherSection
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.project.webapp.AuthViewModel
import com.project.webapp.farmers.CommunityFeed
import com.project.webapp.farmers.DiscountsBanner
import com.project.webapp.farmers.FeaturedProductsSection
import com.project.webapp.farmers.SearchBar
import com.project.webapp.farmers.TopBar

@Composable
fun MarketDashboard(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopBar()
        Text(text = "Market Dashboard")

        // Scrollable content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp), // Add slight top padding
            verticalArrangement = Arrangement.spacedBy(16.dp) // Space between sections
        ) {
            item { SearchBar() }
            item { FeaturedProductsSection(authViewModel, navController) }
            item { DiscountsBanner() }
            item { WeatherSection(context) }
            item { CommunityFeed() }
        }
    }
}