// app/src/main/java/com/example/scarlet/adapter/ProductosAdapter.kt

package com.example.scarlet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.R
import com.example.scarlet.data.model.Producto
import java.text.DecimalFormat

class ProductosAdapter(
    private var productos: List<Producto>,
    private val onItemClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductosAdapter.ProductoViewHolder>() {

    private val decimalFormat = DecimalFormat("$#,##0.00")

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
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombreProducto)
        private val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionProducto)
        private val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecioProducto)
        private val tvStock: TextView = itemView.findViewById(R.id.tvStockProducto)
        private val imgAgregarCarrito: android.widget.ImageView = itemView.findViewById(R.id.imgAgregarCarrito)

        fun bind(producto: Producto) {
            tvNombre.text = producto.nombreProducto
            tvDescripcion.text = producto.descripcion ?: "Sin descripción"
            tvPrecio.text = decimalFormat.format(producto.precioVenta)
            tvStock.text = "Stock: ${producto.stock}"

            itemView.setOnClickListener {
                onItemClick(producto)
            }
            imgAgregarCarrito.setOnClickListener {
                onItemClick(producto)
            }
        }
    }
}