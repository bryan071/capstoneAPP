import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.project.webapp.AuthViewModel
import com.project.webapp.dashboards.CommunityFeed
import com.project.webapp.dashboards.DiscountsBanner
import com.project.webapp.dashboards.FeaturedProductsSection
import com.project.webapp.dashboards.HeroBanner
import com.project.webapp.dashboards.SearchBar
import com.project.webapp.dashboards.TopBar
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding


@Composable
fun MarketDashboard(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {

    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        TopBar()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SearchBar() }
            item { HeroBanner() }
            item { FeaturedProductsSection(authViewModel, navController) }
            item { DiscountsBanner() }
            item { WeatherSection(context) }
            item { CommunityFeed() }
        }
    }
}
