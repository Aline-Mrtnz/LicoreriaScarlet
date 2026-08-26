package com.example.scarlet

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class Reportes : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_reports)

        // ===============================
        // HEADER
        // ===============================

        val header =
            findViewById<android.view.View>(R.id.headerLayout)

        // ===============================
        // BOTTOM NAVIGATION
        // ===============================

        val bottomNavigation =
            findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // ===============================
        // AJUSTAR BARRAS DEL SISTEMA
        // ===============================

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

        setupFilters()
        setupBottomNavigation()

        // ============================================
        // NOTIFICACIONES
        // ============================================

        setupNotifications()
    }
    // ============================================
    // MOSTRAR NUMERO DE NOTIFICACIONES
    // ============================================

    private fun setupNotifications() {

        val notificationIcon =
            findViewById<ImageView>(
                R.id.imgNotificacion
            )

        val badge =
            findViewById<TextView>(
                R.id.txtNotificationBadge
            )

        val notificationCount = 3

        // ============================================
        // MOSTRAR NUMERO DE NOTIFICACIONES
        // ============================================

        if (notificationCount > 0) {

            badge.text =
                if (notificationCount > 99) {
                    "99+"
                } else {
                    notificationCount.toString()
                }

            badge.visibility =
                android.view.View.VISIBLE

        } else {

            badge.visibility =
                android.view.View.GONE
        }

        // ============================================
        // TOCAR LA CAMPANA
        // ============================================

        notificationIcon.setOnClickListener {

            Toast.makeText(
                this,
                "Tienes $notificationCount nuevas notificaciones",
                Toast.LENGTH_SHORT
            ).show()

            // Ocultar el número después de verlas
            badge.visibility =
                android.view.View.GONE
        }
    }

    // ============================================
    // FILTROS
    // ============================================

    private fun setupFilters() {

        val filters = listOf(
            R.id.filterDay to "Día",
            R.id.filterWeek to "Semana",
            R.id.filterMonth to "Mes",
            R.id.filterYear to "Año"
        )

        var selectedView: TextView? =
            findViewById(R.id.filterDay)

        filters.forEach { (id, name) ->

            val view =
                findViewById<TextView>(id)

            view.setOnClickListener {

                // Resetear estilo anterior
                selectedView?.apply {

                    setBackgroundResource(
                        R.drawable.bg_filter_unselected
                    )

                    setTextColor(
                        resources.getColor(
                            android.R.color.darker_gray
                        )
                    )
                }

                // Nuevo seleccionado
                view.apply {

                    setBackgroundResource(
                        R.drawable.bg_filter_selected
                    )

                    setTextColor(
                        resources.getColor(
                            android.R.color.white
                        )
                    )
                }

                selectedView = view

                updateDataForFilter(name)
            }
        }
    }

    // ============================================
    // ACTUALIZAR DATOS
    // ============================================

    private fun updateDataForFilter(filter: String) {

        when (filter) {

            "Día" -> {

                findViewById<TextView>(
                    R.id.txtTotalVentas
                ).text = "$3,450"

                findViewById<TextView>(
                    R.id.txtGanancia
                ).text = "$1,120"
            }

            "Semana" -> {

                findViewById<TextView>(
                    R.id.txtTotalVentas
                ).text = "$24,850"

                findViewById<TextView>(
                    R.id.txtGanancia
                ).text = "$8,210"
            }

            "Mes" -> {

                findViewById<TextView>(
                    R.id.txtTotalVentas
                ).text = "$98,400"

                findViewById<TextView>(
                    R.id.txtGanancia
                ).text = "$32,500"
            }

            "Año" -> {

                findViewById<TextView>(
                    R.id.txtTotalVentas
                ).text = "$1,245,000"

                findViewById<TextView>(
                    R.id.txtGanancia
                ).text = "$412,000"
            }
        }
    }

    // ============================================
    // BOTTOM NAVIGATION
    // ============================================

    private fun setupBottomNavigation() {

        val bottomNav =
            findViewById<BottomNavigationView>(
                R.id.bottomNavigation
            )

        bottomNav.selectedItemId =
            R.id.nav_reportes

        bottomNav.setOnItemSelectedListener { menuItem ->

            when (menuItem.itemId) {

                R.id.nav_home -> {

                    startActivity(
                        Intent(
                            this,
                            MainActivity::class.java
                        )
                    )

                    finish()

                    true
                }

                R.id.nav_productos -> {

                    startActivity(
                        Intent(
                            this,
                            Productos::class.java
                        )
                    )

                    finish()

                    true
                }

                R.id.nav_ventas -> {

                    startActivity(
                        Intent(
                            this,
                            Ventas::class.java
                        )
                    )

                    finish()

                    true
                }

                R.id.nav_reportes -> {
                    true
                }

                else -> false
            }
        }
    }
}