package com.example.scarlet

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.KeyEvent
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.scarlet.data.repository.CuentaRepository
import com.example.scarlet.database.databasehelpers
import com.example.scarlet.util.Session
import com.google.android.material.button.MaterialButton
import android.database.sqlite.SQLiteException
import java.io.File

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Verificar si la base de datos existe y se puede crear
        verificarBaseDatos()

        val pin1 = findViewById<EditText>(R.id.pin1)
        val pin2 = findViewById<EditText>(R.id.pin2)
        val pin3 = findViewById<EditText>(R.id.pin3)
        val pin4 = findViewById<EditText>(R.id.pin4)

        val btnIngresar = findViewById<MaterialButton>(R.id.buttonLogout)
        val ivTogglePassword = findViewById<ImageView>(R.id.ivTogglePassword)
        var pinVisible = false

        val pines = listOf(pin1, pin2, pin3, pin4)
        pines.forEach { pin ->
            pin.inputType = InputType.TYPE_CLASS_NUMBER
        }

        fun aplicarVisibilidadPin(visible: Boolean) {
            pines.forEach { pin ->
                val cursorPos = pin.selectionStart
                pin.transformationMethod = if (visible) {
                    HideReturnsTransformationMethod.getInstance()
                } else {
                    PasswordTransformationMethod.getInstance()
                }
                pin.setSelection(cursorPos.coerceAtLeast(0))
            }
            ivTogglePassword.setImageResource(
                if (visible) R.drawable.ojocerrado else R.drawable.ojoavierto
            )
        }

        aplicarVisibilidadPin(pinVisible)

        ivTogglePassword.setOnClickListener {
            pinVisible = !pinVisible
            aplicarVisibilidadPin(pinVisible)
        }

        setupPinAutoJump(pin1, pin2, null)
        setupPinAutoJump(pin2, pin3, pin1)
        setupPinAutoJump(pin3, pin4, pin2)
        setupPinAutoJump(pin4, null, pin3)

        btnIngresar.setOnClickListener {
            val pinCompleto = "${pin1.text}${pin2.text}${pin3.text}${pin4.text}"

            if (pinCompleto.length < 4) {
                Toast.makeText(this, "Ingresa los 4 dígitos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val dbHelper = databasehelpers(this)
                val esValido = dbHelper.validarPin(pinCompleto)

                if (esValido) {
                    // Guardar la sesión del usuario que acaba de iniciar sesión
                    // para que el resto de la app sepa quién registra cada venta.
                    val cuentaRepository = CuentaRepository(this)
                    val usuarioInfo = cuentaRepository.obtenerUsuarioPorPin(pinCompleto)
                    if (usuarioInfo != null) {
                        Session.iniciar(
                            idCuenta = usuarioInfo.idCuenta,
                            usuario = usuarioInfo.usuario,
                            nombreCompleto = "${usuarioInfo.nombres} ${usuarioInfo.apellidos}",
                            rol = usuarioInfo.nombreRol
                        )
                    }

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                    limpiarPines(pin1, pin2, pin3, pin4)
                }
            } catch (e: SQLiteException) {
                Toast.makeText(this, "Error de SQLite: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
                // Intentar recrear la base de datos
                borrarYRecrearBaseDatos()
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun verificarBaseDatos() {
        try {
            val dbHelper = databasehelpers(this)
            val db = dbHelper.readableDatabase
            db.close()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al crear BD: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun borrarYRecrearBaseDatos() {
        try {
            val dbFile = File(applicationContext.filesDir, "../databases/LicoreriaScarlet.db")
            if (dbFile.exists()) {
                dbFile.delete()
                Toast.makeText(this, "Base de datos eliminada, reinicia la app", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun limpiarPines(vararg pines: EditText) {
        pines.forEach { it.text.clear() }
        pines.firstOrNull()?.requestFocus()
    }

    private fun setupPinAutoJump(current: EditText, next: EditText?, previous: EditText?) {
        current.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 1 && next != null) {
                    next.requestFocus()
                }
            }
        })

        current.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN
                && current.text.isEmpty() && previous != null
            ) {
                previous.requestFocus()
                previous.text?.clear()
                return@setOnKeyListener true
            }
            false
        }
    }
}