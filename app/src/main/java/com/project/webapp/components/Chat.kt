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
import com.google.firebase.firestore.FirebaseFirestore
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
    val orgColor = Color(0xFFE91E63) // Pink for Organization

    // Dynamic title & metadata
    var chatTitle by remember { mutableStateOf("Chat") }
    var otherParticipantId by remember { mutableStateOf<String?>(null) }
    var chatType by remember { mutableStateOf("normal") } // normal, admin, donation_org

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Detect chat type
    LaunchedEffect(chatRoomId) {
        chatType = when {
            chatRoomId.startsWith("admin_chat_") -> "admin"
            chatRoomId.startsWith("donation_org_") -> "donation_org"
            else -> "normal"
        }

        when (chatType) {
            "admin" -> {
                chatTitle = "Admin Support"
            }
            "donation_org" -> {
                // We set the title ourselves — don't let getChatRoomTitle() override it!
                FirebaseFirestore.getInstance()
                    .collection("donationOrgChats")
                    .document(chatRoomId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val orgName = doc.getString("organizationName") ?: "Organization"
                        val donorName = doc.getString("donorName") ?: "Donor"
                        val donorId = doc.getString("donorId")
                        chatTitle = if (currentUserId == donorId) {
                            "$orgName Team"
                        } else {
                            donorName
                        }
                    }
            }
            else -> {
                // Only normal user-to-user chats use the ViewModel title fetcher
                viewModel.getChatRoomTitle(chatRoomId) { title ->
                    chatTitle = title
                }
                viewModel.getOtherParticipantId(chatRoomId) { id ->
                    otherParticipantId = id
                }
            }
        }
    }

    // Set chat room & mark read
    LaunchedEffect(chatRoomId) {
        viewModel.setChatRoomId(chatRoomId, isAdminChat = (chatType == "admin"))
        viewModel.markMessagesAsRead(isAdminChat = (chatType == "admin"))
    }

    // Auto-scroll
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 4.dp) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = chatTitle, fontWeight = FontWeight.Bold)
                            when (chatType) {
                                "admin" -> {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(color = adminColor, shape = RoundedCornerShape(4.dp)) {
                                        Text("ADMIN", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                "donation_org" -> {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(color = orgColor, shape = RoundedCornerShape(4.dp)) {
                                        Text("ORG", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
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
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(backgroundColor)) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)
            ) {
                items(messages.reversed(), key = { it.id }) { msg ->
                    ChatBubble(
                        message = msg,
                        currentUserId = currentUserId,
                        chatType = chatType,
                        primaryColor = primaryColor,
                        adminColor = adminColor,
                        orgColor = orgColor
                    )
                }
            }

            MessageInputField(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        when (chatType) {
                            "admin" -> viewModel.sendAdminMessage(messageText)
                            "donation_org" -> viewModel.sendDonationOrgMessage(messageText)
                            else -> {
                                val receiverId = otherParticipantId ?: ""
                                if (receiverId.isNotEmpty()) {
                                    viewModel.sendMessageToParticipant(messageText, receiverId)
                                }
                            }
                        }
                        messageText = ""
                    }
                },
                primaryColor = primaryColor,
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
            )
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    currentUserId: String?,
    chatType: String,
    primaryColor: Color,
    adminColor: Color,
    orgColor: Color
) {
    val isMe = message.senderId == currentUserId
    val isOrgAdmin = message.senderId == "ORG_ADMIN" || (chatType == "donation_org" && !isMe)
    val isAppAdmin = message.senderId == "ADMIN" || (chatType == "admin" && !isMe)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isMe) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = when {
                        isOrgAdmin -> "https://api.dicebear.com/7.x/avataaars/svg?seed=org"
                        isAppAdmin -> "https://api.dicebear.com/7.x/avataaars/svg?seed=admin"
                        else -> "https://api.dicebear.com/7.x/avataaars/svg?seed=${message.senderId}"
                    },
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )

                if (isOrgAdmin || isAppAdmin) {
                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd).size(16.dp)
                            .clip(CircleShape).background(if (isOrgAdmin) orgColor else adminColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isOrgAdmin) "O" else "A",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            // Sender name (only show for others)
            if (!isMe) {
                Text(
                    text = when {
                        isOrgAdmin -> "Organization Admin"
                        isAppAdmin -> "App Admin"
                        else -> message.senderName.ifEmpty { "User" }
                    },
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }

            Surface(
                shape = if (isMe) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                color = when {
                    isMe -> primaryColor
                    isOrgAdmin -> orgColor
                    isAppAdmin -> adminColor
                    else -> Color.White
                },
                shadowElevation = 2.dp
            ) {
                Text(
                    text = message.message,
                    modifier = Modifier.padding(12.dp),
                    color = if (isMe || isOrgAdmin || isAppAdmin) Color.White else Color.Black,
                    fontSize = 16.sp
                )
            }

            Text(
                text = formatTimestamp(message.timestamp),
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 2.dp)
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
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = messageText,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...", color = Color.Gray) },
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(Modifier.width(8.dp))
            FloatingActionButton(
                onClick = onSendClick,
                containerColor = primaryColor,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

fun formatTimestamp(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}