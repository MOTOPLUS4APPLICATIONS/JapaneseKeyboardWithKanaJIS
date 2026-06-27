/*
    All Rights Reserved, Copyright (C) 2025, mikoto2000
      Licensed Material of mikoto2000.

    All Rights Reserved, Copyright (C) 2026, Moto+4 Applications LLC
      Licensed Material of Moto+4 Applications LLC.
 */

package dev.moto4app.J_KeyboardKanaPlus

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.util.Log;
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.TextView
import android.content.Intent
import dev.moto4app.J_KeyboardKanaPlus.ui.LicenseActivity

class MainActivity : AppCompatActivity() {
    var mydbg = "[MYDBG1]"

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(mydbg, "onCreate() in");

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.btn_open_licenses)?.setOnClickListener {
            startActivity(Intent(this, LicenseActivity::class.java))
        }

        findViewById<TextView>(R.id.txtDescription)?.text =
        "PCのキーボートレイアウトで文字入力が可能なソフトウェアキーボードです。\n" +
        "以下のモードとレイアウトがあります。\n" +
        "- 英数字キーボード + 英数字入力。\n" +
        "- 英数字キーボード + ローマ字入力。\n" +
        "- ひらがなキーボード + かな入力。\n" +
        "- カタカナキーボード + カタカナ入力。\n"
    }
}
