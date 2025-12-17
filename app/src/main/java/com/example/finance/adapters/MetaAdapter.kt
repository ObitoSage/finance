package com.example.finance.adapters

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.finance.R
import com.example.finance.databinding.ItemMetaBinding
import com.example.finance.dataClass.Meta
import java.text.NumberFormat
import java.util.*

class MetaAdapter(
    private val onAgregarClick: (Meta) -> Unit,
    private val onEliminarClick: (Meta) -> Unit
) : RecyclerView.Adapter<MetaAdapter.MetaViewHolder>() {

    private var metas: List<Meta> = emptyList()
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    fun submitList(newMetas: List<Meta>) {
        val diffCallback = MetaDiffCallback(metas, newMetas)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        metas = newMetas
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetaViewHolder {
        val binding = ItemMetaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MetaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MetaViewHolder, position: Int) {
        holder.bind(metas[position])
    }

    override fun getItemCount(): Int = metas.size

    inner class MetaViewHolder(
        private val binding: ItemMetaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(meta: Meta) {
            // Verificar si la meta está completada
            val estaCompletada = meta.ahorrado >= meta.objetivo

            // Nombre - aplicar tachado si está completada
            binding.tvNombre.text = meta.nombre
            if (estaCompletada) {
                binding.tvNombre.paintFlags = binding.tvNombre.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvNombre.paintFlags = binding.tvNombre.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // Progreso en texto
            val progresoText = "${formatCurrency(meta.ahorrado)} de ${formatCurrency(meta.objetivo)}"
            binding.tvProgreso.text = progresoText

            // Porcentaje
            val porcentaje = meta.getPorcentaje()
            binding.tvPorcentaje.text = "${porcentaje.toInt()}% completado"
            
            // Color del texto del porcentaje
            val colorBase = Color.parseColor(meta.color)
            if (estaCompletada) {
                // Color más opaco (50% alpha)
                val colorOpaco = Color.argb(
                    128, // 50% de 255
                    Color.red(colorBase),
                    Color.green(colorBase),
                    Color.blue(colorBase)
                )
                binding.tvPorcentaje.setTextColor(colorOpaco)
            } else {
                binding.tvPorcentaje.setTextColor(colorBase)
            }

            // Barra de progreso
            binding.progressBar.max = 100
            binding.progressBar.progress = porcentaje.toInt()
            
            // Crear drawable personalizado con el color de la meta
            val progressDrawable = android.graphics.drawable.LayerDrawable(
                arrayOf(
                    android.graphics.drawable.ShapeDrawable().apply {
                        paint.color = android.graphics.Color.TRANSPARENT
                        shape = android.graphics.drawable.shapes.RoundRectShape(
                            floatArrayOf(6f, 6f, 6f, 6f, 6f, 6f, 6f, 6f),
                            null,
                            null
                        )
                    },
                    android.graphics.drawable.ClipDrawable(
                        android.graphics.drawable.ShapeDrawable().apply {
                            paint.color = if (estaCompletada) {
                                Color.argb(
                                    128,
                                    Color.red(colorBase),
                                    Color.green(colorBase),
                                    Color.blue(colorBase)
                                )
                            } else {
                                colorBase
                            }
                            shape = android.graphics.drawable.shapes.RoundRectShape(
                                floatArrayOf(6f, 6f, 6f, 6f, 6f, 6f, 6f, 6f),
                                null,
                                null
                            )
                        },
                        android.view.Gravity.LEFT,
                        android.graphics.drawable.ClipDrawable.HORIZONTAL
                    )
                )
            ).apply {
                setId(0, android.R.id.background)
                setId(1, android.R.id.progress)
            }
            
            binding.progressBar.progressDrawable = progressDrawable

            // Ícono y su contenedor
            val iconResId = getIconResource(meta.icono)
            binding.ivIcono.setImageResource(iconResId)
            
            // Color del icono
            if (estaCompletada) {
                val colorIconoOpaco = Color.argb(
                    128,
                    Color.red(colorBase),
                    Color.green(colorBase),
                    Color.blue(colorBase)
                )
                binding.ivIcono.setColorFilter(colorIconoOpaco)
            } else {
                binding.ivIcono.setColorFilter(colorBase)
            }
            
            // Color de fondo del contenedor del ícono (20% opacity)
            val backgroundColor = Color.argb(
                51, // 20% de 255
                Color.red(colorBase),
                Color.green(colorBase),
                Color.blue(colorBase)
            )
            binding.cvIconContainer.setCardBackgroundColor(backgroundColor)

            // Mostrar/ocultar botón agregar y mensaje de completada
            if (estaCompletada) {
                binding.btnAgregar.visibility = android.view.View.GONE
                binding.tvMetaCompletada.visibility = android.view.View.VISIBLE
            } else {
                binding.btnAgregar.visibility = android.view.View.VISIBLE
                binding.tvMetaCompletada.visibility = android.view.View.GONE
                binding.btnAgregar.setOnClickListener {
                    onAgregarClick(meta)
                }
            }

            // Botón eliminar
            binding.btnEliminar.setOnClickListener {
                onEliminarClick(meta)
            }
        }

        private fun formatCurrency(amount: Double): String {
            return currencyFormat.format(amount).replace("COP", "$")
        }

        private fun getIconResource(iconName: String): Int {
            return when (iconName) {
                "target" -> R.drawable.ic_target
                "plane" -> R.drawable.ic_plane
                "piggybank" -> R.drawable.ic_piggybank
                "home" -> R.drawable.ic_home
                "car" -> R.drawable.ic_car
                "book" -> R.drawable.ic_book
                "health" -> R.drawable.ic_health
                else -> R.drawable.ic_target
            }
        }
    }
    
    // DiffUtil para actualizaciones eficientes
    private class MetaDiffCallback(
        private val oldList: List<Meta>,
        private val newList: List<Meta>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize(): Int = oldList.size
        
        override fun getNewListSize(): Int = newList.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldMeta = oldList[oldItemPosition]
            val newMeta = newList[newItemPosition]
            return oldMeta.ahorrado == newMeta.ahorrado && 
                   oldMeta.objetivo == newMeta.objetivo &&
                   oldMeta.nombre == newMeta.nombre &&
                   oldMeta.color == newMeta.color &&
                   oldMeta.icono == newMeta.icono
        }
    }
}
