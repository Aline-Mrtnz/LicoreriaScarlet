package com.example.scarlet.util

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.example.scarlet.NuevaCompra
import com.example.scarlet.R
import com.example.scarlet.data.model.Producto
import com.example.scarlet.data.repository.ProductosRepository

/**
 * Campanita de alertas de stock bajo. Reemplaza el contador fijo que había
 * antes (notificationCount = 3, siempre igual, sin acción real) por la
 * lista real de productos con stock <= stock_minimo. Se usa igual desde
 * Inicio, Productos, Ventas y Reportes: cada Activity solo llama a
 * AlertasUtils.configurar(this) dentro de su setupNotifications().
 */
object AlertasUtils {

    fun configurar(activity: Activity) {
        val notificationIcon = activity.findViewById<ImageView>(R.id.imgNorificacion) ?: return
        val badge = activity.findViewById<TextView>(R.id.txtNotificationBadge) ?: return

        val alertas = try {
            ProductosRepository(activity).obtenerProductosConStockBajo()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }

        if (alertas.isNotEmpty()) {
            badge.text = if (alertas.size > 99) "99+" else alertas.size.toString()
            badge.visibility = View.VISIBLE
        } else {
            badge.visibility = View.GONE
        }

        notificationIcon.setOnClickListener {
            mostrarPopup(activity, notificationIcon, alertas)
        }
    }

    private fun mostrarPopup(activity: Activity, anchor: View, alertas: List<Producto>) {
        val inflater = LayoutInflater.from(activity)
        val contenido = inflater.inflate(R.layout.popup_alertas_stock, null)

        val txtContador = contenido.findViewById<TextView>(R.id.txtContadorAlertas)
        val layoutLista = contenido.findViewById<LinearLayout>(R.id.layoutListaAlertas)
        val txtSinAlertas = contenido.findViewById<TextView>(R.id.txtSinAlertas)

        txtContador.text = "${alertas.size} activas"
        layoutLista.removeAllViews()

        val esAdmin = Session.esAdmin

        if (alertas.isEmpty()) {
            txtSinAlertas.visibility = View.VISIBLE
        } else {
            txtSinAlertas.visibility = View.GONE
            alertas.forEach { producto ->
                val fila = inflater.inflate(R.layout.item_alerta_stock, layoutLista, false)

                fila.findViewById<TextView>(R.id.txtNombreAlerta).text = producto.nombreProducto

                val detalle = if (producto.stock <= 0) {
                    "Sin existencias. Requerido reposición de inventario."
                } else {
                    "Quedan ${producto.stock} (mín. ${producto.stockMinimo}). Reposición recomendada."
                }
                fila.findViewById<TextView>(R.id.txtDetalleAlerta).text = detalle

                val btnAccion = fila.findViewById<TextView>(R.id.btnReabastecerAlerta)
                if (esAdmin) {
                    btnAccion.visibility = View.VISIBLE
                    btnAccion.setOnClickListener {
                        val intent = Intent(activity, NuevaCompra::class.java)
                        intent.putExtra(NuevaCompra.EXTRA_ID_PRODUCTO, producto.idProducto)
                        activity.startActivity(intent)
                    }
                } else {
                    btnAccion.visibility = View.GONE
                }

                layoutLista.addView(fila)
            }
        }

        val anchoPx = (300 * activity.resources.displayMetrics.density).toInt()
        val popup = PopupWindow(contenido, anchoPx, LinearLayout.LayoutParams.WRAP_CONTENT, true)
        popup.isOutsideTouchable = true
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup.elevation = 16f
        popup.showAsDropDown(anchor, -anchoPx + anchor.width, 12)
    }
}