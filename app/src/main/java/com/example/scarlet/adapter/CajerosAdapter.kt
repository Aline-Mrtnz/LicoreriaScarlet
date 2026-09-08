package com.example.scarlet.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.R
import com.example.scarlet.data.repository.CajeroInfo
import com.google.android.material.switchmaterial.SwitchMaterial

class CajerosAdapter(
    private var cajeros: List<CajeroInfo>,
    private val onEditar: (CajeroInfo) -> Unit,
    private val onEstadoCambiado: (CajeroInfo, Boolean) -> Unit
) : RecyclerView.Adapter<CajerosAdapter.CajeroViewHolder>() {

    // Misma paleta rotativa que se usa en ProveedoresAdapter, para mantener
    // el mismo lenguaje visual entre pantallas.
    private val coloresAvatar = listOf("#ED2F09", "#C9A227", "#4C6EF5", "#2E9E5B", "#8D5F50")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CajeroViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cajero, parent, false)
        return CajeroViewHolder(view)
    }

    override fun onBindViewHolder(holder: CajeroViewHolder, position: Int) {
        holder.bind(cajeros[position], coloresAvatar[position % coloresAvatar.size])
    }

    override fun getItemCount(): Int = cajeros.size

    fun actualizar(nuevaLista: List<CajeroInfo>) {
        cajeros = nuevaLista
        notifyDataSetChanged()
    }

    inner class CajeroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvIniciales: TextView = itemView.findViewById(R.id.tvInicialesCajero)
        private val txtNombre: TextView = itemView.findViewById(R.id.txtNombreCajero)
        private val txtUsuario: TextView = itemView.findViewById(R.id.txtUsuarioCajero)
        private val txtEstado: TextView = itemView.findViewById(R.id.txtEstadoCajero)
        private val txtContacto: TextView = itemView.findViewById(R.id.txtContactoCajero)
        private val switchEstado: SwitchMaterial = itemView.findViewById(R.id.switchEstadoCajero)
        private val btnEditar: LinearLayout = itemView.findViewById(R.id.btnEditarCajero)

        fun bind(cajero: CajeroInfo, colorAvatar: String) {
            tvIniciales.text = cajero.iniciales
            tvIniciales.background.setTint(Color.parseColor(colorAvatar))

            txtNombre.text = "${cajero.nombres} ${cajero.apellidos}"

            val ci = if (cajero.ci.isNotBlank()) " • CI: ${cajero.ci}" else ""
            txtUsuario.text = "@${cajero.usuario}$ci"

            txtContacto.text = if (cajero.telefono.isNotBlank()) "Cel: ${cajero.telefono}" else "Sin celular registrado"

            if (cajero.esActivo) {
                txtEstado.text = "● Activo"
                txtEstado.setTextColor(Color.parseColor("#4CD964"))
                txtEstado.setBackgroundResource(R.drawable.bg_badge_activo)
            } else {
                txtEstado.text = "● Inactivo"
                txtEstado.setTextColor(Color.parseColor("#999999"))
                txtEstado.setBackgroundResource(R.drawable.bg_badge_inactivo)
            }

            // Se limpia el listener antes de setChecked para que no se dispare
            // onEstadoCambiado al reciclar la vista.
            switchEstado.setOnCheckedChangeListener(null)
            switchEstado.isChecked = cajero.esActivo
            switchEstado.text = if (cajero.esActivo) "Activo" else "Inactivo"
            switchEstado.setOnCheckedChangeListener { _, isChecked ->
                switchEstado.text = if (isChecked) "Activo" else "Inactivo"
                onEstadoCambiado(cajero, isChecked)
            }

            btnEditar.setOnClickListener { onEditar(cajero) }
        }
    }
}