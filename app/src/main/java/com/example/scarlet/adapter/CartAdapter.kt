package com.example.scarlet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.R
import com.example.scarlet.cart.CartManager
import com.example.scarlet.data.model.CartItem
import com.example.scarlet.util.ImagenUtils
import java.text.NumberFormat
import java.util.Locale

/**
 * Adaptador de RecyclerView para el carrito de compras (pantalla Shopping).
 * Los cambios de cantidad/eliminación se aplican siempre a través de
 * [CartManager] (fuente única de verdad del carrito) y luego se notifica a
 * través de [onCambio] para que la Activity refresque el resumen y la lista.
 */
class CartAdapter(
    private val items: MutableList<CartItem>,
    private val onCambio: () -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private val format = java.text.DecimalFormat("'Bs '#,##0.00")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgProducto: ImageView = itemView.findViewById(R.id.imgProductoCart)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombreCart)
        private val tvCategoria: TextView = itemView.findViewById(R.id.tvCategoriaCart)
        private val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecioCart)
        private val tvCantidad: TextView = itemView.findViewById(R.id.tvCantidadCart)
        private val btnRestar: Button = itemView.findViewById(R.id.btnRestarCart)
        private val btnSumar: Button = itemView.findViewById(R.id.btnSumarCart)

        fun bind(item: CartItem) {
            imgProducto.setImageResource(ImagenUtils.resolver(itemView.context, item.imagenNombre))
            tvNombre.text = item.nombre
            tvCategoria.text = item.categoria
            tvCantidad.text = item.cantidad.toString()
            tvPrecio.text = format.format(item.subtotal)

            btnRestar.setOnClickListener {
                if (item.cantidad > 1) {
                    CartManager.actualizarCantidad(item, item.cantidad - 1)
                } else {
                    // Bajar de 1 elimina el producto del carrito
                    CartManager.quitarItem(item)
                    Toast.makeText(itemView.context, "${item.nombre} eliminado del carrito", Toast.LENGTH_SHORT).show()
                }
                onCambio()
            }

            btnSumar.setOnClickListener {
                if (item.cantidad >= item.stockDisponible) {
                    Toast.makeText(itemView.context, "No hay más stock disponible", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                CartManager.actualizarCantidad(item, item.cantidad + 1)
                onCambio()
            }

            itemView.setOnLongClickListener {
                CartManager.quitarItem(item)
                Toast.makeText(itemView.context, "${item.nombre} eliminado del carrito", Toast.LENGTH_SHORT).show()
                onCambio()
                true
            }
        }
    }
}