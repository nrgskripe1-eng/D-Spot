package com.mika.dspot.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mika.dspot.ui.icon.Chat
import com.mika.dspot.ui.icon.Close
import com.mika.dspot.ui.icon.Send
import com.mika.dspot.ui.icon.SimpIcons
import com.mika.dspot.ui.theme.typo
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class DSpotRoomChatMessage(
    val id: String = "",
    val sender: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
)

private const val CHAT_API_BASE = "https://dspot-web.vercel.app/api/room-chat"

@Composable
fun RoomChatSection(
    roomCode: String,
    currentUsername: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<DSpotRoomChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val httpClient = remember { HttpClient(CIO) }
    DisposableEffect(httpClient) {
        onDispose {
            httpClient.close()
        }
    }

    // Polling messages from API
    LaunchedEffect(roomCode) {
        if (roomCode.isBlank()) return@LaunchedEffect
        val json = Json { ignoreUnknownKeys = true }
        while (isActive) {
            try {
                val clean = roomCode.filter { it.isLetterOrDigit() }.uppercase()
                val response = httpClient.get("$CHAT_API_BASE?code=$clean") {
                    header("User-Agent", "D-Spot Desktop")
                }
                val body = response.bodyAsText()
                val root = json.parseToJsonElement(body).jsonObject
                val success = root["success"]?.jsonPrimitive?.toString()?.toBoolean() ?: false
                if (success) {
                    val dataArr = root["data"]?.jsonArray
                    if (dataArr != null) {
                        val parsed = dataArr.map { elem ->
                            val obj = elem.jsonObject
                            DSpotRoomChatMessage(
                                id = obj["id"]?.jsonPrimitive?.content ?: "",
                                sender = obj["sender"]?.jsonPrimitive?.content ?: "Guest",
                                text = obj["text"]?.jsonPrimitive?.content ?: "",
                                timestamp = obj["timestamp"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                            )
                        }
                        if (parsed != messages) {
                            val previousCount = messages.size
                            messages.clear()
                            messages.addAll(parsed)
                            if (parsed.size > previousCount && expanded) {
                                scope.launch {
                                    listState.animateScrollToItem((parsed.size - 1).coerceAtLeast(0))
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
            delay(2500)
        }
    }

    fun sendMessage() {
        val trimmed = inputText.trim()
        if (trimmed.isBlank() || isSending) return
        isSending = true
        scope.launch {
            try {
                val clean = roomCode.filter { it.isLetterOrDigit() }.uppercase()
                val jsonPayload = """{"roomCode":"$clean","sender":"${currentUsername.ifBlank { "Host" }}","text":${Json.encodeToString(trimmed)}}"""
                val response = httpClient.post(CHAT_API_BASE) {
                    contentType(ContentType.Application.Json)
                    setBody(jsonPayload)
                }
                val body = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }
                val root = json.parseToJsonElement(body).jsonObject
                val dataObj = root["data"]?.jsonObject
                if (dataObj != null) {
                    val newMsg = DSpotRoomChatMessage(
                        id = dataObj["id"]?.jsonPrimitive?.content ?: "",
                        sender = dataObj["sender"]?.jsonPrimitive?.content ?: currentUsername,
                        text = dataObj["text"]?.jsonPrimitive?.content ?: trimmed,
                        timestamp = dataObj["timestamp"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                    )
                    messages.add(newMsg)
                    inputText = ""
                    listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
                }
            } catch (_: Exception) {}
            finally {
                isSending = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.06f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                RoundedCornerShape(16.dp),
            )
            .padding(14.dp),
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    SimpIcons.Chat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "Room Chat",
                    style = typo().titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (messages.isNotEmpty()) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "${messages.size}",
                            style = typo().labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Text(
                if (expanded) "Hide" else "Show",
                style = typo().labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Expanded Chat Area
        if (expanded) {
            Spacer(Modifier.height(12.dp))

            // Message List
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 220.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                    .padding(8.dp),
            ) {
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No messages yet. Say hi to listeners!",
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val isMe = msg.sender.equals(currentUsername, ignoreCase = true) || msg.sender.equals("Host", ignoreCase = true)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                            ) {
                                Text(
                                    msg.sender,
                                    style = typo().labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isMe) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            },
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        msg.text,
                                        style = typo().bodySmall,
                                        color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Chat Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it.take(200) },
                    textStyle = typo().bodySmall.copy(color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    decorationBox = { innerTextField ->
                        Box {
                            if (inputText.isBlank()) {
                                Text(
                                    "Type message…",
                                    style = typo().bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                IconButton(
                    onClick = { sendMessage() },
                    enabled = inputText.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank() && !isSending) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                            },
                        ),
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            SimpIcons.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

