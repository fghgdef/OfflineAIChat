package com.example.offlineaichat

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    // اسم ملف الموديل. لازم يكون موجود بمجلد assets بصيغة GGUF
    // مثال: Qwen2.5-Coder-2B-Instruct-Q4_K_M.gguf (أو أي اسم ملف Qwen يلي حمّلته)
    private val modelFileName = "qwen-model.gguf"

    private val llama = LlamaBridge()
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    private lateinit var statusText: TextView
    private lateinit var inputEditText: EditText
    private lateinit var sendButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        inputEditText = findViewById(R.id.inputEditText)
        sendButton = findViewById(R.id.sendButton)

        val recyclerView = findViewById<RecyclerView>(R.id.chatRecyclerView)
        adapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        sendButton.setOnClickListener {
            val text = inputEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                inputEditText.text.clear()
            }
        }

        loadModel()
    }

    private fun loadModel() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val modelPath = prepareModelFile()
                val threadCount = Runtime.getRuntime().availableProcessors().coerceAtMost(6)

                val ok = llama.loadModel(modelPath, nThreads = threadCount, nCtx = 2048)

                withContext(Dispatchers.Main) {
                    if (ok) {
                        statusText.text = "جاهز! اكتب رسالتك"
                        inputEditText.isEnabled = true
                        sendButton.isEnabled = true
                    } else {
                        statusText.text = "فشل تحميل الموديل"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "فشل تحميل الموديل: ${e.message}"
                    Toast.makeText(
                        this@MainActivity,
                        "تأكد إنك حطيت ملف $modelFileName بمجلد assets",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // بينسخ ملف الموديل من assets لمساحة تخزين التطبيق الداخلية (لازم مسار ملف حقيقي على القرص)
    private fun prepareModelFile(): String {
        val outFile = File(filesDir, modelFileName)
        if (!outFile.exists()) {
            assets.open(modelFileName).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return outFile.absolutePath
    }

    private fun sendMessage(text: String) {
        adapter.addMessage(ChatMessage(text, isUser = true))
        adapter.addMessage(ChatMessage("...", isUser = false))

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // توليد الرد محليًا بالكامل عبر llama.cpp، بدون أي اتصال إنترنت
                val response = llama.generate(text, maxTokens = 256)
                withContext(Dispatchers.Main) {
                    adapter.updateLastMessage(response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    adapter.updateLastMessage("صار خطأ: ${e.message}")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        llama.free()
    }
}
