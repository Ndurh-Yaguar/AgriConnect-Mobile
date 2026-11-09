package com.example.agriconnect

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.awaitCancellation

// Data models
data class ForumPost(
    val id: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
    val reactions: Map<String, Int> = mapOf("👍" to 0, "❤️" to 0, "🌱" to 0),
    val comments: List<Comment> = emptyList(),
    val createdAt: Timestamp? = null
)

data class Comment(
    val id: String = UUID.randomUUID().toString(),
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val timestamp: Timestamp? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityForumScreen(navController: NavController) {
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    var posts by remember { mutableStateOf<List<ForumPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreatePost by remember { mutableStateOf(false) }

    // Load posts
    DisposableEffect(Unit) {
        val listenerRegistration = db.collection("forumPosts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("Forum", "Listen failed", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    posts = snapshot.documents.mapNotNull { it.toObject(ForumPost::class.java) }
                } else {
                    posts = emptyList()
                }

                isLoading = false
            }

        // Cleanup when the composable leaves composition
        onDispose {
            listenerRegistration.remove()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🌾 Community Forum") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
            if (user?.isEmailVerified == true) {
                FloatingActionButton(
                    onClick = { showCreatePost = true },
                    containerColor = Color(0xFF4CAF50)
                ) {
                    Icon(Icons.Default.Add, "Create Post", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF1F8E9))
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF4CAF50)
                    )
                }
                posts.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Forum,
                        title = "No Posts Yet",
                        subtitle = "Be the first to share your farming knowledge!"
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Email verification notice
                        if (user?.isEmailVerified != true) {
                            item {
                                Card(colors = CardDefaults.cardColors(Color(0xFFFFF3E0))) {
                                    Row(
                                        Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            null,
                                            tint = Color(0xFFEF6C00)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            "Verify your email to create posts and interact with the community",
                                            fontSize = 14.sp,
                                            color = Color(0xFFEF6C00)
                                        )
                                    }
                                }
                            }
                        }

                        items(posts) { post ->
                            ForumPostCard(
                                post = post,
                                currentUserId = user?.uid ?: "",
                                onReact = { postId, emoji ->
                                    reactToPost(db, postId, emoji)
                                },
                                onComment = { postId, comment ->
                                    addComment(db, postId, comment, user)
                                },
                                onDelete = { postId ->
                                    deletePost(db, postId)
                                },
                                canInteract = user?.isEmailVerified == true
                            )
                        }
                    }
                }
            }
        }

        // Create Post Dialog
        if (showCreatePost) {
            CreatePostDialog(
                onDismiss = { showCreatePost = false },
                onPost = { content, imageUri ->
                    showCreatePost = false
                    createPost(db, content, imageUri, user)
                }
            )
        }
    }
}

@Composable
fun ForumPostCard(
    post: ForumPost,
    currentUserId: String,
    onReact: (String, String) -> Unit,
    onComment: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    canInteract: Boolean
) {
    var showCommentInput by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    val isOwner = post.authorId == currentUserId

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF4CAF50), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        post.authorName.firstOrNull()?.uppercase() ?: "U",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.authorName, fontWeight = FontWeight.Bold)
                    Text(
                        formatTimestamp(post.createdAt),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                if (isOwner) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    onDelete(post.id)
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Content
            Text(post.content, fontSize = 16.sp)

            // Image
            if (post.imageUrl.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(12.dp))

            // Reactions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                post.reactions.forEach { (emoji, count) ->
                    Button(
                        onClick = { if (canInteract) onReact(post.id, emoji) },
                        enabled = canInteract,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF81C784),
                            disabledContainerColor = Color(0xFFE0E0E0)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("$emoji $count", color = Color.White, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.weight(1f))

                TextButton(
                    onClick = { if (canInteract) showCommentInput = !showCommentInput },
                    enabled = canInteract
                ) {
                    Icon(Icons.Default.Comment, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${post.comments.size}")
                }
            }

            // Comments
            if (post.comments.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(Color(0xFFF1F8E9))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        post.comments.forEach { comment ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(0xFF4CAF50), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        comment.authorName.firstOrNull()?.uppercase() ?: "U",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        comment.authorName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(comment.content, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Comment input
            if (showCommentInput && canInteract) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add a comment...") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onComment(post.id, commentText)
                                commentText = ""
                                showCommentInput = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, "Send", tint = Color(0xFF4CAF50))
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePostDialog(
    onDismiss: () -> Unit,
    onPost: (String, Uri?) -> Unit
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Post") },
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Share your farming knowledge...") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                if (imageUri != null) {
                    Box {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { imageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(Icons.Default.Close, "Remove", tint = Color.Red)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                OutlinedButton(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (imageUri == null) "Add Image" else "Change Image")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isNotBlank()) {
                        onPost(content, imageUri)
                    }
                },
                colors = ButtonDefaults.buttonColors(Color(0xFF4CAF50))
            ) {
                Text("Post")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions
private fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return "Just now"
    val date = timestamp.toDate()
    val now = Date()
    val diff = now.time - date.time
    val minutes = diff / 60000

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 1440 -> "${minutes / 60}h ago"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(date)
    }
}

private fun createPost(
    db: FirebaseFirestore,
    content: String,
    imageUri: Uri?,
    user: com.google.firebase.auth.FirebaseUser?
) {
    if (user == null) return

    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val imageUrl = if (imageUri != null) {
                // You'll need to pass context here - for simplicity using empty string
                ""
            } else ""

            val postId = UUID.randomUUID().toString()
            val post = hashMapOf(
                "id" to postId,
                "content" to content,
                "imageUrl" to imageUrl,
                "authorId" to user.uid,
                "authorName" to (user.email?.substringBefore("@") ?: "Anonymous"),
                "authorEmail" to user.email,
                "reactions" to mapOf("👍" to 0, "❤️" to 0, "🌱" to 0),
                "comments" to emptyList<Comment>(),
                "createdAt" to FieldValue.serverTimestamp()
            )

            db.collection("forumPosts").document(postId).set(post).await()
        } catch (e: Exception) {
            android.util.Log.e("Forum", "Error creating post", e)
        }
    }
}

private fun reactToPost(db: FirebaseFirestore, postId: String, emoji: String) {
    db.collection("forumPosts").document(postId).get()
        .addOnSuccessListener { doc ->
            val post = doc.toObject(ForumPost::class.java) ?: return@addOnSuccessListener
            val reactions = post.reactions.toMutableMap()
            reactions[emoji] = (reactions[emoji] ?: 0) + 1
            db.collection("forum_posts").document(postId).update("reactions", reactions)
        }
}

private fun addComment(
    db: FirebaseFirestore,
    postId: String,
    commentText: String,
    user: com.google.firebase.auth.FirebaseUser?
) {
    if (user == null) return

    val comment = hashMapOf(
        "id" to UUID.randomUUID().toString(),
        "authorId" to user.uid,
        "authorName" to (user.email?.substringBefore("@") ?: "Anonymous"),
        "content" to commentText,
        "timestamp" to FieldValue.serverTimestamp()
    )

    db.collection("forumPosts").document(postId)
        .update("comments", FieldValue.arrayUnion(comment))
}

private fun deletePost(db: FirebaseFirestore, postId: String) {
    db.collection("forumPosts").document(postId).delete()
}
