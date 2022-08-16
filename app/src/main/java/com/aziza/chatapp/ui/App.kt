package com.aziza.chatapp


import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

// todo 1 change screen background
// todo 2 fix message sending logic
// todo 3 make message appear using LazyColumn

val wsClient = WsClient(HttpClient {
    install(WebSockets)
})
val job by lazy {
    GlobalScope.launch {
        wsClient.connect()
    }

}

@SuppressLint("UnrememberedMutableState", "CoroutineCreationDuringComposition")
@Composable
fun App() {

    var text by remember { mutableStateOf("Send Message!") }
    var sentMessage by remember { mutableStateOf(TextFieldValue("your Message!")) }
    var receivedMessage by remember { mutableStateOf("received") }
    val list = mutableListOf<String>()
    var chat by mutableStateOf(list)
    val platformName = "Android"
    var mList: List<Int> by remember {  mutableStateOf (listOf()) }

    if (!job.isActive)
        job.start()

    suspend fun startChat(wsClient: WsClient) {
        try {
            wsClient.receive {
                //  writeMessage(it)
                list.add(it)
                println("startChat: $chat")
            }
        } catch (e: Exception) {
            if (e is ClosedReceiveChannelException) {
                writeMessage("Disconnected. ${e.message}.")
            } else if (e is WebSocketException) {
                writeMessage("Unable to connect.")
            }
            withTimeout(5000) {
                GlobalScope.launch { startChat(wsClient) }
            }
        }
    }

    GlobalScope.launch { startChat(wsClient) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFF9800))
    ) {


        LazyColumn(Modifier.weight(1f)) {
            list.add(sentMessage.text)
            items(
                items = list + listOf("hello", "Ahmed", " Salaaaaam", "Kotlin"),
                itemContent = { item ->

                    Card(
                        modifier = Modifier
                            .padding(3.dp)
                            .width(300.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(5.dp),
                        backgroundColor = Color(0xFF4B4237)
                    ) {

                        Text(
                           item,
                            color = Color.White,
                            style = Typography().body1,
                            modifier = Modifier.padding(7.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                })
        }
        Row(verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .then(Modifier.padding(30.dp))
                    .background(Color.White),
                value = sentMessage,
                onValueChange = {
                    sentMessage = it

                })
            Spacer(modifier = Modifier.width(10.dp))
            Button(modifier = Modifier
                .height(80.dp)
                .width(110.dp)
                .padding(bottom = 30.dp, end = 30.dp), colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(
                    0xFF4B4237
                )
            ),
                onClick = {
                    GlobalScope.launch {

                        sendMessage(wsClient, sentMessage.text)
                        list.add(sentMessage.text)
                      

                        Log.e("TAG", "App:${list} ")
                        Log.e("TAG", "App: ${sentMessage.text}")
                    }
                }) {
                Text("Send", color = Color.White)
            }
        }

    }
}


fun receiveFlow(message: String) = flow<String> {
    emit(message)
}

suspend fun initConnection(wsClient: WsClient) {
    Log.e("TAG", "befoinitConnection: ", )
    try {
        wsClient.connect()
        wsClient.receive {
            writeMessage(it)
            receiveFlow(it)
            Log.e("TAG", "oooinitConnection: ", )
        }
    } catch (e: Exception) {
        if (e is ClosedReceiveChannelException) {
            writeMessage("Disconnected. ${e.message}.")
        } else if (e is WebSocketException) {
            writeMessage("Unable to connect.")
        }
        withTimeout(5000) {
            GlobalScope.launch {
                Log.e("TAG", "afteeerinitConnection: ", )
                initConnection(wsClient) }
        }
    }
}

suspend fun sendMessage(client: WsClient, input: String) {
    if (input.isNotEmpty()) {
        client.send(input)

    }
}


fun writeMessage(message: String, messageCallback: ((String) -> Unit) = {}) {
    messageCallback(message)
}

class WsClient(private val client: HttpClient) {
    var session: WebSocketSession? = null

    suspend fun connect() {
        session = client.webSocketSession(
            method = HttpMethod.Get,
            host = "10.0.2.2",
            port = 8080,
            path = "/chat"
        )
    }

    suspend fun send(message: String) {
        session?.send(Frame.Text(message))
    }


    suspend fun receive(onReceive: (input: String) -> Unit) {
        while (true) {
            val frame = session?.incoming?.receive()

            if (frame is Frame.Text) {
                onReceive(frame.readText())
            }
        }
    }
}

