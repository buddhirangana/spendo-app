package com.example.spendo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.spendo.R
import com.example.spendo.data.Transaction
import com.example.spendo.data.TransactionType
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(private val transactions: List<Transaction>) : 
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }
    
    override fun getItemCount(): Int = transactions.size
    
    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryIcon: View = itemView.findViewById(R.id.iv_category_icon)
        private val categoryText: TextView = itemView.findViewById(R.id.tv_category)
        private val descriptionText: TextView = itemView.findViewById(R.id.tv_description)
        private val amountText: TextView = itemView.findViewById(R.id.tv_amount)
        private val timeText: TextView = itemView.findViewById(R.id.tv_time)
        
        fun bind(transaction: Transaction) {
            categoryText.text = transaction.category
            descriptionText.text = transaction.description
            
            val amount = String.format("%,d", transaction.amount)
            val prefix = if (transaction.type == TransactionType.INCOME) "+" else "-"
            amountText.text = "$prefix LKR $amount"
            amountText.setTextColor(
                if (transaction.type == TransactionType.INCOME) 
                    itemView.context.getColor(R.color.primary_green)
                else 
                    itemView.context.getColor(R.color.red)
            )
            
            // Format time
            val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            timeText.text = dateFormat.format(transaction.date.toDate())
            
            // Set category icon background color
            val colors = mapOf(
                "Food" to R.color.red,
                "Transportation" to R.color.blue,
                "Shopping" to R.color.orange,
                "Entertainment" to R.color.yellow,
                "Bills" to R.color.primary_green,
                "Healthcare" to R.color.blue,
                "Education" to R.color.orange,
                "Other" to R.color.gray
            )
            
            val colorRes = colors[transaction.category] ?: R.color.gray
            categoryIcon.setBackgroundColor(itemView.context.getColor(colorRes))
        }
    }
}


