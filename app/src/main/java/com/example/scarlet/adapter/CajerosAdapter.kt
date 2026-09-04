package com.example.scarlet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.R
import com.example.scarlet.data.repository.CajeroInfo

class CajerosAdapter(
    private var cajeros: List<CajeroInfo>,
    private val onEditar: (CajeroInfo) -> Unit,
    private val onCambiarEstado: (CajeroInfo) -> Unit
) : RecyclerView.Adapter<CajerosAdapter.CajeroViewHolder>() {

    inner class CajeroViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNombre: TextView = view.findViewById(R.id.txtNombreCajero)
        val txtUsuario: TextView = view.findViewById(R.id.txtUsuarioCajero)
        val txtEstado: TextView = view.findViewById(R.id.txtEstadoCajero)
        val btnEditar: TextView = view.findViewById(R.id.btnEditarCajero)
        val btnEstado: TextView = view.findViewById(R.id.btnEstadoCajero)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CajeroViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cajero, parent, false)
        return CajeroViewHolder(view)
    }

    override fun onBindViewHolder(holder: CajeroViewHolder, position: Int) {
        val cajero = cajeros[position]
        holder.txtNombre.text = "${cajero.nombres} ${cajero.apellidos}"
        holder.txtUsuario.text = "@${cajero.usuario}"

        val activo = cajero.estado == "ACTIVO"
        holder.txtEstado.text = if (activo) "ACTIVO" else "INACTIVO"
        holder.txtEstado.setTextColor(if (activo) 0xFF4CAF50.toInt() else 0xFF888888.toInt())
        holder.btnEstado.text = if (activo) "Desactivar" else "Activar"

        holder.btnEditar.setOnClickListener { onEditar(cajero) }
        holder.btnEstado.setOnClickListener { onCambiarEstado(cajero) }
    }

    override fun getItemCount(): Int = cajeros.size

    fun actualizar(nuevaLista: List<CajeroInfo>) {
        cajeros = nuevaLista
        notifyDataSetChanged()
    }
}