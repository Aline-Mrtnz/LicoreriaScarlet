package com.example.scarlet

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.adapter.CajerosAdapter
import com.example.scarlet.data.repository.CajeroInfo
import com.example.scarlet.data.repository.CuentaRepository
import com.example.scarlet.util.Session

class GestionCajeros : AppCompatActivity() {

    private lateinit var cuentaRepository: CuentaRepository
    private lateinit var adapter: CajerosAdapter
    private lateinit var txtVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_cajeros)

        if (!Session.esAdmin) {
            Toast.makeText(this, "Acceso solo para administradores", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cuentaRepository = CuentaRepository(this)
        txtVacio = findViewById(R.id.txtVacioCajeros)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnNuevoCajero).setOnClickListener {
            startActivity(Intent(this, FormularioCajero::class.java))
        }

        val recycler = findViewById<RecyclerView>(R.id.recyclerCajeros)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = CajerosAdapter(
            emptyList(),
            onEditar = { cajero -> abrirEdicion(cajero) },
            onCambiarEstado = { cajero -> confirmarCambioEstado(cajero) }
        )
        recycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        cargarCajeros()
    }

    private fun cargarCajeros() {
        val cajeros = cuentaRepository.listarCajeros()
        adapter.actualizar(cajeros)
        txtVacio.visibility = if (cajeros.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun abrirEdicion(cajero: CajeroInfo) {
        val intent = Intent(this, FormularioCajero::class.java)
        intent.putExtra(FormularioCajero.EXTRA_ID_CUENTA, cajero.idCuenta)
        intent.putExtra(FormularioCajero.EXTRA_ID_PERSONA, cajero.idPersona)
        intent.putExtra(FormularioCajero.EXTRA_NOMBRES, cajero.nombres)
        intent.putExtra(FormularioCajero.EXTRA_APELLIDOS, cajero.apellidos)
        intent.putExtra(FormularioCajero.EXTRA_CI, cajero.ci)
        intent.putExtra(FormularioCajero.EXTRA_TELEFONO, cajero.telefono)
        intent.putExtra(FormularioCajero.EXTRA_USUARIO, cajero.usuario)
        startActivity(intent)
    }

    private fun confirmarCambioEstado(cajero: CajeroInfo) {
        val activar = cajero.estado != "ACTIVO"
        val mensaje = if (activar) "¿Reactivar la cuenta de ${cajero.nombres}?"
        else "${cajero.nombres} no podrá iniciar sesión hasta que reactives su cuenta. ¿Continuar?"

        AlertDialog.Builder(this)
            .setTitle(if (activar) "Activar cuenta" else "Desactivar cuenta")
            .setMessage(mensaje)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton(if (activar) "Activar" else "Desactivar") { _, _ ->
                if (cuentaRepository.cambiarEstadoCajero(cajero.idCuenta, activar)) {
                    cargarCajeros()
                } else {
                    Toast.makeText(this, "No se pudo actualizar la cuenta", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }
}