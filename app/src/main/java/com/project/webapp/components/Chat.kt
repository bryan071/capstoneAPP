package com.project.webapp.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.project.webapp.Viewmodel.ChatViewModel
import com.project.webapp.datas.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel,
    chatRoomId: String,
    isAdmin: Boolean = false,
    notificationId: String? = null
) {
    val messages by viewModel.messages.collectAsState(initial = emptyList())
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val primaryColor = Color(0xFF0DA54B)
    val backgroundColor = Color(0xFFF8F9FA)
    val adminColor = Color(0xFFFFB74D)

    // Dynamic title based on chat type
    var chatTitle by remember { mutableStateOf("Chat") }

    // Get the other participant's ID for transaction chats
    var otherParticipantId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(chatRoomId) {
        viewModel.getChatRoomTitle(chatRoomId) { title ->
            chatTitle = title
        }

        // Get other participant ID from chat room
        if (!chatRoomId.startsWith("admin_chat_")) {
            viewModel.getOtherParticipantId(chatRoomId) { participantId ->
                otherParticipantId = participantId
                Log.d("ChatScreen", "Other participant ID: $participantId")
            }
        }
    }

    // Set chat room and mark as read
    LaunchedEffect(Unit) {
        viewModel.setChatRoomId(chatRoomId)
        viewModel.markMessagesAsRead()
    }

    // Auto-scroll to bottom
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            viewModel.markMessagesAsRead()
            delay(100)
            coroutineScope.launch {
                listState.animateScrollToItem(0)
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = chatTitle,
                                fontWeight = FontWeight.Bold
                            )
                            if (isAdmin) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = adminColor,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(backgroundColor)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    reverseLayout = true,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages.reversed(), key = { it.timestamp }) { msg ->
                        ChatBubble(msg, primaryColor, adminColor)
                    }
                }

                // Message input with proper receiver ID
                MessageInputField(
                    messageText = messageText,
                    onMessageChange = { messageText = it },
                    onSendClick = {
                        if (messageText.isNotBlank()) {
                            Log.d("ChatScreen", "Send clicked. ChatRoomId: $chatRoomId")
                            if (chatRoomId.startsWith("admin_chat_")) {
                                Log.d("ChatScreen", "Sending admin message")
                                // Admin chat - use the admin flag method
                                viewModel.sendMessage(messageText, false)
                            } else {
                                // Transaction chat - send to the other participant
                                val receiverId = otherParticipantId ?: ""
                                Log.d("ChatScreen", "Sending to participant: $receiverId")
                                if (receiverId.isNotEmpty()) {
                                    viewModel.sendMessageToParticipant(messageText, receiverId)
                                } else {
                                    Log.e("ChatScreen", "Receiver ID is empty!")
                                }
                            }
                            messageText = ""
                        }
                    },
                    primaryColor = primaryColor,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                )
            }
        }
    )
}

@Composable
fun ChatBubble(msg: ChatMessage, primaryColor: Color, adminColor: Color) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isMe = msg.senderId == currentUserId
    val isAdmin = msg.senderId == "ADMIN"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isMe) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = "https://api.dicebear.com/7.x/avataaars/svg?seed=${msg.senderId}",
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )

                if (isAdmin) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(adminColor)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "A",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.padding(end = if (!isMe) 64.dp else 0.dp, start = if (isMe) 64.dp else 0.dp)
        ) {
            val bubbleColor = when {
                isMe -> primaryColor
                isAdmin -> adminColor
                else -> Color.White
            }

            val bubbleShape = when {
                isMe -> RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                else -> RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
            }

            Surface(
                shape = bubbleShape,
                color = bubbleColor,
                shadowElevation = 1.dp,
                modifier = Modifier
            ) {
                Text(
                    text = msg.message,
                    modifier = Modifier.padding(12.dp),
                    color = if (isMe || isAdmin) Color.White else Color.Black,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = formatTimestamp(msg.timestamp),
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInputField(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = messageText,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Type a message...",
                        color = Color.Gray
                    )
                },
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = onSendClick,
                containerColor = primaryColor,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
    }
}

fun formatTimestamp(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}