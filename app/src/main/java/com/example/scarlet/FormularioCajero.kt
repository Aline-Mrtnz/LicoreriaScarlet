package com.example.scarlet

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scarlet.data.repository.CuentaRepository
import com.example.scarlet.data.repository.ResultadoCajero
import com.example.scarlet.util.Session

class FormularioCajero : AppCompatActivity() {

    private lateinit var cuentaRepository: CuentaRepository
    private var idCuenta: Int = -1
    private var idPersona: Int = -1
    private var esEdicion = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formulario_cajero)

        if (!Session.esAdmin) {
            Toast.makeText(this, "Acceso solo para administradores", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cuentaRepository = CuentaRepository(this)
        idCuenta = intent.getIntExtra(EXTRA_ID_CUENTA, -1)
        idPersona = intent.getIntExtra(EXTRA_ID_PERSONA, -1)
        esEdicion = idCuenta > 0

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.txtTituloFormulario).text = if (esEdicion) "Editar Cajero" else "Nuevo Cajero"

        val edtNombres = findViewById<EditText>(R.id.edtNombresCajero)
        val edtApellidos = findViewById<EditText>(R.id.edtApellidosCajero)
        val edtCi = findViewById<EditText>(R.id.edtCiCajero)
        val edtTelefono = findViewById<EditText>(R.id.edtTelefonoCajero)
        val edtUsuario = findViewById<EditText>(R.id.edtUsuarioCajero)
        val edtPin = findViewById<EditText>(R.id.edtPinCajero)
        val txtAyudaPin = findViewById<TextView>(R.id.txtAyudaPin)

        if (esEdicion) {
            edtNombres.setText(intent.getStringExtra(EXTRA_NOMBRES))
            edtApellidos.setText(intent.getStringExtra(EXTRA_APELLIDOS))
            edtCi.setText(intent.getStringExtra(EXTRA_CI))
            edtCi.isEnabled = false
            edtTelefono.setText(intent.getStringExtra(EXTRA_TELEFONO))
            edtUsuario.setText(intent.getStringExtra(EXTRA_USUARIO))
            txtAyudaPin.text = "Déjalo vacío para conservar el PIN actual"
        }

        findViewById<TextView>(R.id.btnGuardarCajero).setOnClickListener {
            guardar(
                edtNombres.text.toString().trim(),
                edtApellidos.text.toString().trim(),
                edtCi.text.toString().trim(),
                edtTelefono.text.toString().trim(),
                edtUsuario.text.toString().trim(),
                edtPin.text.toString().trim()
            )
        }
    }

    private fun guardar(nombres: String, apellidos: String, ci: String, telefono: String, usuario: String, pin: String) {
        if (nombres.isEmpty() || apellidos.isEmpty() || usuario.isEmpty()) {
            Toast.makeText(this, "Completa nombres, apellidos y usuario", Toast.LENGTH_SHORT).show()
            return
        }
        if ((!esEdicion && pin.length != 4) || (pin.isNotEmpty() && pin.length != 4)) {
            Toast.makeText(this, "El PIN debe tener 4 dígitos", Toast.LENGTH_SHORT).show()
            return
        }

        val resultado = if (esEdicion) {
            cuentaRepository.editarCajero(idCuenta, idPersona, nombres, apellidos, telefono, usuario, pin)
        } else {
            cuentaRepository.crearCajero(nombres, apellidos, ci, telefono, usuario, pin)
        }

        when (resultado) {
            is ResultadoCajero.Exito -> {
                Toast.makeText(this, "Cajero guardado correctamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            is ResultadoCajero.UsuarioDuplicado -> Toast.makeText(this, "Ese nombre de usuario ya existe", Toast.LENGTH_SHORT).show()
            is ResultadoCajero.CiDuplicado -> Toast.makeText(this, "Ya existe una persona con ese CI", Toast.LENGTH_SHORT).show()
            is ResultadoCajero.Error -> Toast.makeText(this, "Error: ${resultado.mensaje}", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val EXTRA_ID_CUENTA = "extra_id_cuenta"
        const val EXTRA_ID_PERSONA = "extra_id_persona"
        const val EXTRA_NOMBRES = "extra_nombres"
        const val EXTRA_APELLIDOS = "extra_apellidos"
        const val EXTRA_CI = "extra_ci"
        const val EXTRA_TELEFONO = "extra_telefono"
        const val EXTRA_USUARIO = "extra_usuario"
    }
}