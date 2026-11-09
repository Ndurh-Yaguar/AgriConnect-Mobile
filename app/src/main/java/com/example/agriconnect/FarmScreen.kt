package com.example.agriconnect

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun FarmScreen(farmRepository: FarmRepository) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var farmName by remember { mutableStateOf(TextFieldValue("")) }
    var farmLocation by remember { mutableStateOf(TextFieldValue("")) }
    var loading by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Add Your Farm",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = farmName,
            onValueChange = { farmName = it },
            label = { Text("Farm Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = farmLocation,
            onValueChange = { farmLocation = it },
            label = { Text("Farm Location") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Toast.makeText(context, "Please log in first.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val name = farmName.text.trim()
                val location = farmLocation.text.trim()

                if (name.isEmpty() || location.isEmpty()) {
                    errorMessage = "All fields are required."
                    return@Button
                }

                loading = true

                val farmData = mapOf(
                    "name" to name,
                    "location" to location,
                    "ownerId" to userId
                )

                db.collection("farms")
                    .add(farmData)
                    .addOnSuccessListener {
                        loading = false
                        successMessage = "Farm added successfully!"
                        errorMessage = null
                        farmName = TextFieldValue("")
                        farmLocation = TextFieldValue("")
                    }
                    .addOnFailureListener { e ->
                        loading = false
                        errorMessage = e.localizedMessage ?: "Failed to add farm."
                        successMessage = null
                    }
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            Text(if (loading) "Saving..." else "Save Farm")
        }

        Spacer(modifier = Modifier.height(16.dp))

        successMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary
            )
        }

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
