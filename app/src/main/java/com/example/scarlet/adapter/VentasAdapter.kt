package com.example.scarlet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.R
import com.example.scarlet.data.model.VentaResumen
import java.text.NumberFormat
import java.util.Locale

/**
 * Adaptador para el historial de ventas (pantalla Ventas). Cada fila muestra
 * el id de venta, la hora, el cliente/tipo de pago, la lista de productos
 * vendidos (resuelta aparte, ya que no viene en VentaResumen) y el total.
 */
class VentasAdapter(
    private var ventas: List<VentaResumen>,
    private val obtenerLineasDeVenta: (Int) -> List<String>
) : RecyclerView.Adapter<VentasAdapter.VentaViewHolder>() {

    private val format = java.text.DecimalFormat("Bs #,##0.00")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VentaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_venta, parent, false)
        return VentaViewHolder(view)
    }

    override fun onBindViewHolder(holder: VentaViewHolder, position: Int) {
        holder.bind(ventas[position])
    }

    override fun getItemCount(): Int = ventas.size

    fun actualizar(nuevasVentas: List<VentaResumen>) {
        ventas = nuevasVentas
        notifyDataSetChanged()
    }

    inner class VentaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtId: TextView = itemView.findViewById(R.id.txtIdVenta)
        private val txtFechaHora: TextView = itemView.findViewById(R.id.txtFechaHora)
        private val txtEstado: TextView = itemView.findViewById(R.id.txtEstado)
        private val layoutProductos: LinearLayout = itemView.findViewById(R.id.layoutProductos)
        private val txtTotal: TextView = itemView.findViewById(R.id.txtTotalVenta)

        fun bind(venta: VentaResumen) {
            txtId.text = "#SC-${1000 + venta.idVenta}"

            val hora = try {
                venta.fechaVenta.substring(11, 16)
            } catch (e: Exception) {
                venta.fechaVenta
            }
            txtFechaHora.text = "$hora • ${venta.nombreCliente} • ${venta.tipoPago}"
            txtEstado.text = "COMPLETADO"

            layoutProductos.removeAllViews()
            val lineas = obtenerLineasDeVenta(venta.idVenta)
            lineas.forEach { linea ->
                val tv = TextView(itemView.context).apply {
                    text = linea
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 13f
                }
                layoutProductos.addView(tv)
            }

            txtTotal.text = format.format(venta.total)
        }
    }
}