package com.example.scarlet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.R
import com.example.scarlet.data.model.Compra
import java.text.SimpleDateFormat
import java.util.Locale

class ComprasAdapter(
    private var compras: List<Compra>,
    private val onVerDetalle: (Compra) -> Unit
) : RecyclerView.Adapter<ComprasAdapter.CompraViewHolder>() {

    class CompraViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtProveedor: TextView = view.findViewById(R.id.txtProveedorCompra)
        val txtCodigo: TextView = view.findViewById(R.id.txtCodigoCompra)
        val txtFecha: TextView = view.findViewById(R.id.txtFechaCompra)
        val txtEstado: TextView = view.findViewById(R.id.txtEstadoCompra)
        val txtTotal: TextView = view.findViewById(R.id.txtTotalCompraItem)
        val btnVerDetalle: TextView = view.findViewById(R.id.btnVerDetalle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompraViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_compra, parent, false)
        return CompraViewHolder(view)
    }

    override fun onBindViewHolder(holder: CompraViewHolder, position: Int) {
        val compra = compras[position]

        holder.txtProveedor.text = compra.razonSocialProveedor ?: "Proveedor"
        holder.txtCodigo.text = compra.codigo
        holder.txtFecha.text = formatearFecha(compra.fechaEmision)
        holder.txtTotal.text = formatearBs(compra.total)

        when (compra.estado) {
            "RECIBIDA" -> {
                holder.txtEstado.text = "RECIBIDA"
                holder.txtEstado.setBackgroundResource(R.drawable.bg_pill_recibida)
                holder.txtEstado.setTextColor(0xFF4CAF50.toInt())
            }
            "ANULADA" -> {
                holder.txtEstado.text = "ANULADA"
                holder.txtEstado.setBackgroundResource(R.drawable.bg_pill_anulada)
                holder.txtEstado.setTextColor(0xFFFF5252.toInt())
            }
            else -> {
                holder.txtEstado.text = "PENDIENTE"
                holder.txtEstado.setBackgroundResource(R.drawable.bg_pill_pendiente)
                holder.txtEstado.setTextColor(0xFFFFC107.toInt())
            }
        }

        holder.itemView.setOnClickListener { onVerDetalle(compra) }
        holder.btnVerDetalle.setOnClickListener { onVerDetalle(compra) }
    }

    override fun getItemCount(): Int = compras.size

    fun actualizarCompras(nuevaLista: List<Compra>) {
        compras = nuevaLista
        notifyDataSetChanged()
    }

    companion object {
        fun formatearBs(monto: Double): String {
            return "Bs " + String.format(Locale("es", "BO"), "%,.2f", monto)
        }

        fun formatearFecha(fechaCompleta: String): String {
            return try {
                val entrada = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val salida = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
                salida.format(entrada.parse(fechaCompleta)!!)
            } catch (e: Exception) {
                fechaCompleta.take(10)
            }
        }
    }
}