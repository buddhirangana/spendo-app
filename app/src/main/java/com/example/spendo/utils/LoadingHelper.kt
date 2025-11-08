package com.example.spendo.utils

import android.app.ProgressDialog
import android.content.Context

class LoadingHelper(private val context: Context) {
    private var progressDialog: ProgressDialog? = null
    
    fun showLoading(message: String = "Loading...") {
        try {
            hideLoading() // Hide any existing dialog
            if (context is android.app.Activity && !context.isFinishing && !context.isDestroyed) {
                progressDialog = ProgressDialog(context).apply {
                    setMessage(message)
                    setCancelable(false)
                    setCanceledOnTouchOutside(false)
                    show()
                }
            }
        } catch (e: Exception) {
            // Ignore if context is invalid
        }
    }
    
    fun hideLoading() {
        try {
            progressDialog?.dismiss()
        } catch (e: Exception) {
            // Ignore if dialog is already dismissed
        } finally {
            progressDialog = null
        }
    }
    
    fun updateMessage(message: String) {
        try {
            progressDialog?.setMessage(message)
        } catch (e: Exception) {
            // Ignore if dialog is invalid
        }
    }
}

