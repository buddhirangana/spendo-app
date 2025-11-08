package com.example.spendo.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.content.res.ColorStateList
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.spendo.CategoryBreakdownData
import com.example.spendo.R
import java.text.NumberFormat
import java.util.Currency

class CategoryBreakdownAdapter(
    private var data: List<CategoryBreakdownData>,
    private var currency: String
) : RecyclerView.Adapter<CategoryBreakdownAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_breakdown, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = data.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<CategoryBreakdownData>) {
        data = newData
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateCurrency(newCurrency: String) {
        currency = newCurrency
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryName: TextView = itemView.findViewById(R.id.tv_category_name)
        private val categoryAmount: TextView = itemView.findViewById(R.id.tv_amount)
        private val categoryDot: View = itemView.findViewById(R.id.iv_category_dot)

        fun bind(item: CategoryBreakdownData) {
            categoryName.text = item.category
            // Use a helper for consistent currency formatting
            categoryAmount.text = formatCurrency(item.amount, currency)
            // Set the background tint color for the category dot
            ViewCompat.setBackgroundTintList(categoryDot, ColorStateList.valueOf(item.color))
        }
    }
}

// Helper function for consistent currency formatting
fun formatCurrency(amount: Number, currencyCode: String): String {
    val amountDouble = amount.toDouble()
    return try {
        val format = NumberFormat.getCurrencyInstance()
        format.currency = Currency.getInstance(currencyCode)
        format.format(amountDouble)
    } catch (e: Exception) {
        // Fallback if the currency code is invalid
        val format = NumberFormat.getCurrencyInstance()
        format.currency = Currency.getInstance("LKR")
        format.format(amountDouble)
    }
}