// app/src/main/java/com/example/scarlet/adapter/ProveedoresAdapter.kt

package com.example.scarlet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.R
import com.example.scarlet.data.model.Proveedor
import com.google.android.material.switchmaterial.SwitchMaterial

class ProveedoresAdapter(
    private var proveedores: List<Proveedor>,
    private val onProductosClick: (Proveedor) -> Unit,
    private val onEditarClick: (Proveedor) -> Unit,
    private val onEliminarClick: (Proveedor) -> Unit,
    private val onEstadoCambiado: (Proveedor, Boolean) -> Unit
) : RecyclerView.Adapter<ProveedoresAdapter.ProveedorViewHolder>() {

    // Paleta de colores para los avatares, se asigna de forma rotativa según la posición.
    private val coloresAvatar = listOf("#ED2F09", "#C9A227", "#4C6EF5", "#2E9E5B", "#8D5F50")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProveedorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_proveedor, parent, false)
        return ProveedorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProveedorViewHolder, position: Int) {
        holder.bind(proveedores[position], coloresAvatar[position % coloresAvatar.size])
    }

    override fun getItemCount(): Int = proveedores.size

    fun actualizarLista(nuevaLista: List<Proveedor>) {
        proveedores = nuevaLista
        notifyDataSetChanged()
    }

    inner class ProveedorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvIniciales: TextView = itemView.findViewById(R.id.tvIniciales)
        private val tvRazonSocial: TextView = itemView.findViewById(R.id.tvRazonSocial)
        private val tvRfcCondicion: TextView = itemView.findViewById(R.id.tvRfcCondicion)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
        private val tvMarcas: TextView = itemView.findViewById(R.id.tvMarcas)
        private val tvContacto: TextView = itemView.findViewById(R.id.tvContacto)
        private val switchEstado: SwitchMaterial = itemView.findViewById(R.id.switchEstado)
        private val btnProductos: LinearLayout = itemView.findViewById(R.id.btnProductos)
        private val btnEditar: LinearLayout = itemView.findViewById(R.id.btnEditar)
        private val btnEliminar: ImageView = itemView.findViewById(R.id.btnEliminar)

        fun bind(proveedor: Proveedor, colorAvatar: String) {
            tvIniciales.text = proveedor.iniciales
            tvIniciales.background.setTint(android.graphics.Color.parseColor(colorAvatar))

            tvRazonSocial.text = proveedor.razonSocial

            val rfc = if (proveedor.rfcNit.isNotBlank()) "RFC: ${proveedor.rfcNit}" else "Sin RFC"
            val condicion = if (proveedor.condicionPago.isNotBlank()) " • ${proveedor.condicionPago}" else ""
            tvRfcCondicion.text = "$rfc$condicion"

            if (proveedor.listaMarcas.isNotEmpty()) {
                tvMarcas.visibility = View.VISIBLE
                tvMarcas.text = proveedor.listaMarcas.joinToString(", ")
            } else {
                tvMarcas.visibility = View.GONE
            }

            val contacto = proveedor.contactoEjecutivo
            val telefono = proveedor.telefonoContacto
            tvContacto.text = when {
                contacto.isNotBlank() && telefono.isNotBlank() -> "Ejecutivo: $contacto • $telefono"
                contacto.isNotBlank() -> "Ejecutivo: $contacto"
                telefono.isNotBlank() -> "Contacto: $telefono"
                else -> "Sin contacto registrado"
            }

            if (proveedor.esActivo) {
                tvEstado.text = "● Activo"
                tvEstado.setTextColor(android.graphics.Color.parseColor("#4CD964"))
                tvEstado.setBackgroundResource(R.drawable.bg_badge_activo)
            } else {
                tvEstado.text = "● Inactivo"
                tvEstado.setTextColor(android.graphics.Color.parseColor("#999999"))
                tvEstado.setBackgroundResource(R.drawable.bg_badge_inactivo)
            }

            // Se limpia el listener antes de setChecked para que no se dispare
            // onEstadoCambiado al reciclar la vista.
            switchEstado.setOnCheckedChangeListener(null)
            switchEstado.isChecked = proveedor.esActivo
            switchEstado.text = if (proveedor.esActivo) "Activo" else "Inactivo"
            switchEstado.setOnCheckedChangeListener { _, isChecked ->
                switchEstado.text = if (isChecked) "Activo" else "Inactivo"
                onEstadoCambiado(proveedor, isChecked)
            }

            btnProductos.setOnClickListener { onProductosClick(proveedor) }
            btnEditar.setOnClickListener { onEditarClick(proveedor) }
            btnEliminar.setOnClickListener { onEliminarClick(proveedor) }
        }
    }
}