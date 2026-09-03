package com.example.scarlet

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.scarlet.data.repository.CuentaRepository
import com.example.scarlet.util.Session

class MiCuenta : AppCompatActivity() {

    private lateinit var edtNombres: EditText
    private lateinit var edtApellidos: EditText
    private lateinit var edtUsuario: EditText
    private lateinit var edtCorreo: EditText
    private lateinit var edtTelefono: EditText
    private lateinit var edtContrasena: EditText

    private lateinit var txtNombreCuenta: TextView
    private lateinit var txtRolCuenta: TextView

    private lateinit var cuentaRepository: CuentaRepository

    private var idCuentaActual = -1
    private var idPersonaActual = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_mi_cuenta)

        // ============================================
        // REFERENCIAS
        // ============================================

        edtNombres = findViewById(R.id.edtNombres)
        edtApellidos = findViewById(R.id.edtApellidos)
        edtUsuario = findViewById(R.id.edtUsuario)
        edtCorreo = findViewById(R.id.edtCorreo)
        edtTelefono = findViewById(R.id.edtTelefono)
        edtContrasena = findViewById(R.id.edtContrasena)

        txtNombreCuenta =
            findViewById(R.id.txtNombreCuenta)

        txtRolCuenta =
            findViewById(R.id.txtRolCuenta)

        val btnBack =
            findViewById<ImageView>(R.id.btnBack)

        val btnGuardar =
            findViewById<Button>(R.id.btnGuardarCambios)

        cuentaRepository =
            CuentaRepository(this)

        // ============================================
        // EDGE TO EDGE
        // ============================================

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // ============================================
        // VOLVER
        // ============================================

        btnBack.setOnClickListener {
            finish()
        }

        // ============================================
        // CARGAR DATOS REALES
        // ============================================

        cargarDatos()

        // ============================================
        // GUARDAR
        // ============================================

        btnGuardar.setOnClickListener {
            guardarCambios()
        }
    }

    // ============================================
    // CARGAR DATOS DEL USUARIO LOGUEADO
    // ============================================

    private fun cargarDatos() {

        try {

            val usuarioActual =
                cuentaRepository.obtenerUsuarioActual()

            if (usuarioActual == null) {

                Toast.makeText(
                    this,
                    "No se encontró la cuenta actual",
                    Toast.LENGTH_LONG
                ).show()

                return
            }

            // ========================================
            // GUARDAR IDs REALES
            // ========================================

            idCuentaActual =
                usuarioActual.idCuenta

            idPersonaActual =
                usuarioActual.idPersona

            // ========================================
            // MOSTRAR DATOS REALES
            // ========================================

            edtNombres.setText(
                usuarioActual.nombres
            )

            edtApellidos.setText(
                usuarioActual.apellidos
            )

            edtUsuario.setText(
                usuarioActual.usuario
            )

            edtTelefono.setText(
                usuarioActual.telefono
            )

            // ========================================
            // CORREO
            // ========================================
            // Tu tabla persona actual no tiene
            // una columna correo.
            // Por eso no podemos cargarlo desde BD.

            edtCorreo.setText("")

            // ========================================
            // CONTRASEÑA
            // ========================================
            // Nunca mostramos la contraseña actual.

            edtContrasena.setText("")

            // ========================================
            // INFORMACIÓN DEL PERFIL
            // ========================================

            txtNombreCuenta.text =
                "${usuarioActual.nombres} ${usuarioActual.apellidos}"

            txtRolCuenta.text =
                "● ${usuarioActual.nombreRol}"

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Error al cargar los datos: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ============================================
    // GUARDAR CAMBIOS
    // ============================================

    private fun guardarCambios() {

        val nombres =
            edtNombres.text.toString().trim()

        val apellidos =
            edtApellidos.text.toString().trim()

        val usuario =
            edtUsuario.text.toString().trim()

        val telefono =
            edtTelefono.text.toString().trim()

        val contrasena =
            edtContrasena.text.toString().trim()

        // ============================================
        // VALIDAR SESIÓN
        // ============================================

        if (idCuentaActual <= 0 ||
            idPersonaActual <= 0
        ) {

            Toast.makeText(
                this,
                "No se pudo identificar la cuenta",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        // ============================================
        // VALIDAR NOMBRES
        // ============================================

        if (nombres.isEmpty()) {

            edtNombres.error =
                "Ingresa tus nombres"

            edtNombres.requestFocus()

            return
        }

        // ============================================
        // VALIDAR APELLIDOS
        // ============================================

        if (apellidos.isEmpty()) {

            edtApellidos.error =
                "Ingresa tus apellidos"

            edtApellidos.requestFocus()

            return
        }

        // ============================================
        // VALIDAR USUARIO
        // ============================================

        if (usuario.isEmpty()) {

            edtUsuario.error =
                "Ingresa un nombre de usuario"

            edtUsuario.requestFocus()

            return
        }

        // ============================================
        // VALIDAR TELÉFONO
        // ============================================

        if (telefono.isEmpty()) {

            edtTelefono.error =
                "Ingresa tu teléfono"

            edtTelefono.requestFocus()

            return
        }

        // ============================================
        // ACTUALIZAR BD
        // ============================================

        try {

            val resultado =
                cuentaRepository.actualizarCuenta(
                    idCuenta = idCuentaActual,
                    idPersona = idPersonaActual,
                    nombres = nombres,
                    apellidos = apellidos,
                    usuario = usuario,
                    telefono = telefono,
                    nuevaClave = contrasena
                )

            if (resultado) {

                // ====================================
                // ACTUALIZAR SESSION
                // ====================================

                Session.iniciar(
                    idCuenta = idCuentaActual,
                    usuario = usuario,
                    nombreCompleto =
                        "$nombres $apellidos",
                    rol = Session.rol
                )

                // ====================================
                // ACTUALIZAR CABECERA
                // ====================================

                txtNombreCuenta.text =
                    "$nombres $apellidos"

                Toast.makeText(
                    this,
                    "Datos actualizados correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                // No dejamos la contraseña escrita
                edtContrasena.setText("")

            } else {

                Toast.makeText(
                    this,
                    "No se pudo actualizar. Verifica el nombre de usuario.",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Error al guardar: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}