package com.example.finance

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finance.adapters.GastoAdapter
import com.example.finance.databinding.ActivityDashboardBinding
import com.example.finance.models.ChartData
import com.example.finance.models.Gasto
import com.example.finance.models.Ingreso
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var gastoAdapter: GastoAdapter

    // Datos
    private var gastos: MutableList<Gasto> = mutableListOf()
    private var ingresos: MutableList<Ingreso> = mutableListOf()
    private var presupuestoMensual: Double = 0.0
    private var userName: String = ""

    // Formato de moneda
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "BO")).apply {
        maximumFractionDigits = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Verificar sesión
        if (auth.currentUser == null) {
            navigateToLogin()
            return
        }

        setupUI()
        setupRecyclerView()
        setupChart()
        setupClickListeners()
        loadUserData()
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos cuando vuelve a la pantalla
        loadUserData()
    }

    private fun setupUI() {
        // Configurar fecha actual
        val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM yyyy", Locale("es", "ES"))
        binding.tvDate.text = dateFormat.format(Date()).replaceFirstChar { it.uppercase() }
    }

    private fun setupRecyclerView() {
        gastoAdapter = GastoAdapter()
        binding.rvLastExpenses.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = gastoAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setDrawGridBackground(false)
            setDrawBorders(false)
            
            // Configurar eje X
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = ContextCompat.getColor(context, R.color.gray_500)
                textSize = 12f
                granularity = 1f
            }
            
            // Ocultar eje Y izquierdo
            axisLeft.apply {
                isEnabled = false
            }
            
            // Ocultar eje Y derecho
            axisRight.apply {
                isEnabled = false
            }
            
            // Padding extra
            setExtraOffsets(16f, 16f, 16f, 16f)
        }
    }

    private fun setupClickListeners() {
        // Botón configurar presupuesto
        binding.btnConfigureBudget.setOnClickListener {
            navigateToConfigureBudget()
        }

        // Botón agregar gasto
        binding.btnAddExpense.setOnClickListener {
            // TODO: Navegar a pantalla de registrar gasto
            showToast("Registrar gasto (próximamente)")
        }

        // Botón agregar ingreso
        binding.btnAddIncome.setOnClickListener {
            // TODO: Navegar a pantalla de registrar ingreso
            showToast("Registrar ingreso (próximamente)")
        }

        // Accesos rápidos
        binding.btnCategories.setOnClickListener {
            // TODO: Navegar a categorías
            showToast("Categorías (próximamente)")
        }

        binding.btnGoals.setOnClickListener {
            // TODO: Navegar a metas
            showToast("Metas (próximamente)")
        }

        binding.btnHistory.setOnClickListener {
            // TODO: Navegar a historial
            showToast("Historial (próximamente)")
        }

        binding.btnReport.setOnClickListener {
            // TODO: Navegar a reporte
            showToast("Reporte del mes (próximamente)")
        }

        binding.btnProfile.setOnClickListener {
            // TODO: Navegar a perfil
            showToast("Perfil (próximamente)")
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        // Cargar nombre del usuario
        userName = auth.currentUser?.displayName 
            ?: auth.currentUser?.email?.substringBefore("@") 
            ?: "Usuario"
        
        val firstName = userName.split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Usuario"
        binding.tvGreeting.text = getString(R.string.dashboard_greeting, firstName)

        // Cargar presupuesto mensual
        loadBudget(userId)
        
        // Cargar gastos
        loadGastos(userId)
        
        // Cargar ingresos
        loadIngresos(userId)
    }

    private fun loadBudget(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    presupuestoMensual = document.getDouble("presupuestoMensual") ?: 0.0
                    updateBudgetUI()
                } else {
                    presupuestoMensual = 0.0
                    updateBudgetUI()
                }
            }
            .addOnFailureListener {
                presupuestoMensual = 0.0
                updateBudgetUI()
            }
    }

    private fun loadGastos(userId: String) {
        // Obtener primer día del mes actual
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.time

        db.collection("gastos")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("fecha", startOfMonth)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                gastos.clear()
                for (document in documents) {
                    val gasto = Gasto(
                        id = document.id,
                        categoria = document.getString("categoria") ?: "",
                        descripcion = document.getString("descripcion") ?: "",
                        monto = document.getDouble("monto") ?: 0.0,
                        fecha = document.getDate("fecha") ?: Date(),
                        userId = userId
                    )
                    gastos.add(gasto)
                }
                updateGastosUI()
                updateChartData()
                updateBalanceUI()
                updateProgressUI()
            }
            .addOnFailureListener {
                gastos.clear()
                updateGastosUI()
                updateChartData()
                updateBalanceUI()
                updateProgressUI()
            }
    }

    private fun loadIngresos(userId: String) {
        // Obtener primer día del mes actual
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.time

        db.collection("ingresos")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("fecha", startOfMonth)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                ingresos.clear()
                for (document in documents) {
                    val ingreso = Ingreso(
                        id = document.id,
                        categoria = document.getString("categoria") ?: "",
                        descripcion = document.getString("descripcion") ?: "",
                        monto = document.getDouble("monto") ?: 0.0,
                        fecha = document.getDate("fecha") ?: Date(),
                        userId = userId
                    )
                    ingresos.add(ingreso)
                }
                updateBalanceUI()
            }
            .addOnFailureListener {
                ingresos.clear()
                updateBalanceUI()
            }
    }

    private fun updateBudgetUI() {
        val presupuestoDiario = if (presupuestoMensual > 0) (presupuestoMensual / 30).toInt() else 0
        
        // Calcular gasto de hoy
        val gastoHoy = calcularGastoHoy()
        
        if (presupuestoDiario > 0) {
            binding.btnConfigureBudget.visibility = View.GONE
            
            val remaining = presupuestoDiario - gastoHoy
            if (remaining >= 0) {
                binding.tvBudgetLabel.text = getString(R.string.dashboard_today_remaining)
                binding.tvDailyAmount.text = formatCurrency(remaining.toDouble())
            } else {
                binding.tvBudgetLabel.text = getString(R.string.dashboard_today_spent)
                binding.tvDailyAmount.text = formatCurrency(gastoHoy.toDouble())
            }
            
            binding.tvDailyBudgetInfo.text = getString(R.string.dashboard_daily_budget_value, formatCurrency(presupuestoDiario.toDouble()))
        } else {
            binding.tvBudgetLabel.text = getString(R.string.dashboard_daily_budget_label)
            binding.tvDailyAmount.text = "$0"
            binding.tvDailyBudgetInfo.text = getString(R.string.dashboard_configure_hint)
            binding.btnConfigureBudget.visibility = View.VISIBLE
        }
    }

    private fun updateGastosUI() {
        if (gastos.isEmpty()) {
            binding.rvLastExpenses.visibility = View.GONE
            binding.layoutEmptyExpenses.visibility = View.VISIBLE
        } else {
            binding.rvLastExpenses.visibility = View.VISIBLE
            binding.layoutEmptyExpenses.visibility = View.GONE
            // Mostrar solo los últimos 3 gastos
            gastoAdapter.updateGastos(gastos.take(3))
        }
    }

    private fun updateBalanceUI() {
        val totalIngresos = ingresos.sumOf { it.monto }
        val totalGastos = gastos.sumOf { it.monto }
        val balance = totalIngresos - totalGastos

        if (ingresos.isNotEmpty() || gastos.isNotEmpty()) {
            binding.layoutBalance.visibility = View.VISIBLE
            binding.tvTotalIncome.text = formatCurrency(totalIngresos)
            binding.tvTotalExpenses.text = formatCurrency(totalGastos)
            binding.tvBalance.text = formatCurrency(balance)
        } else {
            binding.layoutBalance.visibility = View.GONE
        }
    }

    private fun updateProgressUI() {
        val presupuestoDiario = if (presupuestoMensual > 0) (presupuestoMensual / 30).toInt() else 0
        
        if (presupuestoDiario > 0) {
            binding.layoutProgress.visibility = View.VISIBLE
            
            val gastoHoy = calcularGastoHoy()
            val porcentaje = minOf(((gastoHoy.toFloat() / presupuestoDiario) * 100).toInt(), 100)
            
            binding.progressBar.progress = porcentaje
            binding.tvProgressPercent.text = getString(R.string.dashboard_percent_used, porcentaje)
            
            // Cambiar color según el porcentaje
            val color = when {
                porcentaje > 100 -> ContextCompat.getColor(this, R.color.red_500)
                porcentaje > 80 -> ContextCompat.getColor(this, R.color.category_home) // Amarillo
                else -> ContextCompat.getColor(this, R.color.accent_teal)
            }
            binding.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(color)
            binding.tvProgressPercent.setTextColor(color)
        } else {
            binding.layoutProgress.visibility = View.GONE
        }
    }

    private fun updateChartData() {
        if (gastos.isEmpty()) {
            binding.lineChart.visibility = View.GONE
            binding.layoutEmptyChart.visibility = View.VISIBLE
            return
        }

        binding.lineChart.visibility = View.VISIBLE
        binding.layoutEmptyChart.visibility = View.GONE

        // Obtener datos de los últimos 7 días
        val chartData = getLast7DaysData()
        
        // Crear entries para el gráfico
        val entries = chartData.mapIndexed { index, data ->
            Entry(index.toFloat(), data.gasto)
        }

        // Configurar el dataset
        val dataSet = LineDataSet(entries, "Gastos").apply {
            color = ContextCompat.getColor(this@DashboardActivity, R.color.primary_dark)
            lineWidth = 3f
            setDrawCircles(true)
            circleRadius = 4f
            setCircleColor(ContextCompat.getColor(this@DashboardActivity, R.color.primary_dark))
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(false)
        }

        // Configurar labels del eje X
        val labels = chartData.map { it.dia }
        binding.lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

        // Aplicar datos al gráfico
        binding.lineChart.data = LineData(dataSet)
        binding.lineChart.invalidate()
    }

    private fun getLast7DaysData(): List<ChartData> {
        val diasNombres = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
        val resultado = mutableListOf<ChartData>()
        val hoy = Calendar.getInstance()

        for (i in 6 downTo 0) {
            val dia = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val nombreDia = diasNombres[dia.get(Calendar.DAY_OF_WEEK) - 1]
            
            // Calcular total de gastos del día
            val totalGasto = gastos.filter { gasto ->
                val gastoCalendar = Calendar.getInstance().apply {
                    time = gasto.fecha
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                gastoCalendar.timeInMillis == dia.timeInMillis
            }.sumOf { it.monto }

            resultado.add(ChartData(nombreDia, totalGasto.toFloat()))
        }

        return resultado
    }

    private fun calcularGastoHoy(): Int {
        val hoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return gastos.filter { gasto ->
            val gastoCalendar = Calendar.getInstance().apply {
                time = gasto.fecha
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            gastoCalendar.timeInMillis == hoy.timeInMillis
        }.sumOf { it.monto }.toInt()
    }

    private fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount)
            .replace("BOB", "Bs")
            .replace("Bs.", "Bs")
            .replace("\u00A0", " ")
            .trim()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToConfigureBudget() {
        // TODO: Implementar navegación a ConfigurarPresupuestoInicialActivity
        showToast("Configurar presupuesto (próximamente)")
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
