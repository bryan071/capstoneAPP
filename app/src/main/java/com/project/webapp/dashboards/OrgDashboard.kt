
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.project.webapp.Viewmodel.AuthViewModel
import com.project.webapp.dashboards.CommunityFeed
import com.project.webapp.dashboards.DiscountsBanner
import com.project.webapp.dashboards.FeaturedProductsSection
import com.project.webapp.dashboards.HeroBanner
import com.project.webapp.dashboards.SearchBar
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import com.project.webapp.components.TopBar

@Composable
fun OrganizationDashboard(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel, cartViewModel: CartViewModel) {

    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        TopBar(navController, cartViewModel, userType = "market")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SearchBar(modifier)}
            item { HeroBanner() }
            item { FeaturedProductsSection(authViewModel, navController) } // Organization-Specific Products
            item { DiscountsBanner() }
            item { WeatherSection(context) }
            item { CommunityFeed() }
        }
    }
}
