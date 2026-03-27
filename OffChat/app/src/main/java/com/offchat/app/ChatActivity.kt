package com.offchat.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.offchat.app.databinding.ActivityChatBinding
import kotlinx.coroutines.*
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val messages = mutableListOf<Message>()
    private lateinit var messageAdapter: MessageAdapter

    private var socket: Socket? = null
    private var outputStream: PrintWriter? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val PORT = 8888
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isGroupOwner = intent.getBooleanExtra("isGroupOwner", false)
        val groupOwnerAddress = intent.getStringExtra("groupOwnerAddress") ?: ""

        messageAdapter = MessageAdapter(messages)
        val layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.chatRecyclerView.layoutManager = layoutManager
        binding.chatRecyclerView.adapter = messageAdapter

        binding.sendButton.setOnClickListener {
            val text = binding.messageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                binding.messageInput.setText("")
            }
        }

        if (isGroupOwner) startServer() else connectToServer(groupOwnerAddress)
    }

    private fun startServer() {
        binding.statusText.text = "Waiting..."
        scope.launch {
            try {
                val serverSocket = ServerSocket(PORT)
                socket = serverSocket.accept()
                serverSocket.close()
                setupStreams()
                withContext(Dispatchers.Main) { binding.statusText.text = "Connected ✓" }
                listenForMessages()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun connectToServer(address: String) {
        binding.statusText.text = "Connecting..."
        scope.launch {
            for (attempt in 1..5) {
                try {
                    val s = Socket()
                    s.connect(InetSocketAddress(address, PORT), 5000)
                    socket = s
                    setupStreams()
                    withContext(Dispatchers.Main) { binding.statusText.text = "Connected ✓" }
                    listenForMessages()
                    return@launch
                } catch (e: Exception) {
                    delay(2000)
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ChatActivity,
                    "Could not connect. Go back and try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupStreams() {
        outputStream = PrintWriter(
            BufferedWriter(OutputStreamWriter(socket!!.getOutputStream())), true
        )
    }

    private fun listenForMessages() {
        val reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val msg = line ?: continue
                runOnUiThread {
                    messages.add(Message(msg, isMine = false))
                    messageAdapter.notifyItemInserted(messages.size - 1)
                    binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
        } catch (e: Exception) {
            runOnUiThread { binding.statusText.text = "Disconnected" }
        }
    }

    private fun sendMessage(text: String) {
        if (outputStream == null) {
            Toast.makeText(this, "Not connected yet", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch(Dispatchers.IO) { outputStream?.println(text) }
        messages.add(Message(text, isMine = true))
        messageAdapter.notifyItemInserted(messages.size - 1)
        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        socket?.close()
    }
}
