package com.example.offlineaichat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// نوع الرسالة: من المستخدم ولا من النموذج (عشان نقدر نلون الفقاعات لاحقًا لو حبيت)
data class ChatMessage(val text: String, val isUser: Boolean)

class ChatAdapter(private val messages: MutableList<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.messageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val msg = messages[position]
        holder.textView.text = if (msg.isUser) "أنت: ${msg.text}" else "AI: ${msg.text}"
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    // بتستخدم هاي الدالة وقت ما النموذج عم يرجع الرد تدريجيًا (streaming)
    fun updateLastMessage(newText: String) {
        if (messages.isNotEmpty()) {
            messages[messages.size - 1] = messages.last().copy(text = newText)
            notifyItemChanged(messages.size - 1)
        }
    }
}
