package com.example.spendo

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.spendo.data.Repository
import com.example.spendo.data.Transaction
import com.example.spendo.data.TransactionType
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {
    private lateinit var repository: Repository
    private var selectedType = TransactionType.EXPENSE // Default to expense
    private var selectedDate = Date()
    private var amount: Long = 0

    // Declare view variables to avoid repeated findViewById calls
    private lateinit var amountTextView: MaterialTextView
    private lateinit var categoryEditText: TextInputEditText
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var dateEditText: TextInputEditText
    private lateinit var incomeButton: MaterialButton
    private lateinit var expenseButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        repository = Repository()

        // Initialize views once
        initViews()
        setupClickListeners()

        // Set initial state
        updateTypeButtons()
        updateDateDisplay()
        updateAmountDisplay()
    }

    private fun initViews() {
        amountTextView = findViewById(R.id.tv_amount)
        categoryEditText = findViewById(R.id.et_category)
        descriptionEditText = findViewById(R.id.et_description)
        dateEditText = findViewById(R.id.et_date)
        incomeButton = findViewById(R.id.btn_income)
        expenseButton = findViewById(R.id.btn_expense)
    }

    private fun setupClickListeners() {
        // Back button
        findViewById<View>(R.id.iv_back).setOnClickListener {
            finish()
        }

        // Amount input
        amountTextView.setOnClickListener {
            showAmountDialog()
        }

        // Category input
        categoryEditText.setOnClickListener {
            showCategoryDialog()
        }

        // Date input
        dateEditText.setOnClickListener {
            showDatePicker()
        }

        // Transaction type buttons
        incomeButton.setOnClickListener {
            selectedType = TransactionType.INCOME
            updateTypeButtons()
        }

        expenseButton.setOnClickListener {
            selectedType = TransactionType.EXPENSE
            updateTypeButtons()
        }

        // Continue button
        findViewById<View>(R.id.btn_continue).setOnClickListener {
            saveTransaction()
        }
    }

    private fun showAmountDialog() {
        val builder = AlertDialog.Builder(this)
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(if (amount > 0) amount.toString() else "")
            hint = "Enter amount in LKR"
        }

        builder.setTitle("Enter Amount")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val amountText = input.text.toString()
                amount = if (amountText.isNotBlank()) {
                    amountText.toLongOrNull() ?: 0L
                } else {
                    0L
                }
                updateAmountDisplay()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("WrongViewCast", "SetTextI18n")
    private fun updateAmountDisplay() {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "LK"))
        format.currency = Currency.getInstance("LKR")
        amountTextView.text = format.format(amount)
    }

    private fun showCategoryDialog() {
        val categories = listOf(
            "Food",
            "Transportation",
            "Shopping",
            "Entertainment",
            "Bills",
            "Healthcare",
            "Education",
            "Salary",
            "Other"
        )
        AlertDialog.Builder(this)
            .setTitle("Select Category")
            .setItems(categories.toTypedArray()) { _, which ->
                categoryEditText.setText(categories[which])
            }
            .show()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { time = selectedDate }

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
        if (selectedType == TransactionType.INCOME) {
            incomeButton.backgroundTintList = getColorStateList(R.color.primary_green)
            expenseButton.backgroundTintList = getColorStateList(R.color.light_gray)
        } else {
            incomeButton.backgroundTintList = getColorStateList(R.color.light_gray)
            expenseButton.backgroundTintList = getColorStateList(R.color.red)
        }
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        dateEditText.setText(dateFormat.format(selectedDate))
    }

    private fun saveTransaction() {
        val category = categoryEditText.text.toString()
        val description = descriptionEditText.text.toString()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        // --- Validations ---
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        if (amount <= 0) {
            Toast.makeText(this, "Please enter an amount greater than 0.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (category.isBlank()) {
            Toast.makeText(this, "Please select a category.", Toast.LENGTH_SHORT).show()
            return
        }
        // --- End of Validations ---

        // Create the transaction object
        val transaction = Transaction(
            userId = userId,
            amount = amount,
            category = category,
            description = description.ifBlank { category }, // Use category if description is empty
            type = selectedType,
            date = Timestamp(selectedDate)
        )

        // Show a loading indicator (optional but good practice)
        findViewById<View>(R.id.btn_continue).isEnabled = false

        lifecycleScope.launch {
            try {
                // Call the repository to add the transaction to Firestore
                repository.addTransaction(transaction)
                Toast.makeText(
                    this@AddTransactionActivity,
                    "Transaction saved successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                finish() // Close the activity and return to the previous screen
            } catch (e: Exception) {
                Toast.makeText(
                    this@AddTransactionActivity,
                    "Failed to save transaction: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                // Re-enable the button on failure
                findViewById<View>(R.id.btn_continue).isEnabled = true
            }
        }
    }
}