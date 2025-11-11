package com.example.spendo

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
import com.example.spendo.utils.PreferencesManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale

class EditTransactionActivity : AppCompatActivity() {

    private lateinit var repository: Repository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var transactionId: String
    private lateinit var userId: String
    private var currencyCode: String = ""
    private var amount: Long = 0
    private var selectedType: TransactionType = TransactionType.EXPENSE
    private var selectedDate: Date = Date()

    private lateinit var amountTextView: MaterialTextView
    private lateinit var categoryEditText: TextInputEditText
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var dateEditText: TextInputEditText
    private lateinit var incomeButton: MaterialButton
    private lateinit var expenseButton: MaterialButton
    private lateinit var updateButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_transaction)

        repository = Repository()
        preferencesManager = PreferencesManager.getInstance(this)

        initViews()
        setupClickListeners()
        loadTransactionFromIntent()

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
        updateButton = findViewById(R.id.btn_update)
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.iv_back).setOnClickListener { finish() }

        amountTextView.setOnClickListener { showAmountDialog() }
        categoryEditText.setOnClickListener { showCategoryDialog() }
        dateEditText.setOnClickListener { showDatePicker() }

        incomeButton.setOnClickListener {
            selectedType = TransactionType.INCOME
            updateTypeButtons()
        }

        expenseButton.setOnClickListener {
            selectedType = TransactionType.EXPENSE
            updateTypeButtons()
        }

        updateButton.setOnClickListener { saveChanges() }
    }

    private fun loadTransactionFromIntent() {
        val extras = intent.extras
        if (extras == null) {
            Toast.makeText(this, "Transaction details missing.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        transactionId = extras.getString(EXTRA_TRANSACTION_ID).orEmpty()
        userId = extras.getString(EXTRA_TRANSACTION_USER_ID).orEmpty()
        amount = extras.getLong(EXTRA_TRANSACTION_AMOUNT, 0L)
        val category = extras.getString(EXTRA_TRANSACTION_CATEGORY).orEmpty()
        val description = extras.getString(EXTRA_TRANSACTION_DESCRIPTION).orEmpty()
        val type = extras.getString(EXTRA_TRANSACTION_TYPE).orEmpty()
        val timestamp = extras.getLong(EXTRA_TRANSACTION_TIMESTAMP, Date().time)
        currencyCode = extras.getString(EXTRA_TRANSACTION_CURRENCY)
            ?: preferencesManager.getDefaultCurrency()

        if (transactionId.isBlank()) {
            Toast.makeText(this, "Invalid transaction.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        selectedType = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE)
        selectedDate = Date(timestamp)

        categoryEditText.setText(category)
        descriptionEditText.setText(description)
    }

    private fun showAmountDialog() {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(if (amount > 0) amount.toString() else "")
            hint = "Enter amount in $currencyCode"
        }

        AlertDialog.Builder(this)
            .setTitle("Enter Amount")
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

    private fun updateAmountDisplay() {
        amountTextView.text = formatAmount(amount)
    }

    private fun formatAmount(value: Long): String {
        return try {
            val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
            val currency = Currency.getInstance(currencyCode.ifBlank { preferencesManager.getDefaultCurrency() })
            formatter.currency = currency
            formatter.format(value)
        } catch (e: Exception) {
            val code = currencyCode.ifBlank { preferencesManager.getDefaultCurrency() }
            "$code ${String.format("%,d", value)}"
        }
    }

    private fun saveChanges() {
        val category = categoryEditText.text?.toString().orEmpty()
        val description = descriptionEditText.text?.toString().orEmpty()

        if (amount <= 0) {
            Toast.makeText(this, "Please enter an amount greater than 0.", Toast.LENGTH_SHORT).show()
            return
        }

        if (category.isBlank()) {
            Toast.makeText(this, "Please select a category.", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId.isBlank()) {
            Toast.makeText(this, "Unable to update transaction. User not found.", Toast.LENGTH_SHORT).show()
            return
        }

        val transaction = Transaction(
            id = transactionId,
            userId = userId,
            amount = amount,
            currency = currencyCode,
            category = category,
            description = description.ifBlank { category },
            type = selectedType,
            date = Timestamp(selectedDate)
        )

        updateButton.isEnabled = false
        lifecycleScope.launch {
            try {
                val result = repository.addTransaction(transaction)
                if (result.isSuccess) {
                    Toast.makeText(
                        this@EditTransactionActivity,
                        "Transaction updated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    val message = result.exceptionOrNull()?.message ?: "Failed to update transaction."
                    Toast.makeText(this@EditTransactionActivity, message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@EditTransactionActivity,
                    "Failed to update transaction: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                updateButton.isEnabled = true
            }
        }
    }

    companion object {
        const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
        const val EXTRA_TRANSACTION_USER_ID = "extra_transaction_user_id"
        const val EXTRA_TRANSACTION_AMOUNT = "extra_transaction_amount"
        const val EXTRA_TRANSACTION_CATEGORY = "extra_transaction_category"
        const val EXTRA_TRANSACTION_DESCRIPTION = "extra_transaction_description"
        const val EXTRA_TRANSACTION_TYPE = "extra_transaction_type"
        const val EXTRA_TRANSACTION_TIMESTAMP = "extra_transaction_timestamp"
        const val EXTRA_TRANSACTION_CURRENCY = "extra_transaction_currency"
    }
}

