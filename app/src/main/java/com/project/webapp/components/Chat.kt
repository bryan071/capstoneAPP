package com.project.webapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.project.webapp.Viewmodel.ChatViewModel
import com.project.webapp.datas.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, viewModel: ChatViewModel, chatRoomId: String, isAdmin: Boolean) {
    val messages by viewModel.messages.collectAsState(initial = emptyList())
    var messageText by remember { mutableStateOf("") }

    // Set chat room when entering
    LaunchedEffect(Unit) {
        viewModel.setChatRoomId(chatRoomId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF5F5F5)) // Light background
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    reverseLayout = true // Messages appear from bottom to top
                ) {
                    items(messages.reversed()) { msg ->
                        ChatBubble(msg)
                    }
                }

                MessageInputField(
                    messageText = messageText,
                    onMessageChange = { messageText = it },
                    onSendClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText, isAdmin)
                            messageText = ""
                        }
                    }
                )
            }
        }
    )
}

// Chat Bubble UI

@Composable
fun ChatBubble(msg: ChatMessage) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isMe = msg.senderId == currentUserId
    val isAdmin = msg.senderId == "ADMIN"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isMe) {
            AsyncImage(
                model = "https://api.dicebear.com/7.x/avataaars/svg?seed=${msg.senderId}",
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = when {
                    isMe -> Color(0xFF0DA54B)
                    isAdmin -> Color(0xFFFFC107) // Yellow for admin messages
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = msg.message,
                    modifier = Modifier.padding(12.dp),
                    color = if (isMe) Color.White else Color.Black
                )
            }

            Text(
                text = formatTimestamp(msg.timestamp),
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.align(if (isMe) Alignment.End else Alignment.Start)
            )
        }
    }
}

// Message Input Field with Send Button
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInputField(messageText: String, onMessageChange: (String) -> Unit, onSendClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = messageText,
            onValueChange = onMessageChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message...") },
            maxLines = 3,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            )
        )

        IconButton(onClick = onSendClick) {
            Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color(0xFF0DA54B)) // Green color
        }
    }
}


// Function to format timestamp
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
