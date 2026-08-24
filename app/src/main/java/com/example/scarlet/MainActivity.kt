package com.example.scarlet

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        val header =
            findViewById<android.view.View>(R.id.headerLayout)

        val bottomNavigation =
            findViewById<BottomNavigationView>(R.id.bottomNavigation)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { _, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            // ===============================
            // HEADER
            // ===============================

            val headerParams =
                header.layoutParams as ViewGroup.MarginLayoutParams

            headerParams.height =
                (82 * resources.displayMetrics.density).toInt() +
                        systemBars.top

            header.layoutParams = headerParams

            header.setPadding(
                header.paddingLeft,
                systemBars.top,
                header.paddingRight,
                header.paddingBottom
            )


            // ===============================
            // MENU INFERIOR
            // ===============================

            val bottomParams =
                bottomNavigation.layoutParams
                        as ViewGroup.MarginLayoutParams

            bottomParams.height =
                (80 * resources.displayMetrics.density).toInt() +
                        systemBars.bottom

            bottomNavigation.layoutParams = bottomParams

            bottomNavigation.setPadding(
                bottomNavigation.paddingLeft,
                bottomNavigation.paddingTop,
                bottomNavigation.paddingRight,
                systemBars.bottom
            )

            insets
        }


        // ============================================
        // BOTTOM NAVIGATION
        // ============================================

        bottomNavigation.selectedItemId = R.id.nav_home

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_productos -> {

                    startActivity(
                        Intent(
                            this,
                            Productos::class.java
                        )
                    )

                    true
                }

                R.id.nav_ventas -> {

                    startActivity(
                        Intent(
                            this,
                            Ventas::class.java
                        )
                    )

                    true
                }

                R.id.nav_reportes -> {

                    startActivity(
                        Intent(
                            this,
                            Reportes::class.java
                        )
                    )

                    true
                }

                R.id.nav_home -> {
                    true
                }

                else -> false
            }
        }
    }
}