package com.offchat.app

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Message(val text: String, val isMine: Boolean)

class MessageAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
    ) {
        val container: LinearLayout = itemView.findViewById(R.id.messageContainer)
        val messageText: TextView = itemView.findViewById(R.id.messageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = messages[position]
        holder.messageText.text = msg.text

        if (msg.isMine) {
            holder.container.gravity = Gravity.END
            holder.messageText.setBackgroundResource(R.drawable.bg_message_mine)
            holder.messageText.setTextColor(Color.parseColor("#000000"))
        } else {
            holder.container.gravity = Gravity.START
            holder.messageText.setBackgroundResource(R.drawable.bg_message_other)
            holder.messageText.setTextColor(Color.parseColor("#FFFFFF"))
        }
    }

    override fun getItemCount() = messages.size
}
