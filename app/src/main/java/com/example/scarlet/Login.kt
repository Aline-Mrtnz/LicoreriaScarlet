package com.example.scarlet

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
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
import com.example.scarlet.database.DatabaseHelper
import com.google.android.material.button.MaterialButton

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

        val pin1 = findViewById<EditText>(R.id.pin1)
        val pin2 = findViewById<EditText>(R.id.pin2)
        val pin3 = findViewById<EditText>(R.id.pin3)
        val pin4 = findViewById<EditText>(R.id.pin4)

        val btnIngresar = findViewById<MaterialButton>(R.id.buttonLogout)
        val ivTogglePassword = findViewById<ImageView>(R.id.ivTogglePassword)
        var pinVisible = false

        val pines = listOf(pin1, pin2, pin3, pin4)

        // Fijar el inputType numérico UNA sola vez (no se vuelve a tocar)
        pines.forEach { pin ->
            pin.inputType = InputType.TYPE_CLASS_NUMBER
        }

        // Función que aplica el estado (oculto/visible) usando transformationMethod
        fun aplicarVisibilidadPin(visible: Boolean) {
            pines.forEach { pin ->
                val cursorPos = pin.selectionStart
                pin.transformationMethod = if (visible) {
                    HideReturnsTransformationMethod.getInstance() // muestra el texto
                } else {
                    PasswordTransformationMethod.getInstance()    // oculta con puntos, sin parpadeo
                }
                pin.setSelection(cursorPos.coerceAtLeast(0))
            }
            ivTogglePassword.setImageResource(
                if (visible) R.drawable.ojocerrado else R.drawable.ojoavierto
            )
        }

        // Estado inicial: oculto
        aplicarVisibilidadPin(pinVisible)

        ivTogglePassword.setOnClickListener {
            pinVisible = !pinVisible
            aplicarVisibilidadPin(pinVisible)
        }

        // Configurar el salto automático entre cajas
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

            val dbHelper = DatabaseHelper(this)

            if (dbHelper.validarPin(pinCompleto)) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                pin1.text.clear()
                pin2.text.clear()
                pin3.text.clear()
                pin4.text.clear()
                pin1.requestFocus()
            }
        }
    }

    /*Configura el salto automático de foco entre cajas de PIN.*/
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