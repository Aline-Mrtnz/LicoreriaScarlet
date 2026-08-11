package com.example.scarlet

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Temp : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        /*Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(
                this,
                LoginActivity::class.java
            )

            startActivity(intent)
            finish()
        }, 2000)*/

        CoroutineScope(Dispatchers.Main).launch {
            delay(3000)

            val intent = Intent(
                this@Temp,
                Login::class.java
            )

            startActivity(intent)
            finish()
        }
    }
}