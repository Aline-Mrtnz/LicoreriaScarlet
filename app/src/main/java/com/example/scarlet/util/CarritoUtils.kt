package com.example.scarlet.util

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.example.scarlet.Productos
import com.example.scarlet.R
import com.example.scarlet.Shopping
import com.example.scarlet.cart.CartManager

/**
 * Icono del carrito compartido por Inicio, Productos, Ventas y Reportes:
 * si hay productos agregados, lleva directo a Shopping (compras); si está
 * vacío, muestra un popup informativo con el mismo estilo visual que
 * AlertasUtils en vez de un Toast fácil de perder.
 */
object CarritoUtils {

    fun manejarClick(activity: Activity, anchor: View) {
        if (CartManager.estaVacio()) {
            mostrarPopupVacio(activity, anchor)
        } else {
            activity.startActivity(Intent(activity, Shopping::class.java))
        }
    }

    private fun mostrarPopupVacio(activity: Activity, anchor: View) {
        val inflater = LayoutInflater.from(activity)
        val contenido = inflater.inflate(R.layout.popup_carrito_vacio, null)

        contenido.findViewById<TextView>(R.id.btnIrProductosCarritoVacio).setOnClickListener {
            activity.startActivity(Intent(activity, Productos::class.java))
        }

        val anchoPx = (300 * activity.resources.displayMetrics.density).toInt()
        val popup = PopupWindow(contenido, anchoPx, LinearLayout.LayoutParams.WRAP_CONTENT, true)
        popup.isOutsideTouchable = true
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup.elevation = 16f
        popup.showAsDropDown(anchor, -anchoPx + anchor.width, 12)
    }
}