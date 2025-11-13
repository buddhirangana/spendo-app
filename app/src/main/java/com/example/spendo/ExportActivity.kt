package com.example.spendo

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.spendo.data.Repository
import com.example.spendo.data.Transaction
import com.example.spendo.data.TransactionType
import com.example.spendo.utils.CurrencyFormatter
import com.example.spendo.utils.LoadingHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExportActivity : AppCompatActivity() {

    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private lateinit var rbPdf: RadioButton
    private lateinit var rbExcel: RadioButton
    private lateinit var repository: Repository
    private lateinit var loadingHelper: LoadingHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export)

        repository = Repository()
        loadingHelper = LoadingHelper(this)

        etStartDate = findViewById(R.id.et_start_date)
        etEndDate = findViewById(R.id.et_end_date)
        rbPdf = findViewById(R.id.rb_pdf)
        rbExcel = findViewById(R.id.rb_excel)

        etStartDate.setOnClickListener {
            showDatePickerDialog(etStartDate)
        }

        etEndDate.setOnClickListener {
            showDatePickerDialog(etEndDate)
        }

        findViewById<MaterialButton>(R.id.btn_export).setOnClickListener {
            exportData()
        }

        findViewById<View>(R.id.iv_back).setOnClickListener { finish() }
    }

    private fun showDatePickerDialog(editText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                editText.setText(format.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun exportData() {
        val startDateText = etStartDate.text?.toString()?.trim().orEmpty()
        val endDateText = etEndDate.text?.toString()?.trim().orEmpty()
        val exportFormat = when {
            rbPdf.isChecked -> ExportFormat.PDF
            rbExcel.isChecked -> ExportFormat.CSV
            else -> null
        }

        if (startDateText.isEmpty() || endDateText.isEmpty() || exportFormat == null) {
            Toast.makeText(this, "Please select start date, end date, and format", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = parseDate(startDateText, dateFormat)
        val endDate = parseDate(endDateText, dateFormat)

        if (startDate == null || endDate == null) {
            Toast.makeText(this, "Invalid date format. Please use YYYY-MM-DD.", Toast.LENGTH_SHORT).show()
            return
        }

        if (startDate.after(endDate)) {
            Toast.makeText(this, "Start date cannot be after end date.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "You need to be signed in to export data.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            loadingHelper.showLoading("Preparing export...")
            try {
                val result = repository.userTransactions(userId)
                if (result.isFailure) {
                    val message = result.exceptionOrNull()?.message ?: "Unable to load transactions."
                    Toast.makeText(this@ExportActivity, message, Toast.LENGTH_LONG).show()
                    return@launch
                }

                val exportRange = createDateRange(startDate, endDate)
                val transactions = result.getOrNull()
                    .orEmpty()
                    .filter { transaction ->
                        val transactionDate = transaction.date.toDate().time
                        transactionDate in exportRange.first..exportRange.second
                    }
                    .sortedBy { it.date.toDate().time }

                if (transactions.isEmpty()) {
                    Toast.makeText(this@ExportActivity, "No transactions found for the selected range.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val exportFile = when (exportFormat) {
                    ExportFormat.PDF -> createPdfFile(transactions, startDate, endDate)
                    ExportFormat.CSV -> createCsvFile(transactions, startDate, endDate)
                }

                shareExportedFile(exportFile, exportFormat.mimeType)
                Toast.makeText(
                    this@ExportActivity,
                    "Exported ${transactions.size} transactions to ${exportFile.name}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@ExportActivity,
                    "Failed to export data: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                loadingHelper.hideLoading()
            }
        }
    }

    private fun parseDate(value: String, dateFormat: SimpleDateFormat): Date? {
        return try {
            dateFormat.parse(value)
        } catch (_: ParseException) {
            null
        }
    }

    private fun createDateRange(startDate: Date, endDate: Date): Pair<Long, Long> {
        val startCalendar = Calendar.getInstance().apply {
            time = startDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCalendar = Calendar.getInstance().apply {
            time = endDate
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return startCalendar.timeInMillis to endCalendar.timeInMillis
    }

    private fun createPdfFile(
        transactions: List<Transaction>,
        startDate: Date,
        endDate: Date
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 size in points (approx 8.3in at 72dpi)
        val pageHeight = 842 // A4 height
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var yPosition = 60f

        val primaryColor = ContextCompat.getColor(this, R.color.primary_green)
        val surfaceColor = ContextCompat.getColor(this, R.color.white)
        val mutedTextColor = ContextCompat.getColor(this, R.color.primary_gray)
        val tableHeaderBg = ContextCompat.getColor(this, R.color.light_gray_3)
        val rowAltBg = ContextCompat.getColor(this, R.color.light_gray)
        val dividerColor = ContextCompat.getColor(this, R.color.light_gray_2)

        val titlePaint = Paint().apply {
            color = ContextCompat.getColor(this@ExportActivity, R.color.white)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 20f
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = ContextCompat.getColor(this@ExportActivity, R.color.white)
            textSize = 11f
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = mutedTextColor
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 11f
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = ContextCompat.getColor(this@ExportActivity, R.color.black)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 10f
            isAntiAlias = true
        }

        val amountPaint = Paint(textPaint).apply {
            textAlign = Paint.Align.RIGHT
        }

        val descriptionPaint = Paint(textPaint).apply {
            color = mutedTextColor
            textSize = 9f
        }

        val dividerPaint = Paint().apply {
            color = dividerColor
            strokeWidth = 1.2f
            isAntiAlias = true
        }

        val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val headerHeight = 80f
        val headerRectLeft = 30f
        val headerRectRight = pageWidth - 30f
        val headerRectTop = 30f
        val headerRectBottom = headerRectTop + headerHeight

        val headerPaintRect = Paint().apply {
            color = primaryColor
            isAntiAlias = true
        }

        canvas.drawRoundRect(
            headerRectLeft,
            headerRectTop,
            headerRectRight,
            headerRectBottom,
            18f,
            18f,
            headerPaintRect
        )

        val headerTitleX = headerRectLeft + 24f
        var headerTextY = headerRectTop + 32f
        canvas.drawText("Spendo Transactions Report", headerTitleX, headerTextY, titlePaint)
        headerTextY += 20f

        val rangeText = "Date range  ${fileDateFormat.format(startDate)} – ${fileDateFormat.format(endDate)}"
        canvas.drawText(rangeText, headerTitleX, headerTextY, subtitlePaint)
        headerTextY += 16f

        val totals = calculateTotals(transactions)
        val summaryText = "Income ${totals.income}   ·   Expense ${totals.expense}   ·   Net ${totals.net}"
        canvas.drawText(summaryText, headerTitleX, headerTextY, subtitlePaint)

        val startX = 36f
        val dateX = startX
        val typeX = dateX + 150f
        val categoryX = typeX + 90f
        val amountX = pageWidth - 46f

        val tableHeaderPaint = Paint().apply {
            color = tableHeaderBg
            isAntiAlias = true
        }

        fun drawTableHeaderRow() {
            val sectionTop = yPosition
            val sectionBottom = sectionTop + 34f
            canvas.drawRoundRect(
                startX - 6f,
                sectionTop,
                amountX + 20f,
                sectionBottom,
                12f,
                12f,
                tableHeaderPaint
            )

            val headerBaseline = sectionTop + 22f
            headerPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Date & Time", dateX, headerBaseline, headerPaint)
            canvas.drawText("Type", typeX, headerBaseline, headerPaint)
            canvas.drawText("Category", categoryX, headerBaseline, headerPaint)
            headerPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Amount", amountX, headerBaseline, headerPaint)
            headerPaint.textAlign = Paint.Align.LEFT

            val dividerY = sectionBottom - 6f
            canvas.drawLine(startX - 6f, dividerY, amountX + 20f, dividerY, dividerPaint)
            yPosition = sectionBottom + 18f
        }

        fun startNewPage() {
            pdfDocument.finishPage(page)
            pageNumber += 1
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPosition = 60f
            drawTableHeaderRow()
        }

        fun ensureSpace(requiredSpace: Float) {
            if (yPosition + requiredSpace > pageHeight - 60) {
                startNewPage()
            }
        }

        yPosition = headerRectBottom + 24f
        drawTableHeaderRow()

        transactions.forEachIndexed { index, transaction ->
            val dateText = displayDateTimeFormat.format(transaction.date.toDate())
            val typeText = transaction.type.name
            val categoryText = transaction.category
            val amountText = CurrencyFormatter.formatAmount(this, transaction.amount)

            val wrappedLines = if (transaction.description.isNotBlank()) {
                wrapText(
                    transaction.description,
                    descriptionPaint,
                    (pageWidth - (startX * 2)).toInt()
                )
            } else {
                emptyList()
            }

            val descriptionLines = wrappedLines.size
            val rowTextHeight = 18f + (descriptionLines * 12f)
            val rowHeight = rowTextHeight + 10f
            ensureSpace(rowHeight + 20f)

            val rowTop = yPosition - 14f
            val rowBottom = rowTop + rowHeight
            val backgroundPaint = Paint().apply {
                color = if ((index % 2) == 0) surfaceColor else rowAltBg
                isAntiAlias = true
            }

            canvas.drawRoundRect(
                startX - 6f,
                rowTop,
                amountX + 20f,
                rowBottom,
                12f,
                12f,
                backgroundPaint
            )

            canvas.drawText(dateText, dateX, yPosition, textPaint)
            canvas.drawText(typeText, typeX, yPosition, textPaint)
            canvas.drawText(categoryText, categoryX, yPosition, textPaint)
            canvas.drawText(amountText, amountX, yPosition, amountPaint)
            var contentY = yPosition + 16f

            wrappedLines.forEach { line ->
                canvas.drawText(line, dateX, contentY, descriptionPaint)
                contentY += 12f
            }

            yPosition = rowBottom + 8f
            canvas.drawLine(startX - 6f, yPosition, amountX + 20f, yPosition, dividerPaint)
            yPosition += 12f
        }

        pdfDocument.finishPage(page)

        val exportDir = getExportDirectory()
        val fileName = "Spendo_${fileDateFormat.format(startDate)}_${fileDateFormat.format(endDate)}.pdf"
        val file = File(exportDir, fileName)
        FileOutputStream(file).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }
        pdfDocument.close()
        return file
    }

    private fun createCsvFile(
        transactions: List<Transaction>,
        startDate: Date,
        endDate: Date
    ): File {
        val exportDir = getExportDirectory()
        val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fileName = "Spendo_${fileDateFormat.format(startDate)}_${fileDateFormat.format(endDate)}.csv"
        val file = File(exportDir, fileName)
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8).use { writer ->
            writer.appendLine("Date & Time,Type,Category,Description,Amount")
            transactions.forEach { transaction ->
                val columns = listOf(
                    dateTimeFormat.format(transaction.date.toDate()),
                    transaction.type.name,
                    sanitizeCsvField(transaction.category),
                    sanitizeCsvField(transaction.description),
                    CurrencyFormatter.formatAmount(this, transaction.amount)
                )
                writer.appendLine(columns.joinToString(",") { field ->
                    if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
                        "\"${field.replace("\"", "\"\"")}\""
                    } else {
                        field
                    }
                })
            }
        }
        return file
    }

    private fun sanitizeCsvField(value: String): String {
        return value.replace("\n", " ").trim()
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        if (text.isBlank()) return emptyList()

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine = candidate
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }

    private fun getExportDirectory(): File {
        val exportDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        return exportDir
    }

    private fun shareExportedFile(file: File, mimeType: String) {
        val authority = "${applicationContext.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(this, authority, file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share exported file"))
    }

    private fun calculateTotals(transactions: List<Transaction>): ExportSummary {
        var income = 0L
        var expense = 0L
        transactions.forEach { transaction ->
            when (transaction.type) {
                TransactionType.INCOME -> income += transaction.amount
                TransactionType.EXPENSE -> expense += transaction.amount
            }
        }
        val incomeFormatted = CurrencyFormatter.formatAmount(this, income)
        val expenseFormatted = CurrencyFormatter.formatAmount(this, expense)
        val netFormatted = CurrencyFormatter.formatAmount(this, income - expense)

        return ExportSummary(incomeFormatted, expenseFormatted, netFormatted)
    }

    private data class ExportSummary(
        val income: String,
        val expense: String,
        val net: String
    )

    private enum class ExportFormat(val mimeType: String) {
        PDF("application/pdf"),
        CSV("text/csv")
    }
}