package com.example.agriconnect

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.UUID

// Product Grading Standards
enum class ProductGrade(val displayName: String, val description: String) {
    CLASS_1("Class 1", "Superior quality - Perfect appearance"),
    CLASS_2("Class 2", "Good quality - Minor defects allowed"),
    CLASS_3("Class 3", "Marketable quality - More defects allowed"),
    ORGANIC("Organic Certified", "Certified organic produce"),
    PREMIUM("Premium", "Highest quality grade")
}

// Product Category
enum class ProductCategory(val displayName: String, val icon: String) {
    VEGETABLES("Vegetables", "🥬"),
    FRUITS("Fruits", "🍎"),
    GRAINS("Grains & Cereals", "🌾"),
    DAIRY("Dairy Products", "🥛"),
    MEAT("Meat & Poultry", "🍖"),
    EGGS("Eggs", "🥚"),
    HERBS("Herbs & Spices", "🌿"),
    HONEY("Honey & Bee Products", "🍯"),
    OTHER("Other", "📦")
}

// Product Unit of Measurement
enum class ProductUnit(val displayName: String) {
    KG("Kilogram (kg)"),
    G("Gram (g)"),
    L("Liter (L)"),
    ML("Milliliter (mL)"),
    PIECE("Piece"),
    DOZEN("Dozen"),
    BAG("Bag"),
    BOX("Box"),
    BUNCH("Bunch")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductUploadScreen(farmId: String, farmName: String, navController: NavController) {
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var productName by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var availableQuantity by remember { mutableStateOf("") }
    var productImageUri by remember { mutableStateOf<Uri?>(null) }
    var certificateUri by remember { mutableStateOf<Uri?>(null) }
    var certificateFileName by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        productImageUri = uri
        errors = errors - "image"
    }

    val certPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        certificateUri = uri
        uri?.let { certificateFileName = getFileName(context, it) }
        errors = errors - "certificate"
    }

    fun validateForm(): Boolean {
        val newErrors = mutableMapOf<String, String>()
        if (productName.isBlank()) newErrors["name"] = "Product name required"
        if (category.isEmpty()) newErrors["category"] = "Select category"
        if (grade.isEmpty()) newErrors["grade"] = "Select grade"
        if (price.toDoubleOrNull() == null || price.toDouble() <= 0) newErrors["price"] = "Enter valid price"
        if (unit.isEmpty()) newErrors["unit"] = "Select unit"
        if (availableQuantity.toIntOrNull() == null) newErrors["quantity"] = "Enter valid quantity"
        if (productImageUri == null) newErrors["image"] = "Product image required"
        if (certificateUri == null) newErrors["certificate"] = "Certificate required"
        errors = newErrors
        return newErrors.isEmpty()
    }

    fun uploadProduct() {
        if (!validateForm()) {
            Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_LONG).show()
            return
        }

        isUploading = true
        uploadProgress = "Starting upload..."

        scope.launch {
            try {
                // Generate product ID first
                val productId = UUID.randomUUID().toString()

                // Upload product image
                uploadProgress = "Uploading product image..."
                val imageResult = CloudinaryUploadHelper.uploadFile(context, productImageUri!!)
                if (imageResult.isFailure) {
                    throw imageResult.exceptionOrNull() ?: Exception("Image upload failed")
                }
                val imageUrl = imageResult.getOrThrow()
                android.util.Log.d("ProductUpload", "Image uploaded: $imageUrl")

                // Upload certificate
                uploadProgress = "Uploading certificate..."
                val certResult = CloudinaryUploadHelper.uploadFile(context, certificateUri!!)
                if (certResult.isFailure) {
                    throw certResult.exceptionOrNull() ?: Exception("Certificate upload failed")
                }
                val certUrl = certResult.getOrThrow()
                android.util.Log.d("ProductUpload", "Certificate uploaded: $certUrl")

                // Create product document with proper ID
                uploadProgress = "Saving product details..."

                val product = hashMapOf(
                    "id" to productId, // CRITICAL: Set the ID field
                    "name" to productName.trim(),
                    "description" to productDescription.trim(),
                    "category" to category,
                    "grade" to grade,
                    "price" to price.toDouble(),
                    "unit" to unit,
                    "availableQuantity" to availableQuantity.toInt(),
                    "farmId" to farmId,
                    "farmName" to farmName,
                    "ownerId" to user!!.uid,
                    "ownerEmail" to (user.email ?: ""),
                    "productImageUrl" to imageUrl,
                    "certificateUrl" to certUrl,
                    "status" to "pending", // CRITICAL: Set status to pending
                    "createdAt" to FieldValue.serverTimestamp(),
                    "approvedAt" to null,
                    "approvedBy" to null,
                    "rejectionReason" to null,
                    "rejectedAt" to null,
                    "rejectedBy" to null
                )

                // Use the productId as the document ID
                db.collection("products").document(productId).set(product)
                    .addOnSuccessListener {
                        isUploading = false
                        android.util.Log.d("ProductUpload", "Product saved with ID: $productId, status: pending")
                        Toast.makeText(
                            context,
                            "Product uploaded successfully! Awaiting admin approval.",
                            Toast.LENGTH_LONG
                        ).show()
                        navController.navigateUp()
                    }
                    .addOnFailureListener { e ->
                        isUploading = false
                        android.util.Log.e("ProductUpload", "Error saving product", e)
                        Toast.makeText(context, "Error saving product: ${e.message}", Toast.LENGTH_LONG).show()
                    }

            } catch (e: Exception) {
                isUploading = false
                android.util.Log.e("ProductUpload", "Upload failed", e)
                Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Product") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Farm info
            Card(colors = CardDefaults.cardColors(Color.White)) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Business, null, tint = Color(0xFF4CAF50))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Listing for:", fontSize = 12.sp, color = Color.Gray)
                        Text(farmName, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Product Image Upload
            Text("Product Image *", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            if (productImageUri != null) {
                Box {
                    AsyncImage(
                        model = productImageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { productImageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.Default.Close, "Remove", tint = Color.Red)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload Product Image")
                }
            }
            errors["image"]?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            // Certificate Upload
            Text("Conformity Certificate *", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            if (certificateUri != null) {
                Card(colors = CardDefaults.cardColors(Color(0xFFE8F5E9))) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Description, null, tint = Color(0xFF4CAF50))
                        Spacer(Modifier.width(12.dp))
                        Text(certificateFileName.ifEmpty { "Certificate selected" })
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {
                            certificateUri = null
                            certificateFileName = ""
                        }) {
                            Icon(Icons.Default.Close, "Remove", tint = Color.Red)
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { certPicker.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.UploadFile, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload Certificate (PDF or Image)")
                }
            }
            errors["certificate"]?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Divider()

            // Product Details
            Text("Product Details", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = productName,
                onValueChange = {
                    productName = it
                    errors = errors - "name"
                },
                label = { Text("Product Name *") },
                isError = errors.containsKey("name"),
                supportingText = errors["name"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            CategoryDropdown(category) {
                category = it
                errors = errors - "category"
            }

            GradeDropdown(grade) {
                grade = it
                errors = errors - "grade"
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = {
                        price = it
                        errors = errors - "price"
                    },
                    label = { Text("Price (R) *") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    isError = errors.containsKey("price"),
                    modifier = Modifier.weight(1f)
                )

                Box(modifier = Modifier.weight(1f)) {
                    UnitDropdown(unit) {
                        unit = it
                        errors = errors - "unit"
                    }
                }
            }

            OutlinedTextField(
                value = availableQuantity,
                onValueChange = {
                    availableQuantity = it
                    errors = errors - "quantity"
                },
                label = { Text("Available Quantity *") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                isError = errors.containsKey("quantity"),
                supportingText = errors["quantity"]?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = productDescription,
                onValueChange = { productDescription = it },
                label = { Text("Description (Optional)") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            // Info Card
            Card(colors = CardDefaults.cardColors(Color(0xFFE3F2FD))) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Default.Info, null, tint = Color(0xFF1976D2))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Your product will be reviewed by admins before appearing in the marketplace.",
                        fontSize = 13.sp,
                        color = Color(0xFF1976D2)
                    )
                }
            }

            // Upload Button
            Button(
                onClick = { uploadProduct() },
                enabled = !isUploading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(uploadProgress)
                } else {
                    Icon(Icons.Default.CloudUpload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload Product", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Dropdown Components
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = ProductCategory.values().find { it.name == selected }?.displayName ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Category *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ProductCategory.values().forEach {
                DropdownMenuItem(
                    text = { Text("${it.icon} ${it.displayName}") },
                    onClick = {
                        onSelect(it.name)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = ProductGrade.values().find { it.name == selected }?.displayName ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Grade *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ProductGrade.values().forEach {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(it.displayName, fontWeight = FontWeight.Bold)
                            Text(it.description, fontSize = 12.sp, color = Color.Gray)
                        }
                    },
                    onClick = {
                        onSelect(it.name)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = ProductUnit.values().find { it.name == selected }?.displayName ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Unit *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ProductUnit.values().forEach {
                DropdownMenuItem(
                    text = { Text(it.displayName) },
                    onClick = {
                        onSelect(it.name)
                        expanded = false
                    }
                )
            }
        }
    }
}

// Utility function
fun getFileName(context: Context, uri: Uri): String {
    var name = "Unknown"
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = it.getString(idx)
            }
        }
    }
    if (name == "Unknown") {
        val path = uri.path ?: ""
        val cut = path.lastIndexOf('/')
        if (cut != -1) name = path.substring(cut + 1)
    }
    return name
}