package com.example.agriconnect

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

// --- Data Model ---
data class LocalAuction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val imageUrl: String,
    val startingPrice: Double,
    val currentPrice: Double,
    val ownerId: String,
    val ownerName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val endTime: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000, // default 24h
)

// --- Local in-memory list ---
val localAuctionCache = mutableStateListOf<LocalAuction>()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionScreen(navController: NavController) {
    val user = FirebaseAuth.getInstance().currentUser
    var selectedTab by remember { mutableStateOf(0) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    // Timer for countdowns
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🌾 Farm Auctions (Offline)") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { navController.navigate("create_auction") },
                    containerColor = Color(0xFF4CAF50)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Auction", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF2E7D32),
                contentColor = Color.White
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Browse") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("My Auctions") })
            }

            val filtered = if (selectedTab == 0) {
                localAuctionCache
            } else {
                localAuctionCache.filter { it.ownerId == user?.uid }
            }

            if (filtered.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Gavel,
                    title = "No Auctions Found",
                    subtitle = "Create one or check back soon"
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered) { auction ->
                        AuctionCardLocal(auction = auction, now = now)
                    }
                }
            }
        }
    }
}

@Composable
fun AuctionCardLocal(auction: LocalAuction, now: Long) {
    val remaining = max(0L, auction.endTime - now)
    val ended = remaining <= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            if (auction.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = auction.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }

            Column(Modifier.padding(16.dp)) {
                Text(auction.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(auction.description, fontSize = 14.sp, color = Color.Gray, maxLines = 2)

                Spacer(Modifier.height(8.dp))

                Text("Current Price: ${formatZAR(auction.currentPrice)}", fontWeight = FontWeight.Medium)
                Text(
                    if (ended) "Auction Ended" else "Ends in: ${formatCountdown(remaining)}",
                    fontSize = 13.sp,
                    color = if (ended) Color.Gray else Color(0xFF2E7D32)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAuctionScreen(navController: NavController) {
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startingPrice by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Local Auction") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Auction Title") })
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
            OutlinedTextField(
                value = startingPrice,
                onValueChange = { startingPrice = it },
                label = { Text("Starting Price (R)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Image, null)
                Spacer(Modifier.width(8.dp))
                Text("Add Image")
            }

            Button(
                onClick = {
                    if (title.isBlank() || startingPrice.toDoubleOrNull() == null) {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val newAuction = LocalAuction(
                        title = title,
                        description = description,
                        imageUrl = imageUri?.toString() ?: "",
                        startingPrice = startingPrice.toDouble(),
                        currentPrice = startingPrice.toDouble(),
                        ownerId = user?.uid ?: "guest",
                        ownerName = user?.email ?: "Anonymous"
                    )

                    localAuctionCache.add(0, newAuction)
                    Toast.makeText(context, "Auction created locally!", Toast.LENGTH_SHORT).show()
                    navController.navigateUp()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Save Auction")
            }
        }
    }
}

// Utility functions
private fun formatZAR(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("en", "ZA")).format(amount)
}

private fun formatCountdown(ms: Long): String {
    val minutes = ms / 60000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days > 0 -> "${days}d ${hours % 24}h"
        hours > 0 -> "${hours}h ${minutes % 60}m"
        else -> "${minutes}m"
    }
}
