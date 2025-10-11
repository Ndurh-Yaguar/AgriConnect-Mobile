package com.example.agriconnect

import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

// Data classes for form state
data class RegistrationState(
    val farmName: String = "",
    val farmType: String = "",
    val farmSize: String = "",
    val farmLocation: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val passwordStrength: PasswordStrength = PasswordStrength.WEAK
)

enum class PasswordStrength(val label: String, val color: Color) {
    WEAK("Weak", Color(0xFFE53E3E)),
    MEDIUM("Medium", Color(0xFFDD6B20)),
    STRONG("Strong", Color(0xFF38A169))
}

enum class FarmType(val displayName: String, val emoji: String) {
    ORGANIC("Organic", "🌿"),
    CONVENTIONAL("Conventional", "🌾"),
    HYDROPONIC("Hydroponic", "💧"),
    GREENHOUSE("Greenhouse", "🏠"),
    LIVESTOCK("Livestock", "🐄"),
    MIXED("Mixed Farming", "🔄")
}

@Composable
fun LoginScreen(auth: FirebaseAuth, navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF667eea),
                        Color(0xFF764ba2)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Text(
                text = "🌱",
                fontSize = 48.sp
            )
            Text(
                text = "Welcome Back",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Sign in to your AgriConnect account",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Login Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                ) {
                    // Email Field
                    CustomTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address",
                        placeholder = "your.email@example.com",
                        leadingIcon = Icons.Default.Email,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Password Field
                    CustomTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        placeholder = "Enter your password",
                        leadingIcon = Icons.Default.Lock,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Button
                    Button(
                        onClick = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                isLoading = true
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        isLoading = false
                                        if (task.isSuccessful) {
                                            Toast.makeText(
                                                context,
                                                "Login successful!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            navController.navigate("home") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Login failed: ${task.exception?.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                            }
                        },
                        enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
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
                            Text("Signing In...")
                        } else {
                            Text(
                                text = "Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Register Link
                    TextButton(
                        onClick = { navController.navigate("register") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Don't have an account? Register",
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(auth: FirebaseAuth, navController: NavController) {
    var state by remember { mutableStateOf(RegistrationState()) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val db = FirebaseFirestore.getInstance()

    fun updateFarmName(name: String) {
        state = state.copy(
            farmName = name,
            errors = state.errors - "farmName"
        )
    }

    fun updateFarmType(type: String) {
        state = state.copy(
            farmType = type,
            errors = state.errors - "farmType"
        )
    }

    fun updateFarmSize(size: String) {
        state = state.copy(
            farmSize = size,
            errors = state.errors - "farmSize"
        )
    }

    fun updateFarmLocation(location: String) {
        state = state.copy(
            farmLocation = location,
            errors = state.errors - "farmLocation"
        )
    }

    fun updateEmail(email: String) {
        state = state.copy(
            email = email,
            errors = state.errors - "email"
        )
    }

    fun updatePassword(password: String) {
        state = state.copy(
            password = password,
            errors = state.errors - "password",
            passwordStrength = calculatePasswordStrength(password)
        )
    }

    fun validateForm(): Boolean {
        val errors = mutableMapOf<String, String>()

        if (state.farmName.trim().length < 2) {
            errors["farmName"] = "Farm name must be at least 2 characters"
        }
        if (state.farmType.isEmpty()) {
            errors["farmType"] = "Please select a farm type"
        }
        if (state.farmLocation.trim().length < 5) {
            errors["farmLocation"] = "Please enter a valid location"
        }
        if (!state.email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            errors["email"] = "Please enter a valid email"
        }
        if (state.password.length < 8) {
            errors["password"] = "Password must be at least 8 characters"
        }

        state = state.copy(errors = errors)
        return errors.isEmpty()
    }

    fun registerFarmer() {
        if (!validateForm()) return

        state = state.copy(isLoading = true)
        focusManager.clearFocus()

        auth.createUserWithEmailAndPassword(state.email, state.password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    // Send email verification
                    user?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                        if (emailTask.isSuccessful) {
                            Toast.makeText(
                                context,
                                "Registration successful! Please check your email for verification.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    // Save farmer data to Firestore
                    val farmerData = mapOf(
                        "farmName" to state.farmName.trim(),
                        "farmType" to state.farmType,
                        "farmSize" to state.farmSize.trim(),
                        "location" to state.farmLocation.trim(),
                        "email" to state.email,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "status" to "active",
                        "role" to "farmer"
                    )

                    user?.let { firebaseUser ->
                        db.collection("users").document(firebaseUser.uid)
                            .set(farmerData)
                            .addOnSuccessListener {
                                state = state.copy(isLoading = false)
                                navController.navigate("home") {
                                    popUpTo("register") { inclusive = true }
                                }
                            }
                            .addOnFailureListener { e ->
                                state = state.copy(isLoading = false)
                                Toast.makeText(
                                    context,
                                    "Error saving profile: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                } else {
                    state = state.copy(isLoading = false)
                    val errorMessage = when (task.exception?.message) {
                        "The email address is already in use by another account." ->
                            "This email is already registered. Try signing in instead."
                        "The password is invalid or the user does not have a password." ->
                            "Password should be at least 8 characters."
                        else -> task.exception?.message ?: "Registration failed"
                    }
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF667eea),
                        Color(0xFF764ba2)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Header
            Text(
                text = "🌱",
                fontSize = 48.sp
            )
            Text(
                text = "Join AgriConnect",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Create your farmer account",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Registration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "🚜 Create Farmer Account",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Farm Name
                    CustomTextField(
                        value = state.farmName,
                        onValueChange = ::updateFarmName,
                        label = "Farm Name",
                        placeholder = "Green Valley Organic Farm",
                        leadingIcon = Icons.Default.Home,
                        error = state.errors["farmName"],
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    // Farm Type and Size Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Farm Type Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            FarmTypeDropdown(
                                selectedType = state.farmType,
                                onTypeSelected = ::updateFarmType,
                                error = state.errors["farmType"]
                            )
                        }

                        // Farm Size
                        Box(modifier = Modifier.weight(1f)) {
                            CustomTextField(
                                value = state.farmSize,
                                onValueChange = ::updateFarmSize,
                                label = "Size (ha)",
                                placeholder = "5.0",
                                leadingIcon = Icons.Default.Edit,
                                error = state.errors["farmSize"],
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                )
                            )
                        }
                    }

                    // Farm Location
                    CustomTextField(
                        value = state.farmLocation,
                        onValueChange = ::updateFarmLocation,
                        label = "Farm Location",
                        placeholder = "123 Farm Road, Rural Valley",
                        leadingIcon = Icons.Default.LocationOn,
                        error = state.errors["farmLocation"],
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    // Email
                    CustomTextField(
                        value = state.email,
                        onValueChange = ::updateEmail,
                        label = "Email Address",
                        placeholder = "your.email@example.com",
                        leadingIcon = Icons.Default.Email,
                        error = state.errors["email"],
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    // Password with Strength Indicator
                    Column {
                        CustomTextField(
                            value = state.password,
                            onValueChange = ::updatePassword,
                            label = "Password",
                            placeholder = "Create a strong password",
                            leadingIcon = Icons.Default.Lock,
                            error = state.errors["password"],
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { registerFarmer() })
                        )

                        // Password Strength Indicator
                        AnimatedVisibility(
                            visible = state.password.isNotEmpty(),
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically()
                        ) {
                            PasswordStrengthIndicator(strength = state.passwordStrength)
                        }
                    }

                    // Submit Button
                    Button(
                        onClick = ::registerFarmer,
                        enabled = !state.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Creating Account...")
                        } else {
                            Text(
                                text = "Create Farmer Account",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Login Link
                    TextButton(
                        onClick = { navController.navigate("login") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Already have an account? Sign In",
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// Helper Composables
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    error: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions(),
    keyboardActions: KeyboardActions = KeyboardActions(),
    modifier: Modifier = Modifier
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = leadingIcon?.let { icon ->
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                }
            },
            isError = error != null,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4CAF50),
                focusedLabelColor = Color(0xFF4CAF50),
                cursorColor = Color(0xFF4CAF50)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = modifier.fillMaxWidth()
        )

        error?.let { errorText ->
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmTypeDropdown(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    error: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val farmTypes = FarmType.values()

    Column {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = if (selectedType.isNotEmpty()) {
                    farmTypes.find { it.name.lowercase() == selectedType.lowercase() }
                        ?.let { "${it.emoji} ${it.displayName}" } ?: selectedType
                } else "",
                onValueChange = { },
                readOnly = true,
                label = { Text("Farm Type") },
                placeholder = { Text("Select type") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                isError = error != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4CAF50),
                    focusedLabelColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                farmTypes.forEach { farmType ->
                    DropdownMenuItem(
                        text = { Text("${farmType.emoji} ${farmType.displayName}") },
                        onClick = {
                            onTypeSelected(farmType.name.lowercase())
                            expanded = false
                        }
                    )
                }
            }
        }

        error?.let { errorText ->
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun PasswordStrengthIndicator(strength: PasswordStrength) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = strength.color.copy(alpha = 0.1f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = strength.color,
                        shape = RoundedCornerShape(6.dp)
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${strength.label} password",
                color = strength.color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Helper function
fun calculatePasswordStrength(password: String): PasswordStrength {
    if (password.length < 8) return PasswordStrength.WEAK

    val hasNumbers = password.any { it.isDigit() }
    val hasLower = password.any { it.isLowerCase() }
    val hasUpper = password.any { it.isUpperCase() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }

    val score = listOf(hasNumbers, hasLower, hasUpper, hasSpecial).count { it }

    return when {
        score >= 3 && password.length >= 12 -> PasswordStrength.STRONG
        score >= 2 && password.length >= 8 -> PasswordStrength.MEDIUM
        else -> PasswordStrength.WEAK
    }
}