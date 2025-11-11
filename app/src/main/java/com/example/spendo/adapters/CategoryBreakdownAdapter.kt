package com.example.spendo.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.spendo.CategoryBreakdownData
import com.example.spendo.R
import com.example.spendo.utils.CurrencyFormatter

class CategoryBreakdownAdapter(
    private var data: List<CategoryBreakdownData>,
    private var isExpense: Boolean = true
) : 
    RecyclerView.Adapter<CategoryBreakdownAdapter.CategoryViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_breakdown, parent, false)
        return CategoryViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(data[position], isExpense)
    }
    
    override fun getItemCount(): Int = data.size
    
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<CategoryBreakdownData>, isExpense: Boolean) {
        this.data = newData
        this.isExpense = isExpense
        notifyDataSetChanged()
    }
    
    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryDot: View = itemView.findViewById(R.id.iv_category_dot)
        private val categoryName: TextView = itemView.findViewById(R.id.tv_category_name)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)
        private val amount: TextView = itemView.findViewById(R.id.tv_amount)
        
        fun bind(data: CategoryBreakdownData, isExpense: Boolean) {
            categoryName.text = data.category
            val formattedAmount = CurrencyFormatter.formatAmountWithCode(itemView.context, data.amount)

            if (isExpense) {
                amount.text = "- $formattedAmount"
                amount.setTextColor(ContextCompat.getColor(itemView.context, R.color.red))
            } else {
                amount.text = "+ $formattedAmount"
                amount.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary_green))
            }

            categoryDot.setBackgroundColor(data.color)
            
            val totalAmount = data.amount 
            val maxAmount = if (totalAmount > 0) totalAmount * 2 else 1L 
            val progress = ((totalAmount * 100) / maxAmount).toInt()
            progressBar.progress = progress
            progressBar.progressTintList = android.content.res.ColorStateList.valueOf(data.color)
        }
    }
}
