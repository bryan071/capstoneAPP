package com.project.webapp.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.R
import com.project.webapp.datas.Product
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    navController: NavController,
    firestore: FirebaseFirestore
) {
    var query by remember { mutableStateOf("") }
    var showResults by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                if (it.length >= 2) {
                    showResults = true
                    isSearching = true
                    coroutineScope.launch {
                        searchProducts(query, firestore) { results ->
                            searchResults = results
                            isSearching = false
                        }
                    }
                } else {
                    showResults = false
                    searchResults = emptyList()
                }
            },
            placeholder = { Text("What are you looking for?") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Icon",
                    tint = Color(0xFF0DA54B)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        showResults = false
                        searchResults = emptyList()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Search",
                            tint = Color(0xFF0DA54B)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color(0xFF0DA54B),
                unfocusedIndicatorColor = Color.LightGray,
                cursorColor = Color(0xFF0DA54B)
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (query.isNotEmpty()) {
                        isSearching = true
                        coroutineScope.launch {
                            searchProducts(query, firestore) { results ->
                                searchResults = results
                                isSearching = false
                            }
                        }
                    }
                }
            )
        )

        AnimatedVisibility(
            visible = showResults,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            SearchResultsPanel(
                isSearching = isSearching,
                searchResults = searchResults,
                navController = navController,
                onDismiss = { showResults = false }
            )
        }
    }
}

@Composable
fun SearchResultsPanel(
    isSearching: Boolean,
    searchResults: List<Product>,
    navController: NavController,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .heightIn(max = 350.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Search Results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Close Results",
                        tint = Color(0xFF0DA54B)
                    )
                }
            }

            Divider(thickness = 1.dp, color = Color.LightGray)
            Spacer(modifier = Modifier.height(8.dp))

            when {
                isSearching -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF0DA54B))
                    }
                }
                searchResults.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No products found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(searchResults) { product ->
                            SearchResultItem(product, navController, onDismiss)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    product: Product,
    navController: NavController,
    onDismiss: () -> Unit
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    val formattedPrice = currencyFormatter.format(product.price)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate("productDetails/${product.prodId}")
                onDismiss()
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product Image
        Card(
            modifier = Modifier.size(60.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Product Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.category,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Text(
                text = formattedPrice,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0DA54B)
            )
        }

        // Arrow indicator
        Icon(
            painter = painterResource(id = R.drawable.search), // Replace with appropriate icon
            contentDescription = "View Product",
            tint = Color(0xFF0DA54B),
            modifier = Modifier.size(20.dp)
        )
    }

    Divider(thickness = 0.5.dp, color = Color.LightGray)
}

fun searchProducts(
    query: String,
    firestore: FirebaseFirestore,
    onSearchComplete: (List<Product>) -> Unit
) {
    if (query.length < 2) {
        onSearchComplete(emptyList())
        return
    }

    val normalizedQuery = query.lowercase().trim()
    val productsCollection = firestore.collection("products")

    // Create a compound query to search in multiple fields
    productsCollection
        .get()
        .addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                onSearchComplete(emptyList())
                return@addOnSuccessListener
            }

            // Filter products client-side for more flexible searching
            val results = snapshot.documents.mapNotNull { doc ->
                try {
                    val product = Product(
                        prodId = doc.id,
                        ownerId = doc.getString("ownerId") ?: return@mapNotNull null,
                        category = doc.getString("category") ?: return@mapNotNull null,
                        imageUrl = doc.getString("imageUrl") ?: return@mapNotNull null,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        description = doc.getString("description") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        quantity = doc.getDouble("quantity") ?: 0.0,
                        quantityUnit = doc.getString("quantityUnit") ?: "unit",
                        cityName = doc.getString("cityName") ?: "Unknown",
                        timestamp = doc.getTimestamp("listedAt") ?: Timestamp.now() // <-- this line fixed
                    )

                    // Check if product matches search query
                    val nameMatches = product.name.lowercase().contains(normalizedQuery)
                    val categoryMatches = product.category.lowercase().contains(normalizedQuery)
                    val descriptionMatches = product.description.lowercase().contains(normalizedQuery)
                    val cityMatches = product.cityName.lowercase().contains(normalizedQuery)

                    if (nameMatches || categoryMatches || descriptionMatches || cityMatches) {
                        product
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("SearchDebug", "Error parsing product data: ${e.message}", e)
                    null
                }
            }

            // Sort results by relevance - items with name matches first
            val sortedResults = results.sortedWith(compareBy(
                { !it.name.lowercase().contains(normalizedQuery) }, // Name matches first
                { it.name } // Then alphabetically
            ))

            onSearchComplete(sortedResults)
        }
        .addOnFailureListener { exception ->
            Log.e("SearchDebug", "Error searching products: ${exception.message}", exception)
            onSearchComplete(emptyList())
        }
}