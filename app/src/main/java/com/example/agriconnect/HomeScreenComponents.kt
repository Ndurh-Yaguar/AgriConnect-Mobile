package com.example.agriconnect

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Bottom Navigation Items
sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Home")
    object Marketplace : BottomNavItem("marketplace", Icons.Default.Store, "Marketplace")
    object Community : BottomNavItem("community", Icons.Default.Forum, "Community")
    object Orders : BottomNavItem("orders", Icons.Default.ShoppingBag, "Orders")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profile")
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenWithNavigation(auth: FirebaseAuth, navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    val user = auth.currentUser
    val emailVerified = user?.isEmailVerified ?: false

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🌱 AgriConnect", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = Color(0xFF4CAF50)
            ) {
                val items = listOf(
                    BottomNavItem.Home,
                    BottomNavItem.Marketplace,
                    BottomNavItem.Community,
                    BottomNavItem.Orders,
                    BottomNavItem.Profile
                )

                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            when (item.route) {
                                "home" -> navController.navigate("home")
                                "marketplace" -> navController.navigate("marketplace")
                                "community" -> navController.navigate("community_forum")
                                "orders" -> {/* Coming soon */}
                                "profile" -> {/* handled within home */}
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF4CAF50),
                            selectedTextColor = Color(0xFF4CAF50),
                            indicatorColor = Color(0xFFE8F5E9)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> FloatingActionButton(
                    onClick = { navController.navigate("farm_create") },
                    containerColor = Color(0xFF4CAF50)
                ) { Icon(Icons.Default.Add, contentDescription = "Add Farm", tint = Color.White) }

                1 -> FloatingActionButton(
                    onClick = { /* TODO: Search marketplace */ },
                    containerColor = Color(0xFF4CAF50)
                ) { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White) }

                2 -> FloatingActionButton(
                    onClick = { /* TODO: Create new post in forum */ },
                    containerColor = Color(0xFF4CAF50)
                ) { Icon(Icons.Default.Edit, contentDescription = "New Post", tint = Color.White) }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> HomeTab(auth, navController, emailVerified)
                1 -> MarketplaceTab()
                2 -> CommunityTab(emailVerified)
                3 -> OrdersTab(emailVerified)
                4 -> ProfileTab(auth, navController)
            }
        }
    }
}


@Composable
fun HomeTab(auth: FirebaseAuth, navController: NavController, emailVerified: Boolean) {
    val user = auth.currentUser
    var userFarms by remember { mutableStateOf<List<Farm>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val db = FirebaseFirestore.getInstance()

    // Load farms - triggers on user change or manual refresh
    LaunchedEffect(user, refreshTrigger) {
        user?.let {
            isLoading = true
            try {
                val snapshot = db.collection("farms")
                    .whereEqualTo("ownerId", it.uid)
                    .get()
                    .await()
                userFarms = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Farm::class.java)
                }
            } catch (e: Exception) {
                // Handle error
                android.util.Log.e("HomeTab", "Error loading farms", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Listen to navigation back event to refresh
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            if (destination.route == "home") {
                refreshTrigger++
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF4CAF50), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (user?.email?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Welcome back!", fontSize = 14.sp, color = Color.Gray)
                        Text(
                            user?.email?.substringBefore("@") ?: "User",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Refresh button
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(
                            Icons.Default.Refresh,
                            "Refresh",
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }

        // Loading indicator
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Stats Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    Modifier.weight(1f),
                    Icons.Default.Store,
                    "${userFarms.size}",
                    "My Farms",
                    Color(0xFF4CAF50)
                )
                StatCard(
                    Modifier.weight(1f),
                    Icons.Default.ShoppingCart,
                    "0",
                    "Orders",
                    Color(0xFF2196F3)
                )
            }
        }

        // Quick Actions Header
        item {
            Text(
                "Quick Actions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

// Quick Actions Row 1 - Farm & Shop
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        Modifier.weight(1f),
                        Icons.Default.Store,
                        "My Farms",
                        if (userFarms.isEmpty()) "Create your first farm" else "${userFarms.size} farm(s)",
                        { navController.navigate("farm_list") },
                        emailVerified
                    )
                    QuickActionCard(
                        Modifier.weight(1f),
                        Icons.Default.ShoppingCart,
                        "Marketplace",
                        "Shop fresh produce",
                        { navController.navigate("marketplace") },
                        true
                    )
                }
            }

// Quick Actions Row 2 - Auctions & Forum (NEW!)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        Modifier.weight(1f),
                        Icons.Default.Gavel,
                        "Auctions",
                        "Bid on farm products",
                        { navController.navigate("auctions") },
                        true
                    )
                    QuickActionCard(
                        Modifier.weight(1f),
                        Icons.Default.Forum,
                        "Community",
                        "Share & discuss",
                        { navController.navigate("community_forum") },
                        emailVerified
                    )
                }
            }

// Quick Actions Row 3 - Orders & Admin
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        Modifier.weight(1f),
                        Icons.Default.ShoppingBag,
                        "My Orders",
                        "Track purchases",
                        { /* Navigate */ },
                        emailVerified
                    )

                    // Show Admin button only if user is admin
                    var isAdmin by remember { mutableStateOf(false) }
                    LaunchedEffect(user) {
                        user?.let {
                            try {
                                val userDoc = FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(it.uid)
                                    .get()
                                    .await()
                                isAdmin = userDoc.getBoolean("isAdmin") ?: false
                            } catch (e: Exception) {
                                isAdmin = false
                            }
                        }
                    }

                    if (isAdmin) {
                        QuickActionCard(
                            Modifier.weight(1f),
                            Icons.Default.AdminPanelSettings,
                            "Admin",
                            "Manage platform",
                            { navController.navigate("admin_dashboard") },
                            true
                        )
                    } else {
                        // Placeholder to maintain grid
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

        // My Farms Section
        if (userFarms.isNotEmpty()) {
            item {
                Text(
                    "My Farms",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(userFarms) { farm ->
                FarmCard(farm) { /* Navigate */ }
            }
        }

        // Email Verification Reminder
        if (!emailVerified) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = Color(0xFFEF6C00))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Verify your email",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF6C00)
                            )
                            Text(
                                "To create farms and sell products",
                                fontSize = 12.sp,
                                color = Color(0xFFEF6C00)
                            )
                        }
                        TextButton(onClick = { user?.sendEmailVerification() }) {
                            Text("Verify")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun QuickActionCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            disabledContainerColor = Color.White.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                icon,
                null,
                tint = if (enabled) Color(0xFF4CAF50) else Color.Gray,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Color.Black else Color.Gray
            )
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            if (!enabled) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Verify email first",
                    fontSize = 10.sp,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun FarmCard(farm: Farm, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Color(0xFF4CAF50).copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Business,
                    null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(farm.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    farm.address ?: "No address provided",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

@Composable
fun MarketplaceTab() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.Store, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
            Text("Marketplace Coming Soon", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Browse fresh produce from local farms",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CommunityTab(emailVerified: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(Icons.Default.Forum, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
            Text(
                "Community Forum Coming Soon",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Connect with farmers and buyers\nShare tips, ask questions, and learn",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            if (!emailVerified) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "⚠️ Email verification required to post",
                    fontSize = 12.sp,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
fun OrdersTab(emailVerified: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(Icons.Default.ShoppingBag, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
            Text("No Orders Yet", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Your purchase history will appear here",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            if (!emailVerified) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "⚠️ Verify email to place orders",
                    fontSize = 12.sp,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
fun ProfileTab(auth: FirebaseAuth, navController: NavController) {
    val user = auth.currentUser

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFF4CAF50), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (user?.email?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"),
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(user?.email ?: "User", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (user?.isEmailVerified == true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Verified", fontSize = 12.sp, color = Color(0xFF4CAF50))
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    ProfileMenuItem(Icons.Default.Person, "Edit Profile", "Update your information") {}
                    Divider()
                    ProfileMenuItem(Icons.Default.Notifications, "Notifications", "Manage your alerts") {}
                    Divider()
                    ProfileMenuItem(Icons.Default.Lock, "Privacy & Security", "Password and privacy settings") {}
                    Divider()
                    ProfileMenuItem(Icons.Default.Help, "Help & Support", "FAQs and contact us") {}
                }
            }
        }

        item {
            Button(
                onClick = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
            ) {
                Icon(Icons.Default.ExitToApp, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout")
            }
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color(0xFF4CAF50))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}