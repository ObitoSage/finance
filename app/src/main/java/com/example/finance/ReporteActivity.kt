package com.example.finance

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finance.databinding.ActivityReporteBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ReporteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReporteBinding
    private lateinit var auth: FirebaseAuth
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }
    
    private val categoryColors = mapOf(
        "Comida afuera" to "#EF4444",
        "Transporte" to "#3B82F6",
        "Café" to "#8B4513",
        "Mercado" to "#10B981",
        "Hogar" to "#F59E0B",
        "Entretenimiento" to "#EC4899",
        "Servicios" to "#6366F1",
        "Celular" to "#14B8A6",
        "Salud" to "#F472B6",
        "Educación" to "#8B5CF6",
        "Ropa" to "#EC4899",
        "Otros" to "#6B7280"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReporteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupClickListeners()
        cargarDatosReporte()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnCompartir.setOnClickListener {
            compartirReporte()
        }
    }

    private fun cargarDatosReporte() {
        val userId = auth.currentUser?.uid ?: return
        val app = application as FinanceApplication
        val repository = app.repository

        lifecycleScope.launch {
            try {
                // Obtener fecha actual
                val calendario = Calendar.getInstance()
                val mesActual = calendario.get(Calendar.MONTH)
                val anioActual = calendario.get(Calendar.YEAR)

                // Actualizar título con mes y año
                val mesNombre = calendario.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("es", "ES"))
                    ?.replaceFirstChar { it.uppercase() } ?: "Mes"
                binding.tvTitulo.text = "Resumen de $mesNombre de $anioActual"

                // Cargar todos los gastos del usuario
                val todosLosGastos = repository.getAllGastosList(userId)

                // Filtrar gastos del mes actual
                val gastosDelMes = todosLosGastos.filter { gasto ->
                    val fechaGasto = Calendar.getInstance().apply {
                        timeInMillis = gasto.fecha
                    }
                    fechaGasto.get(Calendar.MONTH) == mesActual &&
                    fechaGasto.get(Calendar.YEAR) == anioActual
                }

                // Calcular total gastado
                val totalGastado = gastosDelMes.sumOf { it.monto }
                binding.tvTotalGastado.text = formatCurrency(totalGastado)

                // Calcular gastos del mes pasado
                val mesPasado = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                }
                val gastosDelMesPasado = todosLosGastos.filter { gasto ->
                    val fechaGasto = Calendar.getInstance().apply {
                        timeInMillis = gasto.fecha
                    }
                    fechaGasto.get(Calendar.MONTH) == mesPasado.get(Calendar.MONTH) &&
                    fechaGasto.get(Calendar.YEAR) == mesPasado.get(Calendar.YEAR)
                }

                val totalMesPasado = gastosDelMesPasado.sumOf { it.monto }

                // Mostrar comparación con mes anterior
                mostrarComparacion(totalGastado, totalMesPasado)

                // Agrupar gastos por categoría
                val gastosPorCategoria = gastosDelMes.groupBy { it.categoria }
                    .mapValues { entry -> entry.value.sumOf { it.monto } }
                    .toList()
                    .sortedByDescending { it.second }

                // Configurar gráfico
                if (gastosPorCategoria.isNotEmpty()) {
                    configurarGrafico(gastosPorCategoria, totalGastado)
                    mostrarListaCategorias(gastosPorCategoria, totalGastado)
                    binding.pieChart.visibility = View.VISIBLE
                    binding.layoutEmpty.visibility = View.GONE
                    binding.layoutCategorias.visibility = View.VISIBLE
                } else {
                    binding.pieChart.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.layoutCategorias.visibility = View.GONE
                }

                // Calcular ahorro
                val totalIngresos = repository.getTotalIngresos(userId)
                val totalAhorrado = totalIngresos - repository.getTotalGastos(userId)
                binding.tvTotalAhorrado.text = formatCurrency(if (totalAhorrado > 0) totalAhorrado else 0.0)

            } catch (e: Exception) {
                showToast("Error al cargar reporte: ${e.message}")
            }
        }
    }

    private fun mostrarComparacion(totalActual: Double, totalAnterior: Double) {
        if (totalAnterior > 0) {
            val diferencia = totalActual - totalAnterior
            val porcentaje = ((diferencia / totalAnterior) * 100)
            val aumento = diferencia > 0

            binding.layoutComparacion.visibility = View.VISIBLE

            if (aumento) {
                binding.ivTendencia.setImageResource(R.drawable.ic_trending_up)
                binding.ivTendencia.setColorFilter(getColor(R.color.error_red))
                binding.tvComparacion.setTextColor(getColor(R.color.error_red))
                binding.layoutComparacion.setBackgroundColor(getColor(R.color.error_red_light))
                binding.tvComparacion.text = String.format("+%.1f%% vs mes anterior", porcentaje)
            } else {
                binding.ivTendencia.setImageResource(R.drawable.ic_trending_down)
                binding.ivTendencia.setColorFilter(getColor(R.color.success_green))
                binding.tvComparacion.setTextColor(getColor(R.color.success_green))
                binding.layoutComparacion.setBackgroundColor(getColor(R.color.success_green_bg))
                binding.tvComparacion.text = String.format("%.1f%% vs mes anterior", porcentaje)
            }
        } else {
            binding.layoutComparacion.visibility = View.GONE
        }
    }

    private fun configurarGrafico(gastosPorCategoria: List<Pair<String, Double>>, total: Double) {
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        gastosPorCategoria.forEach { (categoria, monto) ->
            entries.add(PieEntry(monto.toFloat(), categoria))
            val colorHex = categoryColors[categoria] ?: "#6B7280"
            colors.add(Color.parseColor(colorHex))
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE
        dataSet.sliceSpace = 2f
        dataSet.setDrawValues(true)

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter(binding.pieChart))

        binding.pieChart.apply {
            data = pieData
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 50f
            transparentCircleRadius = 55f
            setHoleColor(Color.TRANSPARENT)
            setDrawEntryLabels(false)
            legend.isEnabled = false
            setUsePercentValues(true)
            animateY(1000)
            invalidate()
        }
    }

    private fun mostrarListaCategorias(gastosPorCategoria: List<Pair<String, Double>>, total: Double) {
        binding.layoutCategorias.removeAllViews()

        gastosPorCategoria.forEach { (categoria, monto) ->
            val itemView = LayoutInflater.from(this).inflate(
                R.layout.item_categoria_reporte,
                binding.layoutCategorias,
                false
            )

            val colorIndicator = itemView.findViewById<View>(R.id.colorIndicator)
            val tvCategoria = itemView.findViewById<TextView>(R.id.tvCategoria)
            val tvMonto = itemView.findViewById<TextView>(R.id.tvMonto)
            val tvPorcentaje = itemView.findViewById<TextView>(R.id.tvPorcentaje)

            val colorHex = categoryColors[categoria] ?: "#6B7280"
            colorIndicator.setBackgroundColor(Color.parseColor(colorHex))

            tvCategoria.text = categoria
            tvMonto.text = formatCurrency(monto)

            val porcentaje = (monto / total * 100)
            tvPorcentaje.text = String.format("%.1f%%", porcentaje)

            binding.layoutCategorias.addView(itemView)
        }
    }

    private fun compartirReporte() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Mi reporte de gastos")
            putExtra(Intent.EXTRA_TEXT, "Reporte generado desde Plata Clara")
        }
        startActivity(Intent.createChooser(intent, "Compartir reporte"))
    }

    private fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount)
            .replace("COP", "$")
            .replace("\u00A0", "")
            .trim()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
