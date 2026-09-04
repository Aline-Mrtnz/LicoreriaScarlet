// app/src/main/java/com/example/scarlet/adapter/MovimientoAdapter.kt

package com.example.scarlet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.R
import com.example.scarlet.data.model.MovimientoInventario
import java.text.SimpleDateFormat
import java.util.Locale

class MovimientoAdapter(
    private var movimientos: List<MovimientoInventario>
) : RecyclerView.Adapter<MovimientoAdapter.MovimientoViewHolder>() {

    inner class MovimientoViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val txtFecha: TextView = itemView.findViewById(R.id.txtFechaMovimiento)
        val txtTipo: TextView = itemView.findViewById(R.id.txtTipoMovimiento)
        val imgTendencia: ImageView = itemView.findViewById(R.id.imgTendenciaMovimiento)
        val txtCantidad: TextView = itemView.findViewById(R.id.txtCantidadMovimiento)
        val txtDetalle: TextView = itemView.findViewById(R.id.txtDetalleMovimiento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovimientoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movimiento, parent, false)
        return MovimientoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovimientoViewHolder, position: Int) {
        val mov = movimientos[position]
        val esEntrada = mov.tipo.equals("ENTRADA", ignoreCase = true)

        holder.txtFecha.text = formatearFecha(mov.fecha)
        holder.txtTipo.text = mov.tipo

        if (esEntrada) {
            holder.txtTipo.setTextColor(0xFF3DDC84.toInt())
            holder.txtTipo.background = holder.itemView.context.getDrawable(R.drawable.bg_badge_activo)
            holder.imgTendencia.setImageResource(R.drawable.ic_trend_up)
            holder.txtCantidad.text = "+ ${mov.cantidad} unidad${if (mov.cantidad == 1) "" else "es"}"
        } else {
            holder.txtTipo.setTextColor(0xFFFF2A00.toInt())
            holder.txtTipo.background = holder.itemView.context.getDrawable(R.drawable.bg_badge_stock_bajo)
            holder.imgTendencia.setImageResource(R.drawable.ic_trend_down)
            holder.txtCantidad.text = "- ${mov.cantidad} unidad${if (mov.cantidad == 1) "" else "es"}"
        }

        val origenTexto = mov.origen ?: "-"
        val usuarioTexto = mov.usuario ?: "-"
        var detalle = "Origen: $origenTexto   Stock: ${mov.stockAnterior} → ${mov.stockNuevo}   Por: $usuarioTexto"
        if (!mov.nombreProveedor.isNullOrBlank()) {
            detalle += "\nProveedor: ${mov.nombreProveedor}"
        }
        if (!mov.notas.isNullOrBlank()) {
            detalle += "\nNotas: ${mov.notas}"
        }
        holder.txtDetalle.text = detalle
    }

    override fun getItemCount(): Int = movimientos.size

    fun actualizar(nuevaLista: List<MovimientoInventario>) {
        movimientos = nuevaLista
        notifyDataSetChanged()
    }

    private fun formatearFecha(fechaSql: String): String {
        return try {
            val formatoEntrada = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("es", "ES"))
            val formatoSalida = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("es", "ES"))
            val fecha = formatoEntrada.parse(fechaSql)
            if (fecha != null) formatoSalida.format(fecha) else fechaSql
        } catch (e: Exception) {
            fechaSql
        }
    }
}