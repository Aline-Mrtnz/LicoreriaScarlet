package com.example.scarlet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.R
import com.example.scarlet.data.model.Producto
import com.example.scarlet.util.ImagenUtils
import java.text.DecimalFormat

class ProductosGridAdapter(
    private var productos: List<Producto>,
    private val onAgregarClick: (Producto) -> Unit,
    private val onDescripcionClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductosGridAdapter.ProductoGridViewHolder>() {

    private val decimalFormat = DecimalFormat("Bs #,##0.00")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoGridViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto_grid, parent, false)
        return ProductoGridViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoGridViewHolder, position: Int) {
        holder.bind(productos[position])
    }

    override fun getItemCount(): Int = productos.size

    fun actualizarProductos(nuevosProductos: List<Producto>) {
        productos = nuevosProductos
        notifyDataSetChanged()
    }

    inner class ProductoGridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProducto: ImageView = itemView.findViewById(R.id.ivProductoGrid)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombreProductoGrid)
        private val tvSubtitulo: TextView = itemView.findViewById(R.id.tvSubtituloProductoGrid)
        private val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecioProductoGrid)
        private val imgAgregar: ImageView = itemView.findViewById(R.id.imgAgregarCarritoGrid)
        private val tvDescripcionBtn: TextView = itemView.findViewById(R.id.tvDescripcionBtnGrid)

        fun bind(producto: Producto) {
            ImagenUtils.cargarEnImageView(itemView.context, ivProducto, producto.imagen)

            tvNombre.text = producto.nombreProducto
            tvSubtitulo.text = construirSubtitulo(producto)
            tvPrecio.text = decimalFormat.format(producto.precioVenta)

            imgAgregar.setOnClickListener { onAgregarClick(producto) }
            tvDescripcionBtn.setOnClickListener { onDescripcionClick(producto) }
            itemView.setOnClickListener { onDescripcionClick(producto) }
        }

        private fun construirSubtitulo(producto: Producto): String {
            val volumen = producto.volumenMl?.let { formatearVolumen(it) }
            val abv = producto.abv?.let { "${formatearAbv(it)}% ABV" }
            return when {
                volumen != null && abv != null -> "$volumen • $abv"
                volumen != null -> volumen
                abv != null -> abv
                !producto.nombreCategoria.isNullOrBlank() -> producto.nombreCategoria
                else -> ""
            }
        }

        private fun formatearAbv(valor: Double): String {
            return if (valor == valor.toInt().toDouble()) valor.toInt().toString() else valor.toString()
        }

        private fun formatearVolumen(ml: Int): String {
            return if (ml >= 1000) {
                val litros = ml / 1000.0
                if (litros == litros.toInt().toDouble()) "${litros.toInt()}L" else "${litros}L"
            } else {
                "${ml}ml"
            }
        }
    }
}