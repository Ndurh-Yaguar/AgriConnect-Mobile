package com.example.agriconnect

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavController) {
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    var selectedTab by remember { mutableStateOf(0) }
    var isAdmin by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableStateOf(0) }

    // Check if user is admin
    LaunchedEffect(user) {
        user?.let {
            try {
                val userDoc = db.collection("users").document(it.uid).get().await()
                isAdmin = userDoc.getBoolean("isAdmin") ?: false
                android.util.Log.d("AdminDashboard", "User ${it.uid} isAdmin: $isAdmin")
            } catch (e: Exception) {
                android.util.Log.e("AdminDashboard", "Error checking admin status", e)
                isAdmin = false
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF4CAF50))
        }
        return
    }

    if (!isAdmin) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Lock,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
                Text(
                    "Access Denied",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "You don't have admin privileges",
                    color = Color.Gray
                )
                Button(onClick = { navController.navigateUp() }) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF1976D2)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Pending") },
                    icon = { Icon(Icons.Default.PendingActions, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Approved") },
                    icon = { Icon(Icons.Default.CheckCircle, null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Rejected") },
                    icon = { Icon(Icons.Default.Cancel, null) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Stats") },
                    icon = { Icon(Icons.Default.BarChart, null) }
                )
            }

            when (selectedTab) {
                0 -> PendingProductsTab(refreshTrigger)
                1 -> ApprovedProductsTab(refreshTrigger)
                2 -> RejectedProductsTab(refreshTrigger)
                3 -> AdminStatsTab(refreshTrigger)
            }
        }
    }
}

@Composable
fun PendingProductsTab(refreshTrigger: Int) {
    val db = FirebaseFirestore.getInstance()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        try {
            android.util.Log.d("AdminDashboard", "Loading pending products...")
            val snapshot = db.collection("products")
                .whereEqualTo("status", "pending")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            products = snapshot.documents.mapNotNull { doc ->
                try {
                    val product = doc.toObject(Product::class.java)
                    product?.copy(id = doc.id)
                } catch (e: Exception) {
                    android.util.Log.e("AdminDashboard", "Error parsing product ${doc.id}", e)
                    null
                }
            }
            android.util.Log.d("AdminDashboard", "Loaded ${products.size} pending products")
        } catch (e: Exception) {
            android.util.Log.e("AdminDashboard", "Error loading products", e)
        } finally {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF1976D2)
            )
        } else if (products.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Inbox,
                title = "No Pending Products",
                subtitle = "All products have been reviewed"
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    PendingProductCard(
                        product = product,
                        onClick = { selectedProduct = product }
                    )
                }
            }
        }

        selectedProduct?.let { product ->
            ProductReviewDialog(
                product = product,
                onDismiss = { selectedProduct = null },
                onReviewed = {
                    selectedProduct = null
                    products = products.filter { it.id != product.id }
                }
            )
        }
    }
}

@Composable
fun PendingProductCard(
    product: Product,
    onClick: () -> Unit
) {
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
            AsyncImage(
                model = product.productImageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Grade: ${product.grade.replace("_", " ")}",
                    fontSize = 14.sp,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Farm: ${product.farmName}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    "R${String.format("%.2f", product.price)}/${product.unit}",
                    fontSize = 14.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductReviewDialog(
    product: Product,
    onDismiss: () -> Unit,
    onReviewed: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    var showCertificate by remember { mutableStateOf(false) }
    var rejectionReason by remember { mutableStateOf("") }
    var showRejectionDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    fun approveProduct() {
        if (isProcessing) return
        isProcessing = true

        android.util.Log.d("AdminDashboard", "Approving product ${product.id}")

        val updates = hashMapOf<String, Any?>(
            "status" to "approved",
            "approvedAt" to FieldValue.serverTimestamp(),
            "approvedBy" to user?.uid,
            "rejectionReason" to null,
            "rejectedAt" to null,
            "rejectedBy" to null
        )

        db.collection("products").document(product.id)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Product approved successfully!", Toast.LENGTH_SHORT).show()
                android.util.Log.d("AdminDashboard", "Product ${product.id} approved")
                onReviewed()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                android.util.Log.e("AdminDashboard", "Error approving product ${product.id}", e)
                isProcessing = false
            }
    }

    fun rejectProduct(reason: String) {
        if (isProcessing) return
        isProcessing = true

        android.util.Log.d("AdminDashboard", "Rejecting product ${product.id} with reason: $reason")

        val updates = hashMapOf<String, Any?>(
            "status" to "rejected",
            "rejectionReason" to reason,
            "rejectedBy" to user?.uid,
            "rejectedAt" to FieldValue.serverTimestamp(),
            "approvedAt" to null,
            "approvedBy" to null
        )

        db.collection("products").document(product.id)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Product rejected", Toast.LENGTH_SHORT).show()
                android.util.Log.d("AdminDashboard", "Product ${product.id} rejected")
                onReviewed()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                android.util.Log.e("AdminDashboard", "Error rejecting product ${product.id}", e)
                isProcessing = false
            }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1976D2))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Review Product",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Close", tint = Color.White)
                        }
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    AsyncImage(
                        model = product.productImageUrl,
                        contentDescription = product.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    DetailRow("Product Name", product.name)
                    DetailRow("Category", product.category)
                    DetailRow("Grade", product.grade.replace("_", " "), Color(0xFFFF9800))
                    DetailRow("Price", "R${String.format("%.2f", product.price)}/${product.unit}")
                    DetailRow("Quantity", "${product.availableQuantity}")
                    DetailRow("Farm", product.farmName)
                    DetailRow("Seller", product.ownerEmail)

                    if (product.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Description:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(product.description, fontSize = 14.sp, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { showCertificate = !showCertificate },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VerifiedUser, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (showCertificate) "Hide Certificate" else "View Certificate")
                    }

                    AnimatedVisibility(visible = showCertificate) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            AsyncImage(
                                model = product.certificateUrl,
                                contentDescription = "Certificate",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showRejectionDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE57373)
                            ),
                            enabled = !isProcessing
                        ) {
                            Icon(Icons.Default.Cancel, null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reject")
                        }

                        Button(
                            onClick = { approveProduct() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            enabled = !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(Icons.Default.CheckCircle, null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Approve")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRejectionDialog) {
        AlertDialog(
            onDismissRequest = { showRejectionDialog = false },
            title = { Text("Reject Product") },
            text = {
                Column {
                    Text("Please provide a reason for rejection:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        placeholder = { Text("e.g., Grade doesn't match certificate") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectionReason.isNotBlank()) {
                            rejectProduct(rejectionReason)
                            showRejectionDialog = false
                        }
                    },
                    enabled = rejectionReason.isNotBlank() && !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE57373)
                    )
                ) {
                    Text("Reject Product")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = Color.Black) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "$label:",
            fontSize = 14.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Text(
            value,
            fontSize = 14.sp,
            color = valueColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ApprovedProductsTab(refreshTrigger: Int) {
    val db = FirebaseFirestore.getInstance()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        try {
            android.util.Log.d("AdminDashboard", "Loading approved products...")
            val snapshot = db.collection("products")
                .whereEqualTo("status", "approved")
                .orderBy("approvedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            products = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Product::class.java)?.copy(id = doc.id)
            }
            android.util.Log.d("AdminDashboard", "Loaded ${products.size} approved products")
        } catch (e: Exception) {
            android.util.Log.e("AdminDashboard", "Error loading approved products", e)
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF4CAF50))
        }
    } else if (products.isEmpty()) {
        EmptyState(
            icon = Icons.Default.CheckCircle,
            title = "No Approved Products",
            subtitle = "Approved products will appear here"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products, key = { it.id }) { product ->
                ApprovedProductCard(product)
            }
        }
    }
}

@Composable
fun ApprovedProductCard(product: Product) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.productImageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    product.farmName,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    "R${String.format("%.2f", product.price)}/${product.unit}",
                    fontSize = 14.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                Icons.Default.CheckCircle,
                null,
                tint = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun RejectedProductsTab(refreshTrigger: Int) {
    val db = FirebaseFirestore.getInstance()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        try {
            android.util.Log.d("AdminDashboard", "Loading rejected products...")
            val snapshot = db.collection("products")
                .whereEqualTo("status", "rejected")
                .orderBy("rejectedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            products = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Product::class.java)?.copy(id = doc.id)
            }
            android.util.Log.d("AdminDashboard", "Loaded ${products.size} rejected products")
        } catch (e: Exception) {
            android.util.Log.e("AdminDashboard", "Error loading rejected products", e)
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFE57373))
        }
    } else if (products.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Cancel,
            title = "No Rejected Products",
            subtitle = "Rejected products will appear here"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products, key = { it.id }) { product ->
                RejectedProductCard(product)
            }
        }
    }
}

@Composable
fun RejectedProductCard(product: Product) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = product.productImageUrl,
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        product.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        product.farmName,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Icon(
                    Icons.Default.Cancel,
                    null,
                    tint = Color(0xFFE57373)
                )
            }

            if (product.rejectionReason?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Rejection Reason:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE57373)
                        )
                        Text(
                            product.rejectionReason!!,
                            fontSize = 12.sp,
                            color = Color(0xFFE57373)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStatsTab(refreshTrigger: Int) {
    val db = FirebaseFirestore.getInstance()
    var stats by remember { mutableStateOf<AdminStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        try {
            val allProducts = db.collection("products").get().await()
            val pending = allProducts.documents.count { it.getString("status") == "pending" }
            val approved = allProducts.documents.count { it.getString("status") == "approved" }
            val rejected = allProducts.documents.count { it.getString("status") == "rejected" }

            val allUsers = db.collection("users").get().await().size()
            val allFarms = db.collection("farms").get().await().size()

            stats = AdminStats(
                totalProducts = allProducts.size(),
                pendingProducts = pending,
                approvedProducts = approved,
                rejectedProducts = rejected,
                totalUsers = allUsers,
                totalFarms = allFarms
            )
        } catch (e: Exception) {
            android.util.Log.e("AdminDashboard", "Error loading stats", e)
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF1976D2))
        }
    } else {
        stats?.let { adminStats ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Platform Statistics",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatsCard(
                            Modifier.weight(1f),
                            Icons.Default.Person,
                            "${adminStats.totalUsers}",
                            "Total Users",
                            Color(0xFF2196F3)
                        )
                        StatsCard(
                            Modifier.weight(1f),
                            Icons.Default.Store,
                            "${adminStats.totalFarms}",
                            "Total Farms",
                            Color(0xFF4CAF50)
                        )
                    }
                }

                item {
                    Text(
                        "Product Statistics",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    StatsCard(
                        Modifier.fillMaxWidth(),
                        Icons.Default.Inventory,
                        "${adminStats.totalProducts}",
                        "Total Products",
                        Color(0xFF9C27B0)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatsCard(
                            Modifier.weight(1f),
                            Icons.Default.PendingActions,
                            "${adminStats.pendingProducts}",
                            "Pending",
                            Color(0xFFFF9800)
                        )
                        StatsCard(
                            Modifier.weight(1f),
                            Icons.Default.CheckCircle,
                            "${adminStats.approvedProducts}",
                            "Approved",
                            Color(0xFF4CAF50)
                        )
                    }
                }

                item {
                    StatsCard(
                        Modifier.fillMaxWidth(),
                        Icons.Default.Cancel,
                        "${adminStats.rejectedProducts}",
                        "Rejected",
                        Color(0xFFE57373)
                    )
                }
            }
        }
    }
}

@Composable
fun StatsCard(
    modifier: Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                null,
                tint = color,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                value,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}