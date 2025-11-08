package com.example.spendo.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.spendo.CategoryBreakdownData
import com.example.spendo.R
import java.text.NumberFormat
import java.util.*

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
        private val categoryAmount: TextView = itemView.findViewById(R.id.tv_category_amount)
        private val categoryIcon: ImageView = itemView.findViewById(R.id.iv_category_icon)

        fun bind(item: CategoryBreakdownData) {
            categoryName.text = item.category
            val format = NumberFormat.getCurrencyInstance()
            try {
                format.currency = Currency.getInstance(currency)
            } catch (e: Exception) {
                format.currency = Currency.getInstance("LKR")
            }
            categoryAmount.text = format.format(item.amount)

            categoryIcon.setColorFilter(item.color)
        }
    }
}