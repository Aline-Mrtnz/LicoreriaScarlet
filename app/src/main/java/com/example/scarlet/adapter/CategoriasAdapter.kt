package com.example.scarlet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.scarlet.R
import com.example.scarlet.data.model.CategoriaConConteo
import com.example.scarlet.util.ImagenUtils

class CategoriasAdapter(
    private var items: List<CategoriaConConteo>,
    private val onToggleEstado: (CategoriaConConteo, Boolean) -> Unit,
    private val onVerDetalles: (CategoriaConConteo) -> Unit,
    private val onEditar: (CategoriaConConteo) -> Unit,
    private val onEliminar: (CategoriaConConteo) -> Unit
) : RecyclerView.Adapter<CategoriasAdapter.CategoriaViewHolder>() {

    fun actualizar(nuevaLista: List<CategoriaConConteo>) {
        items = nuevaLista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_categoria, parent, false)
        return CategoriaViewHolder(vista)
    }

    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CategoriaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imgFoto: ImageView = itemView.findViewById(R.id.imgCategoriaFoto)
        private val tvIniciales: TextView = itemView.findViewById(R.id.tvCategoriaIniciales)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombreCategoria)
        private val tvBadgeDestacado: TextView = itemView.findViewById(R.id.tvBadgeDestacado)
        private val tvSubtitulo: TextView = itemView.findViewById(R.id.tvSubtitulo)
        private val tvBadgeEstado: TextView = itemView.findViewById(R.id.tvBadgeEstado)
        private val llTags: LinearLayout = itemView.findViewById(R.id.llTags)
        private val tvMarcas: TextView = itemView.findViewById(R.id.tvMarcas)
        private val switchEstado: SwitchCompat = itemView.findViewById(R.id.switchEstado)
        private val btnVerDetalles: View = itemView.findViewById(R.id.btnVerDetalles)
        private val btnEditar: View = itemView.findViewById(R.id.btnEditar)
        private val btnEliminar: View = itemView.findViewById(R.id.btnEliminar)

        fun bind(item: CategoriaConConteo) {
            val categoria = item.categoria
            val contexto = itemView.context

            tvNombre.text = categoria.nombre_categoria
            tvBadgeDestacado.visibility = if (categoria.destacado) View.VISIBLE else View.GONE

            val etiquetasList = categoria.listaEtiquetas()
            val marcasTexto = if (etiquetasList.isNotEmpty())
                "${item.cantidadProductos} Productos • Pos. #${categoria.orden_menu}"
            else
                "${item.cantidadProductos} Productos • Pos. #${categoria.orden_menu}"
            tvSubtitulo.text = marcasTexto

            if (categoria.estaActiva) {
                tvBadgeEstado.text = "● Activa"
                tvBadgeEstado.setTextColor(0xFF4CD964.toInt())
                tvBadgeEstado.setBackgroundResource(R.drawable.bg_badge_activo)
            } else {
                tvBadgeEstado.text = "● Inactiva"
                tvBadgeEstado.setTextColor(0xFF999999.toInt())
                tvBadgeEstado.setBackgroundResource(R.drawable.bg_badge_inactivo)
            }

            // Foto o iniciales como respaldo
            if (!categoria.imagen_referencia.isNullOrBlank()) {
                imgFoto.visibility = View.VISIBLE
                tvIniciales.visibility = View.GONE
                ImagenUtils.cargarEnImageView(contexto, imgFoto, categoria.imagen_referencia)
            } else {
                imgFoto.visibility = View.GONE
                tvIniciales.visibility = View.VISIBLE
                tvIniciales.text = obtenerIniciales(categoria.nombre_categoria)
            }

            // Etiquetas como chips
            llTags.removeAllViews()
            val etiquetasVisibles = etiquetasList.take(4)
            if (etiquetasVisibles.isEmpty()) {
                llTags.visibility = View.GONE
            } else {
                llTags.visibility = View.VISIBLE
                etiquetasVisibles.forEach { etiqueta ->
                    val chip = TextView(contexto).apply {
                        text = etiqueta
                        textSize = 10f
                        setTextColor(0xFFCCCCCC.toInt())
                        setBackgroundResource(R.drawable.bg_contacto_chip)
                        setPadding(dp(contexto, 8), dp(contexto, 3), dp(contexto, 8), dp(contexto, 3))
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.marginEnd = dp(contexto, 6)
                        layoutParams = params
                    }
                    llTags.addView(chip)
                }
            }

            // Línea de "descripción" (reutilizamos el campo descripción como resumen de marcas/uso)
            if (categoria.descripcion.isNotBlank()) {
                tvMarcas.visibility = View.VISIBLE
                tvMarcas.text = categoria.descripcion
            } else {
                tvMarcas.visibility = View.GONE
            }

            // Switch sin disparar el listener al hacer bind
            switchEstado.setOnCheckedChangeListener(null)
            switchEstado.isChecked = categoria.estaActiva
            switchEstado.text = if (categoria.estaActiva) "Activa" else "Inactiva"
            switchEstado.setOnCheckedChangeListener { _, checked ->
                onToggleEstado(item, checked)
            }

            btnVerDetalles.setOnClickListener { onVerDetalles(item) }
            btnEditar.setOnClickListener { onEditar(item) }
            btnEliminar.setOnClickListener { onEliminar(item) }
        }

        private fun obtenerIniciales(nombre: String): String {
            val palabras = nombre.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            return when {
                palabras.isEmpty() -> "?"
                palabras.size == 1 -> palabras[0].take(2).uppercase()
                else -> (palabras[0].take(1) + palabras[1].take(1)).uppercase()
            }
        }

        private fun dp(contexto: android.content.Context, valor: Int): Int {
            return (valor * contexto.resources.displayMetrics.density).toInt()
        }
    }
}