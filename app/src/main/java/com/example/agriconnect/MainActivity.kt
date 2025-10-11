package com.example.agriconnect

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            MaterialTheme {
                val navController = rememberNavController()

                // Check if already logged in and email is verified
                val startDestination = when {
                    auth.currentUser == null -> "login"
                    else -> "home"
                }

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("login") {
                        LoginScreen(auth, navController)
                    }

                    composable("register") {
                        RegisterScreen(auth, navController)
                    }

                    composable("home") {
                        HomeScreen(auth, navController)
                    }

                    composable("farm") {
                        val farmRepository = FarmRepository()
                        FarmScreen(farmRepository = farmRepository)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(auth: FirebaseAuth, navController: NavController) {
    val user = auth.currentUser
    val emailVerified = user?.isEmailVerified ?: false

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Welcome to AgriConnect 🚜",
                style = MaterialTheme.typography.headlineMedium
            )

            user?.let {
                Text(
                    text = "Logged in as: ${it.email}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Email verification status
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (emailVerified) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (emailVerified) {
                                "✅ Email Verified"
                            } else {
                                "⚠️ Email Not Verified"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (!emailVerified) {
                            Text(
                                text = "Verify your email to access all features",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Navigate to Farm Screen
            Button(
                onClick = { navController.navigate("farm") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage Farms")
            }

            // Logout Button
            OutlinedButton(
                onClick = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
        }
    }
}