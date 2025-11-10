package com.example.spendo.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.spendo.CategoryBreakdownData
import com.example.spendo.R
import com.example.spendo.utils.CurrencyFormatter

class CategoryBreakdownAdapter(private var data: List<CategoryBreakdownData>) : 
    RecyclerView.Adapter<CategoryBreakdownAdapter.CategoryViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_breakdown, parent, false)
        return CategoryViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(data[position])
    }
    
    override fun getItemCount(): Int = data.size
    
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<CategoryBreakdownData>) {
        data = newData
        notifyDataSetChanged()
    }
    
    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryDot: View = itemView.findViewById(R.id.iv_category_dot)
        private val categoryName: TextView = itemView.findViewById(R.id.tv_category_name)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)
        private val amount: TextView = itemView.findViewById(R.id.tv_amount)
        
        fun bind(data: CategoryBreakdownData) {
            categoryName.text = data.category
            val formattedAmount = CurrencyFormatter.formatAmountWithCode(itemView.context, data.amount)
            amount.text = "- $formattedAmount"
            categoryDot.setBackgroundColor(data.color)
            
            // Calculate progress percentage (simplified)
            val maxAmount = if (data.amount > 0) data.amount * 2 else 1L
            val progress = ((data.amount * 100) / maxAmount).toInt()
            progressBar.progress = progress
            progressBar.progressTintList = android.content.res.ColorStateList.valueOf(data.color)
        }
    }
}
