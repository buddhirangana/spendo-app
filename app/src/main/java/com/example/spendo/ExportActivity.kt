package com.example.spendo

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExportActivity : AppCompatActivity() {

    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private lateinit var rbPdf: RadioButton
    private lateinit var rbExcel: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export)

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
        val startDate = etStartDate.text.toString()
        val endDate = etEndDate.text.toString()
        val format = if (rbPdf.isChecked) "PDF" else if (rbExcel.isChecked) "Excel" else ""

        if (startDate.isNotEmpty() && endDate.isNotEmpty() && format.isNotEmpty()) {
            // Implement export logic here
            Toast.makeText(this, "Exporting data from $startDate to $endDate as $format", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Please select start date, end date, and format", Toast.LENGTH_SHORT).show()
        }
    }
}
