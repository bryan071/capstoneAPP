import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.components.TopBar


@Composable
fun MarketDashboard(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel, cartViewModel: CartViewModel) {

    val context = LocalContext.current
    var userType by remember { mutableStateOf<String?>(null) }

    // Simulate fetching userType from Firebase or ViewModel
    LaunchedEffect(Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            FirebaseFirestore.getInstance().collection("users")
                .document(it)
                .get()
                .addOnSuccessListener { document ->
                    userType = document.getString("userType") // Ensure field exists in Firestore
                }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        userType?.let { type ->
            TopBar(navController, cartViewModel, userType = type)
        }
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
