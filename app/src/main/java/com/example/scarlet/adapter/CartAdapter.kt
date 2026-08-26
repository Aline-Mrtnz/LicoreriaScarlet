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
import com.example.scarlet.data.model.CartItem
import java.text.NumberFormat
import java.util.Locale

/**
 * Adaptador de RecyclerView para el carrito de compras (pantalla Shopping).
 * Permite subir/bajar la cantidad de cada producto y notifica los cambios
 * mediante [onCantidadCambiada] para que la Activity recalcule el resumen.
 */
class CartAdapter(
    private val items: MutableList<CartItem>,
    private val onCantidadCambiada: () -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private val format = NumberFormat.getCurrencyInstance(Locale.US)

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
            imgProducto.setImageResource(item.imagenResId)
            tvNombre.text = item.nombre
            tvCategoria.text = item.categoria
            tvCantidad.text = item.cantidad.toString()
            actualizarPrecio(item)

            btnRestar.setOnClickListener {
                if (item.cantidad > 1) {
                    item.cantidad -= 1
                    tvCantidad.text = item.cantidad.toString()
                    actualizarPrecio(item)
                    onCantidadCambiada()
                } else {
                    Toast.makeText(itemView.context, "La cantidad mínima es 1", Toast.LENGTH_SHORT).show()
                }
            }

            btnSumar.setOnClickListener {
                item.cantidad += 1
                tvCantidad.text = item.cantidad.toString()
                actualizarPrecio(item)
                onCantidadCambiada()
            }
        }

        private fun actualizarPrecio(item: CartItem) {
            tvPrecio.text = format.format(item.subtotal)
        }
    }
}
