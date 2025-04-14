package com.example.priyect.ui.chatbot

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.priyect.R
import com.example.priyect.ui.chatbot.data.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MessagingAdapter: RecyclerView.Adapter<MessagingAdapter.MessageViewHolder>() {

    var messagesList = mutableListOf<Message>()

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {

                //Remove message on the item clicked
                messagesList.removeAt(adapterPosition)
                notifyItemRemoved(adapterPosition)
            }
        }
        //Inicializamos las variables que contiene el item de la recycler view
        val textViewMessage = itemView.findViewById<TextView>(R.id.textView_message)
        val textViewMessageBot = itemView.findViewById<TextView>(R.id.textView_messageBot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return MessageViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.message_item, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return messagesList.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val currentMessage = messagesList[position]

        when (currentMessage.id) {
            Chatbot2.SEND_ID -> {
                holder.textViewMessage.apply {
                    println("El current message es ${currentMessage.message}")
                    text = currentMessage.message
                    visibility = View.VISIBLE
                }
                holder.textViewMessageBot.visibility = View.GONE
            }
            Chatbot2.RECEIVE_ID -> {
                holder.textViewMessageBot.apply {
                    println("El currenbt message es ${currentMessage.message}")
                    text = currentMessage.message
                    visibility = View.VISIBLE
                }
                holder.textViewMessage.visibility = View.GONE
            }
        }
    }

    suspend fun insertMessage(message: Message) {
        withContext(Dispatchers.Main) {
            messagesList.add(message)
            notifyItemInserted(messagesList.size)
        }

    }

}