package com.example.finance.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.finance.R
import com.example.finance.databinding.ItemGastoBinding
import com.example.finance.dataClass.Gasto
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter para mostrar la lista de gastos en el Dashboard
 */
class GastoAdapter(
    private var gastos: List<Gasto> = emptyList()
) : RecyclerView.Adapter<GastoAdapter.GastoViewHolder>() {

    // Mapa de categorías a iconos
    private val categoryIcons = mapOf(
        "Comida afuera" to R.drawable.ic_food,
        "Transporte" to R.drawable.ic_transport,
        "Café" to R.drawable.ic_coffee,
        "Mercado" to R.drawable.ic_shopping,
        "Entretenimiento" to R.drawable.ic_categories,
        "Servicios" to R.drawable.ic_receipt,
        "Hogar" to R.drawable.ic_categories
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GastoViewHolder {
        val binding = ItemGastoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GastoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GastoViewHolder, position: Int) {
        holder.bind(gastos[position])
    }

    override fun getItemCount(): Int = gastos.size

    fun updateGastos(newGastos: List<Gasto>) {
        gastos = newGastos
        notifyDataSetChanged()
    }

    inner class GastoViewHolder(private val binding: ItemGastoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val timeFormat = SimpleDateFormat("HH:mm", Locale("es", "CO"))
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }

        fun bind(gasto: Gasto) {
            binding.apply {
                // Descripción
                tvDescription.text = gasto.descripcion
                
                // Hora
                tvTime.text = timeFormat.format(gasto.fecha)
                
                // Monto formateado
                val formattedAmount = currencyFormat.format(gasto.monto)
                    .replace("COP", "$")
                    .replace("\u00A0", "")
                    .trim()
                tvAmount.text = formattedAmount
                
                // Icono de categoría
                val iconRes = categoryIcons[gasto.categoria] ?: R.drawable.ic_shopping
                ivCategoryIcon.setImageResource(iconRes)
            }
        }
    }
}
