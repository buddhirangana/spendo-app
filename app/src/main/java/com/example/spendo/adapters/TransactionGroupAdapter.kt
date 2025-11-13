package com.example.spendo.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spendo.R
import com.example.spendo.data.Transaction

class TransactionGroupAdapter(
    private var data: Map<String, List<Transaction>>,
    private val listener: TransactionAdapter.TransactionActionListener? = null
) :
    RecyclerView.Adapter<TransactionGroupAdapter.GroupViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction_group, parent, false)
        return GroupViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val dateKey = data.keys.elementAt(position)
        val transactions = data[dateKey] ?: emptyList()
        holder.bind(dateKey, transactions)
    }
    
    override fun getItemCount(): Int = data.size
    
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: Map<String, List<Transaction>>) {
        data = newData
        notifyDataSetChanged()
    }
    
    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateHeader: TextView = itemView.findViewById(R.id.tv_date_header)
        private val transactionsRecycler: RecyclerView = itemView.findViewById(R.id.rv_transactions_in_group)
        
        fun bind(dateKey: String, transactions: List<Transaction>) {
            dateHeader.text = dateKey
            
            val adapter = TransactionAdapter(transactions, listener)
            transactionsRecycler.apply {
                layoutManager = LinearLayoutManager(itemView.context)
                this.adapter = adapter
            }
        }
    }
}