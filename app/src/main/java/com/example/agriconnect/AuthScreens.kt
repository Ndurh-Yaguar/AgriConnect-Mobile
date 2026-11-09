package com.example.agriconnect

import android.widget.Toast
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

// Data Classes
data class UserRegistrationState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val wantsToSell: Boolean = false,
    // Optional farm details (if user wants to sell)
    val farmName: String = "",
    val farmType: String = "",
    val farmLocation: String = "",
    val farmDescription: String = "",
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
            Text(text = "🌱", fontSize = 48.sp)
            Text(
                text = "Welcome Back",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            TextButton(
                onClick = {
                    navController.navigate("register") {
                        popUpTo("login") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Don't have an account? Register",
                    color = Color(0xFF4CAF50)
                )
            }

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

                    TextButton(
                        onClick = { navController.navigate("signup") },
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
    var state by remember { mutableStateOf(UserRegistrationState()) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val db = FirebaseFirestore.getInstance()

    fun updateFirstName(value: String) {
        state = state.copy(firstName = value, errors = state.errors - "firstName")
    }

    fun updateLastName(value: String) {
        state = state.copy(lastName = value, errors = state.errors - "lastName")
    }

    fun updateEmail(value: String) {
        state = state.copy(email = value, errors = state.errors - "email")
    }

    fun updatePassword(value: String) {
        state = state.copy(
            password = value,
            errors = state.errors - "password",
            passwordStrength = calculatePasswordStrength(value)
        )
    }

    fun updatePhoneNumber(value: String) {
        state = state.copy(phoneNumber = value, errors = state.errors - "phoneNumber")
    }

    fun updateAddress(value: String) {
        state = state.copy(address = value, errors = state.errors - "address")
    }

    fun updateFarmName(value: String) {
        state = state.copy(farmName = value, errors = state.errors - "farmName")
    }

    fun updateFarmType(value: String) {
        state = state.copy(farmType = value, errors = state.errors - "farmType")
    }

    fun updateFarmLocation(value: String) {
        state = state.copy(farmLocation = value, errors = state.errors - "farmLocation")
    }

    fun updateFarmDescription(value: String) {
        state = state.copy(farmDescription = value)
    }

    fun validateForm(): Boolean {
        val errors = mutableMapOf<String, String>()

        if (state.firstName.trim().length < 2) {
            errors["firstName"] = "First name is required"
        }
        if (state.lastName.trim().length < 2) {
            errors["lastName"] = "Last name is required"
        }
        if (!state.email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            errors["email"] = "Please enter a valid email"
        }
        if (state.password.length < 8) {
            errors["password"] = "Password must be at least 8 characters"
        }

        // Validate farm details if user wants to sell
        if (state.wantsToSell) {
            if (state.farmName.trim().length < 2) {
                errors["farmName"] = "Farm name is required to sell"
            }
            if (state.farmType.isEmpty()) {
                errors["farmType"] = "Farm type is required to sell"
            }
            if (state.farmLocation.trim().length < 3) {
                errors["farmLocation"] = "Farm location is required to sell"
            }
        }

        state = state.copy(errors = errors)
        return errors.isEmpty()
    }

    fun registerUser() {
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
                                "Please check your email for verification!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    // Save user data to Firestore
                    val userData = mapOf(
                        "firstName" to state.firstName.trim(),
                        "lastName" to state.lastName.trim(),
                        "email" to state.email,
                        "phoneNumber" to state.phoneNumber.trim(),
                        "address" to state.address.trim(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "emailVerified" to false,
                        "isSeller" to state.wantsToSell
                    )

                    user?.let { firebaseUser ->
                        db.collection("users").document(firebaseUser.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                // If user wants to sell, create farm after email verification check
                                if (state.wantsToSell) {
                                    // Farm will be created later in FarmScreen after email verification
                                    // Store farm intent in user_private for now
                                    val farmIntent = mapOf(
                                        "pendingFarm" to mapOf(
                                            "farmName" to state.farmName.trim(),
                                            "farmType" to state.farmType,
                                            "location" to state.farmLocation.trim(),
                                            "description" to state.farmDescription.trim()
                                        )
                                    )
                                    db.collection("users_private").document(firebaseUser.uid)
                                        .set(farmIntent)
                                }

                                state = state.copy(isLoading = false)
                                Toast.makeText(
                                    context,
                                    "Account created successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
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
                    val errorMessage = when {
                        task.exception?.message?.contains("email address is already in use") == true ->
                            "This email is already registered. Try signing in instead."
                        task.exception?.message?.contains("password") == true ->
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
            Text(text = "🌱", fontSize = 48.sp)
            Text(
                text = "Join AgriConnect",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Create your account to start buying and selling",
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
                        text = "Create Account",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Personal Information
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            CustomTextField(
                                value = state.firstName,
                                onValueChange = ::updateFirstName,
                                label = "First Name",
                                placeholder = "John",
                                leadingIcon = Icons.Default.Person,
                                error = state.errors["firstName"],
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CustomTextField(
                                value = state.lastName,
                                onValueChange = ::updateLastName,
                                label = "Last Name",
                                placeholder = "Doe",
                                leadingIcon = Icons.Default.Person,
                                error = state.errors["lastName"],
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                        }
                    }

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
                                imeAction = ImeAction.Next
                            )
                        )

                        AnimatedVisibility(
                            visible = state.password.isNotEmpty(),
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically()
                        ) {
                            PasswordStrengthIndicator(strength = state.passwordStrength)
                        }
                    }

                    CustomTextField(
                        value = state.phoneNumber,
                        onValueChange = ::updatePhoneNumber,
                        label = "Phone Number (Optional)",
                        placeholder = "+27 12 345 6789",
                        leadingIcon = Icons.Default.Phone,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        )
                    )

                    CustomTextField(
                        value = state.address,
                        onValueChange = ::updateAddress,
                        label = "Address (Optional)",
                        placeholder = "123 Street, City, Province",
                        leadingIcon = Icons.Default.Home,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Seller Option
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.wantsToSell)
                                Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { state = state.copy(wantsToSell = !state.wantsToSell) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = state.wantsToSell,
                                onCheckedChange = { state = state.copy(wantsToSell = it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF4CAF50)
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "I want to sell products",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = "Create a farm profile to list your products",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Farm Details (shown if wants to sell)
                    AnimatedVisibility(
                        visible = state.wantsToSell,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "📍 Farm Details",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )

                            CustomTextField(
                                value = state.farmName,
                                onValueChange = ::updateFarmName,
                                label = "Farm Name",
                                placeholder = "Green Valley Farm",
                                leadingIcon = Icons.Default.Business,
                                error = state.errors["farmName"],
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )

                            FarmTypeDropdown(
                                selectedType = state.farmType,
                                onTypeSelected = ::updateFarmType,
                                error = state.errors["farmType"]
                            )

                            CustomTextField(
                                value = state.farmLocation,
                                onValueChange = ::updateFarmLocation,
                                label = "Farm Location",
                                placeholder = "City, Province",
                                leadingIcon = Icons.Default.LocationOn,
                                error = state.errors["farmLocation"],
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )

                            CustomTextField(
                                value = state.farmDescription,
                                onValueChange = ::updateFarmDescription,
                                label = "Farm Description (Optional)",
                                placeholder = "Tell us about your farm...",
                                leadingIcon = Icons.Default.Description,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                            )

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFF3E0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFEF6C00),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Note: You'll need to verify your email before you can create your farm and list products.",
                                        fontSize = 12.sp,
                                        color = Color(0xFFEF6C00),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Submit Button
                    Button(
                        onClick = ::registerUser,
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
                                text = "Create Account",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

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

// Helper Composables (same as before)
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
                        imageVector = Icons.Default.Category,
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