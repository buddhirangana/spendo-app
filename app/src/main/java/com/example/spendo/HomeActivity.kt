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
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
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

    private lateinit var ivProfile: CircleImageView
    private lateinit var rvRecentTransactions: RecyclerView
    private lateinit var tvBalance: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpenses: TextView
    private lateinit var barChart: BarChart
    private lateinit var btnToday: MaterialButton
    private lateinit var btnWeek: MaterialButton
    private lateinit var btnMonth2: MaterialButton
    private lateinit var btnYear: MaterialButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize repository
        repository = Repository()

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
        barChart = findViewById(R.id.bar_chart)
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
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

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
                    Toast.makeText(this@HomeActivity, "Failed to load transactions", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSummary() {
        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpenses = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val balance = totalIncome - totalExpenses

        tvBalance.text = "LKR ${String.format("%,d", balance.toLong())}"
        tvIncome.text = "LKR ${String.format("%,d", totalIncome.toLong())}"
        tvExpenses.text = "LKR ${String.format("%,d", totalExpenses.toLong())}"
    }

    private fun setupChart() {
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setDrawValueAboveBar(true)
        barChart.setTouchEnabled(false)
        barChart.setDrawGridBackground(false)

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textColor = Color.BLACK
        xAxis.axisLineColor = Color.BLACK

        val leftAxis = barChart.axisLeft
        leftAxis.setDrawGridLines(false)
        leftAxis.textColor = Color.BLACK
        leftAxis.axisMinimum = 0f

        barChart.axisRight.isEnabled = false
    }


    private fun updateChartWithPeriod(period: String) {
        // Reset button styles
        btnToday.backgroundTintList = ContextCompat.getColorStateList(this, R.color.light_gray)
        btnWeek.backgroundTintList = ContextCompat.getColorStateList(this, R.color.light_gray)
        btnMonth2.backgroundTintList = ContextCompat.getColorStateList(this, R.color.light_gray)
        btnYear.backgroundTintList = ContextCompat.getColorStateList(this, R.color.light_gray)

        // Set selected button style
        when (period) {
            "Today" -> btnToday.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_green)
            "Week" -> btnWeek.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_green)
            "Month" -> btnMonth2.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_green)
            "Year" -> btnYear.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_green)
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
            barChart.clear()
            barChart.invalidate()
            return
        }

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        when (period) {
            "Today" -> {
                // For today, we can show hourly expenses
                val hourlyExpenses = transactions.filter { it.type == TransactionType.EXPENSE }
                    .groupBy { Calendar.getInstance().apply { time = it.date.toDate() }.get(Calendar.HOUR_OF_DAY) }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }

                for (hour in 0..23) {
                    entries.add(BarEntry(hour.toFloat(), (hourlyExpenses[hour] ?: 0.0).toFloat()))
                    labels.add("${hour}h")
                }
            }
            "Week" -> {
                val weeklyExpenses = transactions.filter { it.type == TransactionType.EXPENSE }
                    .groupBy { Calendar.getInstance().apply { time = it.date.toDate() }.get(Calendar.DAY_OF_WEEK) }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }

                val days = DateFormatSymbols().shortWeekdays
                for (i in 1..7) {
                    entries.add(BarEntry(i.toFloat() - 1, (weeklyExpenses[i] ?: 0.0).toFloat()))
                    labels.add(days[i])
                }
            }
            "Month" -> {
                val dailyExpenses = transactions.filter { it.type == TransactionType.EXPENSE }
                    .groupBy { Calendar.getInstance().apply { time = it.date.toDate() }.get(Calendar.DAY_OF_MONTH) }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }

                val daysInMonth = Calendar.getInstance().apply { set(Calendar.MONTH, selectedMonthIndex) }.getActualMaximum(Calendar.DAY_OF_MONTH)
                for (day in 1..daysInMonth) {
                    entries.add(BarEntry(day.toFloat() - 1, (dailyExpenses[day] ?: 0.0).toFloat()))
                    labels.add("$day")
                }
            }
            "Year" -> {
                val monthlyExpenses = transactions.filter { it.type == TransactionType.EXPENSE }
                    .groupBy { Calendar.getInstance().apply { time = it.date.toDate() }.get(Calendar.MONTH) }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }

                val months = DateFormatSymbols().shortMonths
                for (i in 0..11) {
                    entries.add(BarEntry(i.toFloat(), (monthlyExpenses[i] ?: 0.0).toFloat()))
                    labels.add(months[i])
                }
            }
        }

        val dataSet = BarDataSet(entries, "Expenses")
        dataSet.color = ContextCompat.getColor(this, R.color.primary_green)
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 10f

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f

        barChart.data = barData
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.invalidate()
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