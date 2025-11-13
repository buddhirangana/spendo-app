package com.example.spendo

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AppInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_info)

        val ivBackButton: ImageView = findViewById(R.id.iv_back)
        val tvAppName: TextView = findViewById(R.id.tv_app_name)
        val tvAppVersion: TextView = findViewById(R.id.tv_app_version)

        ivBackButton.setOnClickListener {
            onBackPressed()
        }

        // Set app name
        tvAppName.text = getString(R.string.app_name)

        // Set app version
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            tvAppVersion.text = "Version $versionName"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            tvAppVersion.text = "Version N/A"
        }
    }
}