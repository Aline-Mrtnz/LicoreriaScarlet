package com.example.scarlet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.R
import com.example.scarlet.data.model.TopProducto
import java.text.NumberFormat
import java.util.Locale

class TopProductosAdapter(
    private var items: List<TopProducto>
) : RecyclerView.Adapter<TopProductosAdapter.TopProductoViewHolder>() {

    private val format = java.text.DecimalFormat("Bs #,##0.00")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_producto, parent, false)
        return TopProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TopProductoViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun actualizar(nuevosItems: List<TopProducto>) {
        items = nuevosItems
        notifyDataSetChanged()
    }

    inner class TopProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val img: ImageView = itemView.findViewById(R.id.imgTopProducto)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombreTopProducto)
        private val tvCategoria: TextView = itemView.findViewById(R.id.tvCategoriaTopProducto)
        private val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecioTopProducto)
        private val tvVendidos: TextView = itemView.findViewById(R.id.tvVendidosTopProducto)

        fun bind(item: TopProducto) {
            img.setImageResource(item.imagenResId)
            tvNombre.text = item.nombre
            tvCategoria.text = item.categoria
            tvPrecio.text = format.format(item.precio)
            tvVendidos.text = "${item.vendidos} vendidos"
        }
    }
}