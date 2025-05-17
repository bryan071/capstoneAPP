package com.project.webapp.components.payment

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.project.webapp.BuildConfig
import com.project.webapp.R
import com.project.webapp.datas.CartItem
import com.project.webapp.datas.Organization
import com.project.webapp.datas.UserData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    navController: NavController,
    cartViewModel: CartViewModel,
    cartItems: List<CartItem>,
    totalPrice: Double,
    userType: String,
    sellerNames: Map<String, String>,
    paymentMethod: String,
    referenceId: String,
    organization: Organization?,
    isDonation: Boolean,
    orderNumber: String,
    modifier: Modifier = Modifier
) {
    val currentUser by cartViewModel.currentUser.collectAsStateWithLifecycle()
    val themeColor = Color(0xFF0DA54B)

    // Cached formatters and date/time
    val dateFormatter = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val currentDateTime = remember { Date() }
    val currentDate = dateFormatter.format(currentDateTime)
    val currentTime = timeFormatter.format(currentDateTime)

    // Validate inputs early
    if (!validateInputs(cartItems, totalPrice, orderNumber, isDonation, organization)) {
        navController.popBackStack()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isDonation) "Donation Receipt" else "Receipt") },
                navigationIcon = {
                    IconButton(onClick = { /* Disabled */ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.backbtn),
                            contentDescription = null,
                            tint = Color.Transparent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .background(Color(0xFFF8F8F8))
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SuccessHeader(isDonation = isDonation, themeColor = themeColor)
            ReceiptCard(
                isDonation = isDonation,
                orderNumber = orderNumber,
                currentDate = currentDate,
                currentTime = currentTime,
                currentUser = currentUser,
                cartItems = cartItems,
                sellerNames = sellerNames,
                themeColor = themeColor,
                organization = organization
            )
            if (isDonation && organization == null) {
                navController.popBackStack()
                return@Column
            }
            PaymentSummaryCard(
                isDonation = isDonation,
                organization = organization,
                totalPrice = totalPrice,
                paymentMethod = paymentMethod,
                referenceId = referenceId,
                themeColor = themeColor
            )
            DoneButton(
                navController = navController,
                themeColor = themeColor,
                cartViewModel = cartViewModel
            )
        }
    }
}

@Composable
private fun SuccessHeader(isDonation: Boolean, themeColor: Color) {
    // Adding vertical scrolling to the Column
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(top = 16.dp)
            .verticalScroll(rememberScrollState())  // Make the column scrollable
    ) {
        Text(
            text = "✓",
            style = MaterialTheme.typography.headlineLarge,
            color = themeColor,
            modifier = Modifier
                .size(60.dp)
                .background(Color.White, CircleShape)
                .padding(12.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isDonation) "Donation Confirmed!" else "Order Confirmed!",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isDonation) {
                "Your donation has been successfully processed."
            } else {
                "Your order has been placed successfully."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}


@Composable
private fun ReceiptCard(
    isDonation: Boolean,
    orderNumber: String,
    currentDate: String,
    currentTime: String,
    currentUser: UserData?,
    cartItems: List<CartItem>,
    sellerNames: Map<String, String>,
    themeColor: Color,
    organization: Organization?

) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isDonation) "Donation #$orderNumber" else "Order #$orderNumber",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currentDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = currentTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text(
                text = if (isDonation) "Donor Details" else "Customer Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (currentUser != null) {
                if (isDonation) {
                    Text(
                        text = organization!!.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "${currentUser.firstname} ${currentUser.lastname}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = currentUser.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = currentUser.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Text(
                    text = "Loading user information...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text(
                text = if (isDonation) "Donated Items" else "Order Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            cartItems.forEach { item ->
                CartItemRow(
                    item = item,
                    sellerNames = sellerNames,
                    isDonation = isDonation
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    sellerNames: Map<String, String>,
    isDonation: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(3f)) {
            Text(
                text = item.name.orEmpty(),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (isDonation) {
                    "From: ${sellerNames[item.productId].orEmpty()}"
                } else {
                    "Seller: ${sellerNames[item.productId].orEmpty()}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        Text(
            text = "x${item.quantity}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = "₱${"%.2f".format(item.price * item.quantity)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun PaymentSummaryCard(
    isDonation: Boolean,
    organization: Organization?,
    totalPrice: Double,
    paymentMethod: String,
    referenceId: String,
    themeColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isDonation && organization != null) {
                SummaryRow(
                    label = "Organization",
                    value = organization.name.orEmpty()
                )
                SummaryRow(
                    label = "Total",
                    value = "₱${"%.2f".format(totalPrice)}",
                    valueStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )
                )
            } else {
                SummaryRow(
                    label = "Items Subtotal",
                    value = "₱${"%.2f".format(totalPrice - 50.0)}"
                )
                SummaryRow(
                    label = "Shipping Fee",
                    value = "₱50.00"
                )
                SummaryRow(
                    label = "Total",
                    value = "₱${"%.2f".format(totalPrice)}",
                    valueStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SummaryRow(
                label = "Payment Method",
                value = if (paymentMethod == "GCash") "GCash" else "Cash on Delivery"
            )
            if (paymentMethod == "GCash" && referenceId.isNotEmpty()) {
                SummaryRow(
                    label = "Reference ID",
                    value = referenceId
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueStyle: TextStyle = MaterialTheme.typography.bodyMedium
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = valueStyle,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun DoneButton(
    navController: NavController,
    themeColor: Color,
    cartViewModel: CartViewModel
) {
    Button(
        onClick = {
            cartViewModel.clearCart()
            navController.navigate("farmerdashboard") {
                popUpTo("farmerdashboard") { inclusive = true }
                launchSingleTop = true
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
    ) {
        Text(
            text = "Done",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}

@Composable
private fun HorizontalDivider(modifier: Modifier = Modifier) {
    Divider(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFEEEEEE),
        thickness = 1.dp
    )
}

private fun validateInputs(
    cartItems: List<CartItem>,
    totalPrice: Double,
    orderNumber: String,
    isDonation: Boolean,
    organization: Organization?
): Boolean {
    if (orderNumber.isEmpty()) return false
    if (cartItems.isEmpty()) return false
    if (totalPrice <= 0) return false
    if (isDonation && organization?.name.isNullOrEmpty()) return false
    if (!cartItems.all { !it.name.isNullOrEmpty() && !it.productId.isNullOrEmpty() && it.price > 0 }) {
        return false
    }
    return true
}