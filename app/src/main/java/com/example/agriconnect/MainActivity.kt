package com.example.agriconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation(auth)
                }

                }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth) {
    val navController = rememberNavController()
    val startDestination = if (auth.currentUser != null) "home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        // Authentication
        composable("login") {
            LoginScreen(auth, navController)
        }

        composable("signup") {
            RegisterScreen(auth, navController)
        }

        // Home
        composable("home") {
            HomeScreenWithNavigation(auth, navController)
        }

        // Farm Management
        composable("farm_list") {
            FarmListScreen(navController)
        }

        composable("farm_create") {
            val farmRepository = FarmRepository()
            FarmCreationScreen(farmRepository, navController)
        }

        // Product Upload
        composable("product_upload/{farmId}/{farmName}") { backStackEntry ->
            val farmId = backStackEntry.arguments?.getString("farmId") ?: ""
            val farmName = backStackEntry.arguments?.getString("farmName") ?: ""
            ProductUploadScreen(farmId, farmName, navController)
        }

        // Admin Dashboard
        composable("admin_dashboard") {
            AdminDashboardScreen(navController)
        }

        // Marketplace
        composable("marketplace") {
            MarketplaceScreen()
        }

        // 🆕 Auctions
        composable("auctions") {
            AuctionScreen(navController)
        }

        composable("create_auction") {
            CreateAuctionScreen(navController)
        }

        // 🆕 Community Forum
        composable("community_forum") {
            CommunityForumScreen(navController)
        }
    }
}