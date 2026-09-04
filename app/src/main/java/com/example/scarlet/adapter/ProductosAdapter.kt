// app/src/main/java/com/example/scarlet/adapter/ProductosAdapter.kt

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

class ProductosAdapter(
    private var productos: List<Producto>,
    private val onItemClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductosAdapter.ProductoViewHolder>() {

    private val decimalFormat = DecimalFormat("Bs #,##0.00")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = productos[position]
        holder.bind(producto)
    }

    override fun getItemCount(): Int = productos.size

    fun actualizarProductos(nuevosProductos: List<Producto>) {
        productos = nuevosProductos
        notifyDataSetChanged()
    }

    inner class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgProducto: ImageView = itemView.findViewById(R.id.imgProducto)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombreProducto)
        private val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionProducto)
        private val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecioProducto)
        private val tvStock: TextView = itemView.findViewById(R.id.tvStockProducto)
        private val imgAgregarCarrito: android.widget.ImageView = itemView.findViewById(R.id.imgAgregarCarrito)

        fun bind(producto: Producto) {
            ImagenUtils.cargarEnImageView(itemView.context, imgProducto, producto.imagen)

            tvNombre.text = producto.nombreProducto

            val volumen = producto.volumenMl?.let { formatearVolumen(it) }
            tvDescripcion.text = when {
                !producto.nombreCategoria.isNullOrBlank() && volumen != null ->
                    "${producto.nombreCategoria} • $volumen"
                !producto.nombreCategoria.isNullOrBlank() -> producto.nombreCategoria
                else -> producto.descripcion ?: "Sin descripción"
            }

            tvPrecio.text = decimalFormat.format(producto.precioVenta)
            tvStock.text = "Stock: ${producto.stock}"

            itemView.setOnClickListener {
                onItemClick(producto)
            }
            imgAgregarCarrito.setOnClickListener {
                onItemClick(producto)
            }
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