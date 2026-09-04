// app/src/main/java/com/example/scarlet/adapter/InventarioAdapter.kt

package com.example.scarlet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.R
import com.example.scarlet.data.model.Producto
import java.util.Locale

class InventarioAdapter(
    private var productos: List<Producto>,
    private val onClickCard: (Producto) -> Unit,
    private val onReabastecer: (Producto) -> Unit,
    private val onEditar: (Producto) -> Unit,
    private val onEliminar: (Producto) -> Unit,
    private val onToggleActivo: (Producto, Boolean) -> Unit
) : RecyclerView.Adapter<InventarioAdapter.InventarioViewHolder>() {

    inner class InventarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: LinearLayout = itemView.findViewById(R.id.cardInventarioItem)
        val txtAvatar: TextView = itemView.findViewById(R.id.txtAvatarProducto)
        val txtNombre: TextView = itemView.findViewById(R.id.txtNombreProducto)
        val txtBadge: TextView = itemView.findViewById(R.id.txtBadgeEstado)
        val txtInfo1: TextView = itemView.findViewById(R.id.txtInfoLinea1)
        val txtInfo2: TextView = itemView.findViewById(R.id.txtInfoLinea2)
        val txtStockLabel: TextView = itemView.findViewById(R.id.txtStockLabel)
        val txtValor: TextView = itemView.findViewById(R.id.txtValorProducto)
        val progressFill: View = itemView.findViewById(R.id.progressFillStock)
        val switchActivo: SwitchCompat = itemView.findViewById(R.id.switchActivo)
        val btnReabastecer: LinearLayout = itemView.findViewById(R.id.btnReabastecerItem)
        val btnEditar: LinearLayout = itemView.findViewById(R.id.btnEditarItem)
        val btnEliminar: LinearLayout = itemView.findViewById(R.id.btnEliminarItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inventario_producto, parent, false)
        return InventarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: InventarioViewHolder, position: Int) {
        val producto = productos[position]

        holder.txtAvatar.text = iniciales(producto.nombreProducto)
        holder.txtNombre.text = producto.nombreProducto

        val activo = producto.estado.equals("ACTIVO", ignoreCase = true)
        val sinStock = producto.stock <= 0
        val stockBajo = producto.stock in 1..producto.stockMinimo

        when {
            !activo -> {
                holder.txtBadge.text = "Inactivo"
                holder.txtBadge.setTextColor(0xFF999999.toInt())
                holder.txtBadge.background = holder.itemView.context.getDrawable(R.drawable.bg_badge_inactivo)
            }
            sinStock -> {
                holder.txtBadge.text = "Sin Stock"
                holder.txtBadge.setTextColor(0xFFFF2A00.toInt())
                holder.txtBadge.background = holder.itemView.context.getDrawable(R.drawable.bg_badge_stock_bajo)
            }
            stockBajo -> {
                holder.txtBadge.text = "Stock Bajo"
                holder.txtBadge.setTextColor(0xFFFF2A00.toInt())
                holder.txtBadge.background = holder.itemView.context.getDrawable(R.drawable.bg_badge_stock_bajo)
            }
            else -> {
                holder.txtBadge.text = "Normal"
                holder.txtBadge.setTextColor(0xFF3DDC84.toInt())
                holder.txtBadge.background = holder.itemView.context.getDrawable(R.drawable.bg_badge_activo)
            }
        }

        val categoria = (producto.nombreCategoria ?: "Sin categoría").uppercase(Locale.getDefault())
        val volumen = producto.volumenMl?.let { "${it}ml" } ?: "—"
        holder.txtInfo1.text = "$categoria • $volumen • Costo: Bs ${formatearMonto(producto.precioCompra ?: 0.0)} • P. Venta: Bs ${formatearMonto(producto.precioVenta)}"

        val marca = producto.nombreMarca ?: "Sin marca"
        val abv = producto.abv?.let { "${it}% Alc." } ?: "—"
        holder.txtInfo2.text = "$marca • $abv"

        holder.txtStockLabel.text = "Stock actual: ${producto.stock} unidades (Mínimo ${producto.stockMinimo})"
        holder.txtStockLabel.setTextColor(if (sinStock || stockBajo) 0xFFFF2A00.toInt() else 0xFF3DDC84.toInt())

        val valorProducto = producto.precioVenta * producto.stock
        holder.txtValor.text = "Valor: Bs ${formatearMonto(valorProducto)}"

        // Barra de progreso: proporción de stock respecto a un techo de referencia
        // (3x el mínimo, con un piso de 10 unidades para que no se vea vacía en productos
        // con mínimos muy pequeños).
        val techo = maxOf(producto.stockMinimo * 3, 10)
        val proporcion = if (techo > 0) (producto.stock.toFloat() / techo.toFloat()).coerceIn(0f, 1f) else 0f
        holder.progressFill.post {
            val anchoTotal = (holder.progressFill.parent as View).width
            val params = holder.progressFill.layoutParams
            params.width = (anchoTotal * proporcion).toInt()
            holder.progressFill.layoutParams = params
        }
        holder.progressFill.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (sinStock || stockBajo) 0xFFFF2A00.toInt() else 0xFF3DDC84.toInt()
        )

        // Evitar que el listener del switch dispare onToggleActivo al reciclar la vista
        holder.switchActivo.setOnCheckedChangeListener(null)
        holder.switchActivo.isChecked = activo
        holder.switchActivo.setOnCheckedChangeListener { _, isChecked ->
            onToggleActivo(producto, isChecked)
        }

        holder.card.setOnClickListener { onClickCard(producto) }
        holder.btnReabastecer.setOnClickListener { onReabastecer(producto) }
        holder.btnEditar.setOnClickListener { onEditar(producto) }
        holder.btnEliminar.setOnClickListener { onEliminar(producto) }
    }

    override fun getItemCount(): Int = productos.size

    fun actualizar(nuevaLista: List<Producto>) {
        productos = nuevaLista
        notifyDataSetChanged()
    }

    private fun iniciales(nombre: String): String {
        val palabras = nombre.trim().split(" ").filter { it.isNotBlank() }
        return when {
            palabras.isEmpty() -> "?"
            palabras.size == 1 -> palabras[0].take(2).uppercase(Locale.getDefault())
            else -> (palabras[0].take(1) + palabras[1].take(1)).uppercase(Locale.getDefault())
        }
    }

    private fun formatearMonto(valor: Double): String {
        return String.format(Locale.getDefault(), "%,.2f", valor)
    }
}