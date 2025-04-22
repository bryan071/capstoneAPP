package com.project.webapp.components.payment

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.R
import com.project.webapp.components.createDonationNotification
import com.project.webapp.datas.CartItem
import com.project.webapp.datas.Organization
import com.project.webapp.datas.Product
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.collections.forEach
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationScreen(
    navController: NavController,
    cartViewModel: CartViewModel,
    directBuyProductId: String? = null,
    directBuyPrice: String? = null
) {
    val cartItems by cartViewModel.cartItems.collectAsStateWithLifecycle()
    val cartTotalState by cartViewModel.totalCartPrice.collectAsState()
    val currentUser by cartViewModel.currentUser.collectAsStateWithLifecycle()
    val sellerNames = remember { mutableStateMapOf<String, String>() }
    val userType = if (directBuyProductId != null) "direct_donation" else "cart_donation"
    var directBuyProduct by remember { mutableStateOf<Product?>(null) }
    var selectedOrganization by remember { mutableStateOf<Organization?>(null) }
    var showDonationReceipt by remember { mutableStateOf(false) }

    // Define the theme color
    val themeColor = Color.Unspecified

    // Sample list of organizations
    val organizations = remember {
        listOf(
            Organization("1", "Red Cross", "Humanitarian organization providing emergency assistance", R.drawable.redcross_icon),
            Organization("2", "Food for the Hungry", "Hungry children urgently need your help. Flip the odds against hunger today.", R.drawable.fh_icon),
            Organization("3", "Central Kitchen Valenzuela", "Organization providing food for needy families", R.drawable.val_logo),
            Organization("4", "Angat Kabataan", "Lead the Change, Be the Change", R.drawable.angat_kabataan)
        )
    }

    // Fetch direct buy product
    LaunchedEffect(directBuyProductId) {
        directBuyProductId?.let { id ->
            cartViewModel.getProductById(id) { product ->
                directBuyProduct = product
            }
        }
    }

    // Prepare items to display
    val displayItems: List<CartItem> = remember(directBuyProduct, cartItems) {
        if (directBuyProduct != null) {
            listOf(
                CartItem(
                    productId = directBuyProduct!!.prodId,
                    name = directBuyProduct!!.name,
                    price = directBuyProduct!!.price,
                    quantity = 1,
                    imageUrl = directBuyProduct!!.imageUrl,
                    isDirectBuy = true
                )
            )
        } else {
            cartItems
        }
    }

    // Fetch seller info for each item and map it by productId
    LaunchedEffect(displayItems) {
        displayItems.forEach { item ->
            cartViewModel.getProductById(item.productId) { product ->
                product?.let { prod ->
                    cartViewModel.getUserById(prod.ownerId) { user ->
                        user?.let {
                            sellerNames[prod.prodId] = "${it.firstname} ${it.lastname}"
                        }
                    }
                }
            }
        }
    }

    if (showDonationReceipt) {
        DonationReceiptScreen(
            navController = navController,
            cartViewModel = cartViewModel,
            cartItems = displayItems,
            organization = selectedOrganization!!,
            sellerNames = sellerNames
        )
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Donation") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.backbtn),
                                contentDescription = "Back",
                                tint = Color.Unspecified
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF8F8F8))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Section: Donation Introduction
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(40.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Make a Difference",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "Your donation can help someone in need",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }

                    // Section: Items to donate
                    SectionTitle(title = "Items to Donate (${displayItems.size})")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            displayItems.forEachIndexed { index, item ->
                                ItemCard(
                                    item = item,
                                    sellerName = sellerNames[item.productId] ?: "Loading seller...",
                                    themeColor = themeColor
                                )

                                if (index < displayItems.size - 1) {
                                    Divider(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        color = Color(0xFFEEEEEE)
                                    )
                                }
                            }
                        }
                    }

                    // Section: Select organization
                    SectionTitle(title = "Select an Organization")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            organizations.forEach { org ->
                                OrganizationItem(
                                    organization = org,
                                    isSelected = selectedOrganization?.id == org.id,
                                    themeColor = themeColor,
                                    onSelect = { selectedOrganization = org }
                                )
                            }
                        }
                    }

                    // Section: Shipping Info
                    currentUser?.let { user ->
                        SectionTitle(title = "Your Information")

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = themeColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "${user.firstname} ${user.lastname}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = null,
                                        tint = themeColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        user.address,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = themeColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        user.phoneNumber,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    // Donation method (GCash only)
                    SectionTitle(title = "Donation Method")

                    Button(
                        onClick = {
                            if (selectedOrganization != null && displayItems.isNotEmpty()) {
                                val firstItem = displayItems[0]
                                cartViewModel.getProductById(firstItem.productId) { product ->
                                    product?.let {
                                        val sellerId = it.ownerId
                                        val priceToPay = directBuyPrice ?: cartTotalState
                                        navController.navigate("gcashScreen/$priceToPay/$sellerId")
                                    }
                                }
                            }
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF)),
                        enabled = selectedOrganization != null
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Proceed with GCash",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }

// For testing purposes, direct complete button
                    OutlinedButton(
                        onClick = {
                            if (selectedOrganization != null) {
                                showDonationReceipt = true

                                // Create donation records and notifications when using the test button
                                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@OutlinedButton
                                val firestore = FirebaseFirestore.getInstance()

                                selectedOrganization?.let { organization ->
                                    displayItems.forEach { cartItem ->
                                        cartViewModel.getProductById(cartItem.productId) { product ->
                                            product?.let { prod ->
                                                // Now use organization instead of selectedOrganization
                                                createDonationNotification(
                                                    firestore = firestore,
                                                    product = prod,
                                                    donatorId = userId,
                                                    organizationName = organization.name,
                                                    quantity = cartItem.quantity,
                                                    message = "Your product was donated to ${organization.name}. " +
                                                            "Thank you for supporting sustainable agriculture and helping those in need!"
                                                )

                                                // Update product inventory
                                                updateProductInventory(
                                                    prod.prodId,
                                                    cartItem.quantity
                                                )

                                                // Create donation record
                                                createDonationRecord(
                                                    userId = userId,
                                                    productId = prod.prodId,
                                                    productName = prod.name,
                                                    organizationId = organization.id,
                                                    organizationName = organization.name,
                                                    quantity = cartItem.quantity
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedOrganization != null
                    ) {
                        Text("Complete Donation (For Testing)")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun OrganizationItem(
    organization: Organization,
    isSelected: Boolean,
    themeColor: Color,
    onSelect: () -> Unit
) {
    val borderModifier = if (isSelected) {
        Modifier.border(2.dp, themeColor, RoundedCornerShape(8.dp))
    } else {
        Modifier
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .then(borderModifier)
            .clickable { onSelect() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Organization icon/image
            Image(
                painter = painterResource(id = organization.iconResId),
                contentDescription = organization.name,
                modifier = Modifier
                    .size(40.dp)
            )


            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = organization.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = organization.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = themeColor
                )
            }
        }
    }
}

// Create donation record function
private fun createDonationRecord(
    userId: String,
    productId: String,
    productName: String,
    organizationId: String,
    organizationName: String,
    quantity: Int
) {
    val firestore = FirebaseFirestore.getInstance()

    val donationData = hashMapOf(
        "userId" to userId,
        "productId" to productId,
        "productName" to productName,
        "organizationId" to organizationId,
        "organizationName" to organizationName,
        "quantity" to quantity,
        "timestamp" to System.currentTimeMillis(),
        "status" to "completed"
    )

    firestore.collection("donations")
        .add(donationData)
        .addOnSuccessListener { documentReference ->
            Log.d("Donations", "Donation record created with ID: ${documentReference.id}")
        }
        .addOnFailureListener { e ->
            Log.e("Donations", "Error creating donation record", e)
        }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationReceiptScreen(
    navController: NavController,
    cartViewModel: CartViewModel,
    cartItems: List<CartItem>,
    organization: Organization,
    sellerNames: Map<String, String>
) {
    // Define the theme color to match other screens
    val themeColor = Color(0xFF0DA54B)
    val currentUser by cartViewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Donation Complete") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate("market") {
                            popUpTo("market") { inclusive = true }
                        }
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.backbtn),
                            contentDescription = "Back to market",
                            tint = Color.Unspecified
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F8F8))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Success card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(72.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Thank You for Your Donation!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Your items have been donated to ${organization.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        // Generate random donation number
                        val donationNumber = remember {
                            val random = Random.nextInt(100000, 999999)
                            "DON-$random"
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Donation #: ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Text(
                                text = donationNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                // Organization section
                SectionTitle(title = "Organization Details")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationCity,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                organization.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            organization.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Donated items section
                SectionTitle(title = "Donated Items (${cartItems.size})")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        cartItems.forEachIndexed { index, item ->
                            val sellerName = sellerNames[item.productId] ?: "Loading..."

                            ReceiptItemCard(
                                item = item,
                                sellerName = sellerName,
                                themeColor = themeColor
                            )

                            if (index < cartItems.size - 1) {
                                Divider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    color = Color(0xFFEEEEEE)
                                )
                            }
                        }
                    }
                }

                // Donor Information
                currentUser?.let { user ->
                    SectionTitle(title = "Donor Information")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${user.firstname} ${user.lastname}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    user.phoneNumber,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Payment info
                SectionTitle(title = "Payment Information")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PriceLine(
                            title = "Payment Method",
                            value = "GCash",
                            themeColor = themeColor
                        )

                        PriceLine(
                            title = "Donation Date",
                            value = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Calendar.getInstance().time),
                            themeColor = themeColor
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            navController.navigate("market") {
                                popUpTo("market") { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Return to Home",
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
