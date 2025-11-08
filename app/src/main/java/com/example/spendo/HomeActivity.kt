package com.example.spendo

import android.content.Context
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
import java.util.*
import com.example.spendo.adapters.formatCurrency


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
    private lateinit var lineChart: LineChart
    private lateinit var btnToday: MaterialButton
    private lateinit var btnWeek: MaterialButton
    private lateinit var btnMonth2: MaterialButton
    private lateinit var btnYear: MaterialButton

    private var currentCurrency: String = "LKR"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        repository = Repository()
        initViews()
        setupClickListeners()

        val calendar = Calendar.getInstance()
        selectedMonthIndex = calendar.get(Calendar.MONTH)
        updateMonthButtonText()
        setupChart()
    }

    override fun onResume() {
        super.onResume()
        val sharedPrefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        currentCurrency = sharedPrefs.getString("Currency", "LKR") ?: "LKR"

        loadData()
        loadProfilePicture()
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

        transactionAdapter = TransactionAdapter(transactions, currentCurrency)
        rvRecentTransactions.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = transactionAdapter
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        bottomNavigationView.selectedItemId = R.id.nav_home
    }

    private fun setupClickListeners() {
        btnMonth.setOnClickListener { showMonthSelectorDialog() }
        ivProfile.setOnClickListener { startActivity(Intent(this, EditProfileActivity::class.java)) }
        findViewById<View>(R.id.fab_add).setOnClickListener { startActivity(Intent(this, AddTransactionActivity::class.java)) }
        findViewById<View>(R.id.tv_see_all).setOnClickListener { startActivity(Intent(this, TransactionsActivity::class.java)) }
        btnToday.setOnClickListener { updateChartWithPeriod("Today") }
        btnWeek.setOnClickListener { updateChartWithPeriod("Week") }
        btnMonth2.setOnClickListener { updateChartWithPeriod("Month") }
        btnYear.setOnClickListener { updateChartWithPeriod("Year") }

        findViewById<BottomNavigationView>(R.id.bottom_navigation).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
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
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                val result = repository.userTransactions(userId)
                if (result.isSuccess) {
                    val allTransactions = result.getOrNull() ?: emptyList()
                    val calendar = Calendar.getInstance()
                    val filteredTransactions = allTransactions.filter {
                        calendar.time = it.date.toDate()
                        calendar.get(Calendar.MONTH) == selectedMonthIndex
                    }
                    transactions.clear()
                    transactions.addAll(filteredTransactions.sortedByDescending { it.date })
                    transactionAdapter.updateCurrency(currentCurrency)

                    updateSummary()
                    updateChartWithPeriod("Today")
                } else {
                    Toast.makeText(this@HomeActivity, "Failed to load transactions", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSummary() {
        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }.toDouble()
        val totalExpenses = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }.toDouble()
        val balance = totalIncome - totalExpenses

        // Use the consistent currency formatting helper
        tvBalance.text = formatCurrency(balance, currentCurrency)
        tvIncome.text = formatCurrency(totalIncome, currentCurrency)
        tvExpenses.text = formatCurrency(totalExpenses, currentCurrency)
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
        val now = Date()

        val filtered = when (period) {
            "Today" -> transactions.filter { it.date.toDate().isSameDay(now) }
            "Week" -> transactions.filter { it.date.toDate().isSameWeek(now) }
            "Month" -> transactions
            "Year" -> transactions.filter { it.date.toDate().isSameYear(now) }
            else -> transactions
        }

        updateChart(filtered)
    }

    private fun updateChart(transactions: List<Transaction>) {
        if (transactions.isEmpty()) {
            lineChart.clear()
            lineChart.invalidate()
            return
        }

        val entries = ArrayList<Entry>()
        transactions.forEach { transaction ->
            val calendar = Calendar.getInstance().apply { time = transaction.date.toDate() }
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH).toFloat()
            entries.add(Entry(dayOfMonth, transaction.amount.toFloat()))
        }

        val dataSet = LineDataSet(entries, "Expenses").apply {
            color = ContextCompat.getColor(this@HomeActivity, R.color.primary_green)
            lineWidth = 3f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(this@HomeActivity, R.drawable.chart_gradient)
        }

        lineChart.data = LineData(dataSet)
        lineChart.invalidate()
    }

    private fun loadProfilePicture() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user?.photoUrl != null) {
            Glide.with(this).load(user.photoUrl).into(ivProfile)
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
                loadData()
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun updateMonthButtonText() {
        btnMonth.text = months[selectedMonthIndex]
    }
}

fun Date.isSameDay(other: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = this@isSameDay }
    val cal2 = Calendar.getInstance().apply { time = other }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun Date.isSameWeek(other: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = this@isSameWeek }
    val cal2 = Calendar.getInstance().apply { time = other }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)
}

fun Date.isSameYear(other: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = this@isSameYear }
    val cal2 = Calendar.getInstance().apply { time = other }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
}
