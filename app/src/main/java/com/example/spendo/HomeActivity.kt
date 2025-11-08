package com.example.spendo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spendo.adapters.TransactionAdapter
import com.example.spendo.data.Repository
import com.example.spendo.data.Transaction
import com.example.spendo.data.TransactionType
import com.example.spendo.utils.CurrencyFormatter
import com.example.spendo.utils.LoadingHelper
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar

class HomeActivity : AppCompatActivity() {

    private lateinit var btnMonth: MaterialButton
    private val months = DateFormatSymbols().months
    private var selectedMonthIndex = 0

    private lateinit var repository: Repository
    private lateinit var transactionAdapter: TransactionAdapter
    private var transactions = mutableListOf<Transaction>()
    private lateinit var loadingHelper: LoadingHelper
    private var isLoading = false

    private lateinit var ivProfile: CircleImageView
    private lateinit var rvRecentTransactions: RecyclerView
    private lateinit var tvBalance: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpenses: TextView
    private lateinit var lineChart: LineChart
    private lateinit var btnToday: MaterialButton
    private lateinit var btnWeek: MaterialButton
    private lateinit var btnMonth2: MaterialButton
    private lateinit var btnYear: MaterialButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize repository
        repository = Repository()
        loadingHelper = LoadingHelper(this)

        // Initialize all views
        initViews()
        setupClickListeners()

        // Set initial state
        val calendar = Calendar.getInstance()
        selectedMonthIndex = calendar.get(Calendar.MONTH)
        updateMonthButtonText()
        setupChart()

        // Load data for the first time
        loadProfilePicture()
        loadData()
    }

    private fun initViews() {
        ivProfile = findViewById(R.id.iv_profile)
        btnMonth = findViewById(R.id.btn_month)
        rvRecentTransactions = findViewById(R.id.rv_recent_transactions)
        tvBalance = findViewById(R.id.tv_balance)
        tvIncome = findViewById(R.id.tv_income)
        tvExpenses = findViewById(R.id.tv_expenses)
        lineChart = findViewById(R.id.chart)
        btnToday = findViewById(R.id.btn_today)
        btnWeek = findViewById(R.id.btn_week)
        btnMonth2 = findViewById(R.id.btn2_month)
        btnYear = findViewById(R.id.btn_year)

        // Setup RecyclerView
        transactionAdapter = TransactionAdapter(transactions)
        rvRecentTransactions.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = transactionAdapter
        }

        // Setup Bottom Navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        bottomNavigationView.selectedItemId = R.id.nav_home
    }

    private fun setupClickListeners() {
        btnMonth.setOnClickListener {
            showMonthSelectorDialog()
        }

        ivProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<View>(R.id.fab_add).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        findViewById<View>(R.id.tv_see_all).setOnClickListener {
            startActivity(Intent(this, TransactionsActivity::class.java))
        }

        btnToday.setOnClickListener { updateChartWithPeriod("Today") }
        btnWeek.setOnClickListener { updateChartWithPeriod("Week") }
        btnMonth2.setOnClickListener { updateChartWithPeriod("Month") }
        btnYear.setOnClickListener { updateChartWithPeriod("Year") }

        // Bottom Navigation listener
        findViewById<BottomNavigationView>(R.id.bottom_navigation).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true // Already here
                R.id.nav_transactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    true
                }
                R.id.nav_budget -> {
                    startActivity(Intent(this, FinancialReportActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadData() {
        if (isLoading) return // Prevent multiple simultaneous loads
        
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        loadingHelper.showLoading("Loading transactions...")

        lifecycleScope.launch {
            try {
                val result = repository.userTransactions(userId)
                if (result.isSuccess) {
                    val allTransactions = result.getOrNull() ?: emptyList()

                    // Filter transactions for the selected month
                    val calendar = Calendar.getInstance()
                    val filteredTransactions = allTransactions.filter { transaction ->
                        calendar.time = transaction.date.toDate()
                        calendar.get(Calendar.MONTH) == selectedMonthIndex
                    }

                    // Update the local list and notify the adapter
                    transactions.clear()
                    transactions.addAll(filteredTransactions.sortedByDescending { it.date }) // Show most recent first
                    transactionAdapter.notifyDataSetChanged()

                    // Update summary UI
                    updateSummary()
                    updateChartWithPeriod("Today") // Or your default selection
                } else {
                    val error = result.exceptionOrNull()
                    val errorMessage = when {
                        error?.message?.contains("timeout", ignoreCase = true) == true -> 
                            "Connection timeout. Please check your internet connection."
                        error?.message?.contains("network", ignoreCase = true) == true -> 
                            "Network error. Please check your internet connection."
                        else -> "Failed to load transactions. Please try again."
                    }
                    Toast.makeText(this@HomeActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("timeout", ignoreCase = true) == true -> 
                        "Request timed out. Please check your connection and try again."
                    e.message?.contains("network", ignoreCase = true) == true -> 
                        "Network error. Please check your internet connection."
                    else -> "Error loading data: ${e.message ?: "Unknown error"}"
                }
                Toast.makeText(this@HomeActivity, errorMessage, Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
                loadingHelper.hideLoading()
            }
        }
    }

    private fun updateSummary() {
        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpenses = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val balance = totalIncome - totalExpenses

        tvBalance.text = CurrencyFormatter.formatAmountWithCode(this, balance.toLong())
        tvIncome.text = CurrencyFormatter.formatAmountWithCode(this, totalIncome.toLong())
        tvExpenses.text = CurrencyFormatter.formatAmountWithCode(this, totalExpenses.toLong())
    }

    private fun setupChart() {
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.setTouchEnabled(false)
        lineChart.setDrawGridBackground(false)

        val xAxis = lineChart.xAxis
        xAxis.setDrawAxisLine(false)
        xAxis.setDrawGridLines(false)
        xAxis.setDrawLabels(false)
        xAxis.spaceMin = 0.4f
        xAxis.spaceMax = 0.4f

        lineChart.axisLeft.isEnabled = false
        lineChart.axisRight.isEnabled = false

        lineChart.setNoDataText("No expense data for this period.")
        lineChart.setNoDataTextColor(ContextCompat.getColor(this, R.color.primary_green))
    }


    private fun updateChartWithPeriod(period: String) {
        // Reset button styles
        val unselectedBg = ContextCompat.getColorStateList(this, R.color.light_gray)
        val unselectedText = ContextCompat.getColor(this, R.color.gray)
        btnToday.backgroundTintList = unselectedBg
        btnToday.setTextColor(unselectedText)
        btnWeek.backgroundTintList = unselectedBg
        btnWeek.setTextColor(unselectedText)
        btnMonth2.backgroundTintList = unselectedBg
        btnMonth2.setTextColor(unselectedText)
        btnYear.backgroundTintList = unselectedBg
        btnYear.setTextColor(unselectedText)

        val selectedBg = ContextCompat.getColorStateList(this, R.color.primary_green)
        val selectedText = Color.WHITE

        // Set selected button style
        when (period) {
            "Today" -> {
                btnToday.backgroundTintList = selectedBg
                btnToday.setTextColor(selectedText)
            }
            "Week" -> {
                btnWeek.backgroundTintList = selectedBg
                btnWeek.setTextColor(selectedText)
            }
            "Month" -> {
                btnMonth2.backgroundTintList = selectedBg
                btnMonth2.setTextColor(selectedText)
            }
            "Year" -> {
                btnYear.backgroundTintList = selectedBg
                btnYear.setTextColor(selectedText)
            }
        }

        val calendar = Calendar.getInstance()
        val now = calendar.time

        val filtered = when (period) {
            "Today" -> transactions.filter { it.date.toDate().isSameDay(now) }
            "Week" -> transactions.filter { it.date.toDate().isSameWeek(now) }
            "Month" -> transactions // Already filtered by month
            "Year" -> transactions.filter { it.date.toDate().isSameYear(now) }
            else -> transactions
        }

        updateChart(filtered, period)
    }


    private fun updateChart(transactions: List<Transaction>, period: String) {
        if (transactions.isEmpty()) {
            lineChart.clear()
            lineChart.invalidate()
            return
        }

        val entries = ArrayList<Entry>()

        when (period) {
            "Today" -> {
                val hourlyExpenses = transactions.filter { it.type == TransactionType.EXPENSE }
                    .groupBy { Calendar.getInstance().apply { time = it.date.toDate() }.get(Calendar.HOUR_OF_DAY) }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }

                for (hour in 0..23) {
                    entries.add(Entry(hour.toFloat(), (hourlyExpenses[hour] ?: 0.0).toFloat()))
                }
            }
            "Week" -> {
                val weeklyExpenses = transactions.filter { it.type == TransactionType.EXPENSE }
                    .groupBy { Calendar.getInstance().apply { time = it.date.toDate() }.get(Calendar.DAY_OF_WEEK) }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }

                for (i in 1..7) {
                    entries.add(Entry(i.toFloat() - 1, (weeklyExpenses[i] ?: 0.0).toFloat()))
                }
            }
            "Month" -> {
                val dailyExpenses = transactions.filter { it.type == TransactionType.EXPENSE }
                    .groupBy { Calendar.getInstance().apply { time = it.date.toDate() }.get(Calendar.DAY_OF_MONTH) }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }

                val daysInMonth = Calendar.getInstance().apply { set(Calendar.MONTH, selectedMonthIndex) }.getActualMaximum(Calendar.DAY_OF_MONTH)
                for (day in 1..daysInMonth) {
                    entries.add(Entry(day.toFloat() - 1, (dailyExpenses[day] ?: 0.0).toFloat()))
                }
            }
            "Year" -> {
                val monthlyExpenses = transactions.filter { it.type == TransactionType.EXPENSE }
                    .groupBy { Calendar.getInstance().apply { time = it.date.toDate() }.get(Calendar.MONTH) }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }

                for (i in 0..11) {
                    entries.add(Entry(i.toFloat(), (monthlyExpenses[i] ?: 0.0).toFloat()))
                }
            }
        }

        val dataSet = LineDataSet(entries, "Expenses")
        dataSet.color = ContextCompat.getColor(this, R.color.primary_green)
        dataSet.lineWidth = 3f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.setDrawFilled(true)
        dataSet.fillDrawable = ContextCompat.getDrawable(this, R.drawable.chart_gradient)

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate()
    }

    private fun loadProfilePicture() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null && user.photoUrl != null) {
            Glide.with(this)
                .load(user.photoUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(ivProfile)
        } else {
            ivProfile.setImageResource(R.drawable.ic_profile_placeholder)
        }
    }

    private fun showMonthSelectorDialog() {
        AlertDialog.Builder(this)
            .setTitle("Select a Month")
            .setSingleChoiceItems(months, selectedMonthIndex) { dialog, which ->
                selectedMonthIndex = which
                updateMonthButtonText()
                loadData() // Refresh data for the newly selected month
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun updateMonthButtonText() {
        btnMonth.text = months[selectedMonthIndex]
    }

    override fun onResume() {
        super.onResume()
        // Refresh data every time the user returns to this screen
        loadData()
        loadProfilePicture()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        loadingHelper.hideLoading()
    }
}

// Helper extensions for Date
fun java.util.Date.isSameDay(other: java.util.Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = this@isSameDay }
    val cal2 = Calendar.getInstance().apply { time = other }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun java.util.Date.isSameWeek(other: java.util.Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = this@isSameWeek }
    val cal2 = Calendar.getInstance().apply { time = other }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)
}

fun java.util.Date.isSameYear(other: java.util.Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = this@isSameYear }
    val cal2 = Calendar.getInstance().apply { time = other }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
}