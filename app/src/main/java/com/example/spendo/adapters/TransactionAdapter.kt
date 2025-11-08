package com.example.spendo.adapters

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.spendo.R
import com.example.spendo.data.Transaction
import com.example.spendo.data.TransactionType
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val transactions: List<Transaction>,
    private var currency: String,
    private val onLongClick: ((Transaction, View) -> Unit)? = null
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction)
    }

    override fun getItemCount(): Int = transactions.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateCurrency(newCurrency: String) {
        currency = newCurrency
        notifyDataSetChanged()
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val ivCategoryIcon: ImageView = itemView.findViewById(R.id.iv_category_icon)

        init {
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLongClick?.invoke(transactions[position], itemView)
                }
                true
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(transaction: Transaction) {
            tvCategory.text = transaction.category
            tvDescription.text = transaction.description

            // Use the consistent currency formatting helper
            val formattedAmount = formatCurrency(transaction.amount, currency)

            if (transaction.type == TransactionType.INCOME) {
                tvAmount.text = "+ $formattedAmount"
                tvAmount.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary_green))
            } else {
                tvAmount.text = "- $formattedAmount"
                tvAmount.setTextColor(ContextCompat.getColor(itemView.context, R.color.red))
            }

            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            tvTime.text = timeFormat.format(transaction.date.toDate())

            setCategoryIcon(transaction.category)
        }

        private fun setCategoryIcon(category: String) {
            val context = itemView.context
            val iconResId: Int
            val backgroundTintId: Int

            when (category) {
                "Food" -> {
                    iconResId = R.drawable.ic_food
                    backgroundTintId = R.color.category_food
                }
                "Transportation" -> {
                    iconResId = R.drawable.ic_transport
                    backgroundTintId = R.color.category_transport
                }
                "Shopping" -> {
                    iconResId = R.drawable.ic_shopping
                    backgroundTintId = R.color.category_shopping
                }
                "Entertainment" -> {
                    iconResId = R.drawable.ic_entertainment
                    backgroundTintId = R.color.category_entertainment
                }
                "Bills" -> {
                    iconResId = R.drawable.ic_bills
                    backgroundTintId = R.color.category_bills
                }
                "Healthcare" -> {
                    iconResId = R.drawable.ic_health
                    backgroundTintId = R.color.category_health
                }
                "Education" -> {
                    iconResId = R.drawable.ic_education
                    backgroundTintId = R.color.category_education
                }
                "Salary" -> {
                    iconResId = R.drawable.ic_salary
                    backgroundTintId = R.color.category_salary
                }
                else -> {
                    iconResId = R.drawable.ic_other
                    backgroundTintId = R.color.category_other
                }
            }

            ivCategoryIcon.setImageResource(iconResId)

            val drawable = ivCategoryIcon.background as? GradientDrawable
            drawable?.setColor(ContextCompat.getColor(context, backgroundTintId))
        }
    }
}