package com.example.agriconnect

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

// Data class to hold farm with ID
data class FarmWithId(
    val id: String,
    val farm: Farm
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmListScreen(navController: NavController) {
    val user = FirebaseAuth.getInstance().currentUser
    val emailVerified = user?.isEmailVerified ?: false
    var userFarms by remember { mutableStateOf<List<FarmWithId>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasPendingFarm by remember { mutableStateOf(false) }
    var pendingFarmData by remember { mutableStateOf<Map<String, String>?>(null) }
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val farmRepository = remember { FarmRepository() }

    // Check email verification and load farms
    LaunchedEffect(user, emailVerified) {
        isLoading = true
        user?.let { firebaseUser ->
            // Check for pending farm
            try {
                val privateDoc = db.collection("users_private")
                    .document(firebaseUser.uid)
                    .get()
                    .await()

                val pending = privateDoc.get("pendingFarm") as? Map<*, *>
                if (pending != null && emailVerified) {
                    hasPendingFarm = true
                    pendingFarmData = pending.mapKeys { it.key.toString() }
                        .mapValues { it.value.toString() }
                }
            } catch (e: Exception) {
                android.util.Log.e("FarmList", "Error checking pending farm", e)
            }

            // Load existing farms
            try {
                val snapshot = db.collection("farms")
                    .whereEqualTo("ownerId", firebaseUser.uid)
                    .get()
                    .await()

                userFarms = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Farm::class.java)?.let { farm ->
                        FarmWithId(doc.id, farm)
                    }
                }
                android.util.Log.d("FarmList", "Loaded ${userFarms.size} farms")
            } catch (e: Exception) {
                android.util.Log.e("FarmList", "Error loading farms", e)
                Toast.makeText(context, "Error loading farms: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    // Auto-create pending farm if exists
    LaunchedEffect(hasPendingFarm, pendingFarmData) {
        if (hasPendingFarm && pendingFarmData != null && emailVerified) {
            farmRepository.createFarm(
                farmName = pendingFarmData!!["farmName"] ?: "",
                address = pendingFarmData!!["location"],
                description = pendingFarmData!!["description"]
            ) { success ->
                if (success) {
                    // Clear pending farm data
                    user?.let { firebaseUser ->
                        db.collection("users_private")
                            .document(firebaseUser.uid)
                            .update("pendingFarm", null)
                    }
                    hasPendingFarm = false
                    Toast.makeText(
                        context,
                        "Farm created successfully!",
                        Toast.LENGTH_LONG
                    ).show()
                    // Reload farms
                    user?.let { firebaseUser ->
                        db.collection("farms")
                            .whereEqualTo("ownerId", firebaseUser.uid)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                userFarms = snapshot.documents.mapNotNull { doc ->
                                    doc.toObject(Farm::class.java)?.let { farm ->
                                        FarmWithId(doc.id, farm)
                                    }
                                }
                            }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Farms") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (emailVerified) {
                FloatingActionButton(
                    onClick = { navController.navigate("farm_create") },
                    containerColor = Color(0xFF4CAF50)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Farm",
                        tint = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            when {
                !emailVerified -> {
                    // Show email verification message
                    EmailVerificationMessage(
                        user = user,
                        hasPendingFarm = hasPendingFarm
                    )
                }
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF4CAF50))
                    }
                }
                userFarms.isEmpty() && !hasPendingFarm -> {
                    // No farms - show empty state
                    NoFarmsEmptyState(
                        onCreateFarm = { navController.navigate("farm_create") }
                    )
                }
                else -> {
                    // Show farms list
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (hasPendingFarm) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFE8F5E9)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Color(0xFF4CAF50),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "Creating your farm...",
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32)
                                            )
                                            Text(
                                                text = pendingFarmData?.get("farmName") ?: "",
                                                fontSize = 12.sp,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        items(userFarms) { farmWithId ->
                            FarmListCard(
                                farmWithId = farmWithId,
                                navController = navController, // Pass navController here
                                onClick = { /* Navigate to farm details */ }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmailVerificationMessage(
    user: com.google.firebase.auth.FirebaseUser?,
    hasPendingFarm: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = null,
            tint = Color(0xFFFF9800),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Email Verification Required",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (hasPendingFarm) {
                "Verify your email to create your farm"
            } else {
                "Verify your email to create and manage farms"
            },
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { user?.sendEmailVerification() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            )
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Resend Verification Email")
        }
    }
}

@Composable
fun NoFarmsEmptyState(onCreateFarm: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Business,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Farms Yet",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Create your first farm to start selling products",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onCreateFarm,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            ),
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Your First Farm")
        }
    }
}

@Composable
fun FarmListCard(
    farmWithId: FarmWithId,
    navController: NavController, // Add this parameter
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
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
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = farmWithId.farm.name.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = farmWithId.farm.address ?: "No address",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            if (farmWithId.farm.description?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = farmWithId.farm.description!!,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // Navigate to product upload
                        navController.navigate("product_upload/${farmWithId.id}/${farmWithId.farm.name}")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Product", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { /* View products */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Products", fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmCreationScreen(
    farmRepository: FarmRepository,
    navController: NavController
) {
    val user = FirebaseAuth.getInstance().currentUser
    val emailVerified = user?.isEmailVerified ?: false
    var farmName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Farm") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (!emailVerified) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EmailVerificationMessage(user = user, hasPendingFarm = false)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF5F5F5)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Text(
                                text = "Farm Details",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )

                            OutlinedTextField(
                                value = farmName,
                                onValueChange = { farmName = it },
                                label = { Text("Farm Name") },
                                placeholder = { Text("Green Valley Farm") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Business,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4CAF50),
                                    focusedLabelColor = Color(0xFF4CAF50),
                                    cursorColor = Color(0xFF4CAF50)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Farm Address") },
                                placeholder = { Text("123 Farm Road, Rural Valley") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4CAF50),
                                    focusedLabelColor = Color(0xFF4CAF50),
                                    cursorColor = Color(0xFF4CAF50)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Farm Description") },
                                placeholder = { Text("Tell us about your farm...") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4CAF50),
                                    focusedLabelColor = Color(0xFF4CAF50),
                                    cursorColor = Color(0xFF4CAF50)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (farmName.isNotBlank()) {
                                        isLoading = true
                                        farmRepository.createFarm(
                                            farmName = farmName.trim(),
                                            address = address.trim().ifBlank { null },
                                            description = description.trim().ifBlank { null }
                                        ) { success ->
                                            isLoading = false
                                            if (success) {
                                                Toast.makeText(
                                                    context,
                                                    "Farm created successfully!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                navController.navigateUp()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Failed to create farm. Please try again.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Please enter a farm name",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                enabled = !isLoading && farmName.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Creating Farm...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Create Farm",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF3E5F5)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF7B1FA2),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Your farm profile will be visible to customers. Make sure to provide accurate information.",
                                fontSize = 13.sp,
                                color = Color(0xFF7B1FA2),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}