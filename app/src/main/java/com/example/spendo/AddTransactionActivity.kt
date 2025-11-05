package com.example.spendo

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.spendo.data.Repository
import com.example.spendo.data.Transaction
import com.example.spendo.data.TransactionType
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {
    private lateinit var repository: Repository
    private var selectedType = TransactionType.EXPENSE
    private var selectedDate = Date()
    private var amount = 0L
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)
        
        repository = Repository()
        
        setupViews()
    }
    
    private fun setupViews() {
        // Back button
        findViewById<View>(R.id.iv_back).setOnClickListener {
            finish()
        }
        
        // Amount input
        findViewById<View>(R.id.tv_amount).setOnClickListener {
            showAmountDialog()
        }
        
        // Category input
        findViewById<View>(R.id.et_category).setOnClickListener {
            showCategoryDialog()
        }
        
        // Date input
        findViewById<View>(R.id.et_date).setOnClickListener {
            showDatePicker()
        }
        
        // Transaction type buttons
        findViewById<View>(R.id.btn_income).setOnClickListener {
            selectedType = TransactionType.INCOME
            updateTypeButtons()
        }
        
        findViewById<View>(R.id.btn_expense).setOnClickListener {
            selectedType = TransactionType.EXPENSE
            updateTypeButtons()
        }
        
        // Continue button
        findViewById<View>(R.id.btn_continue).setOnClickListener {
            saveTransaction()
        }
        
        updateTypeButtons()
        updateDateDisplay()
    }
    
    private fun showAmountDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.setText(if (amount > 0) amount.toString() else "")
        input.hint = "Enter amount"
        
        builder.setTitle("Enter Amount")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val amountText = input.text.toString()
                if (amountText.isNotBlank()) {
                    amount = amountText.toLongOrNull() ?: 0L
                    updateAmountDisplay()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    @SuppressLint("WrongViewCast", "SetTextI18n")
    private fun updateAmountDisplay() {
        val currentAmount = findViewById<com.google.android.material.textview.MaterialTextView>(R.id.tv_amount)
        currentAmount.text = "LKR ${NumberFormat.getInstance().format(amount)}"
    }
    
    private fun showCategoryDialog() {
        val categories = listOf("Food", "Transportation", "Shopping", "Entertainment", "Bills", "Healthcare", "Education", "Other")
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Select Category")
        builder.setItems(categories.toTypedArray()) { _, which ->
            findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_category).setText(categories[which])
        }
        builder.show()
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance()
                newCalendar.set(year, month, dayOfMonth)
                selectedDate = newCalendar.time
                updateDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun updateTypeButtons() {
        val incomeBtn = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_income)
        val expenseBtn = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_expense)
        
        if (selectedType == TransactionType.INCOME) {
            incomeBtn.setBackgroundColor(getColor(R.color.primary_green))
            expenseBtn.setBackgroundColor(getColor(R.color.light_gray))
        } else {
            incomeBtn.setBackgroundColor(getColor(R.color.light_gray))
            expenseBtn.setBackgroundColor(getColor(R.color.red))
        }
    }
    
    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_date).setText(dateFormat.format(selectedDate))
    }
    
    private fun saveTransaction() {
        val category = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_category).text.toString()
        val description = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_description).text.toString()
        
        if (category.isBlank()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (amount <= 0) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
            return
        }
        
        val transaction = Transaction(
            userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "",
            amount = amount,
            category = category,
            description = description,
            type = selectedType,
            date = Timestamp(selectedDate)
        )
        
        lifecycleScope.launch {
            try {
                repository.addTransaction(transaction)
                Toast.makeText(this@AddTransactionActivity, "Transaction saved!", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddTransactionActivity, "Failed to save transaction: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
