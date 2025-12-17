package com.example.finance

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finance.adapters.GastoAdapter
import com.example.finance.databinding.ActivityDashboardBinding
import com.example.finance.dataClass.ChartData
import com.example.finance.dataClass.Gasto
import com.example.finance.dataClass.Ingreso
import com.example.finance.dataBase.EntityMappers.toDomain
import com.example.finance.dataBase.EntityMappers.toGastosDomain
import com.example.finance.dataBase.EntityMappers.toIngresosDomain
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var auth: FirebaseAuth
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
        
        // Verificar si viene de editar perfil
        checkProfileUpdateMessage()
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos cuando vuelve a la pantalla
        loadUserData()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkProfileUpdateMessage()
    }
    
    private fun checkProfileUpdateMessage() {
        if (intent.getBooleanExtra("PROFILE_UPDATED", false)) {
            showToast("Los cambios se guardaron correctamente")
            // Limpiar el extra para que no se muestre de nuevo
            intent.removeExtra("PROFILE_UPDATED")
        }
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
        
        // Botón editar presupuesto
        binding.btnEditBudget.setOnClickListener {
            navigateToConfigureBudget()
        }

        // Botón agregar gasto
        binding.btnAddExpense.setOnClickListener {
            startActivity(Intent(this, RegistrarGastoActivity::class.java))
        }

        // Botón agregar ingreso
        binding.btnAddIncome.setOnClickListener {
            startActivity(Intent(this, RegistrarIngresoActivity::class.java))
        }

        // Accesos rápidos
        binding.btnCategories.setOnClickListener {
            startActivity(Intent(this, CategoriasActivity::class.java))
        }

        binding.btnGoals.setOnClickListener {
            startActivity(Intent(this, MetasActivity::class.java))
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
        }

        binding.btnReport.setOnClickListener {
            val intent = Intent(this, ReporteActivity::class.java)
            startActivity(intent)
        }

        binding.btnProfile.setOnClickListener {
            navigateToProfile()
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return
        
        // Recargar usuario desde Firebase para obtener cambios recientes
        currentUser.reload().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = currentUser.uid

                // Cargar nombre del usuario
                userName = currentUser.displayName 
                    ?: currentUser.email?.substringBefore("@") 
                    ?: "Usuario"
                
                val firstName = userName.split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Usuario"
                binding.tvGreeting.text = getString(R.string.dashboard_greeting, firstName)

                // Cargar presupuesto mensual desde Firestore
                loadBudget(userId)
                
                // Cargar gastos e ingresos desde Room Database
                loadGastosFromRoom(userId)
                loadIngresosFromRoom(userId)
            }
        }
    }

    private fun loadBudget(userId: String) {
        val app = application as FinanceApplication
        val repository = app.repository

        lifecycleScope.launch {
            try {
                presupuestoMensual = repository.getPresupuestoMensual(userId)
                updateBudgetUI()
            } catch (e: Exception) {
                presupuestoMensual = 0.0
                updateBudgetUI()
            }
        }
    }

    /**
     * Carga los gastos del mes actual desde Room Database.
     * Usa Flow para recibir actualizaciones automáticas cuando cambian los datos.
     */
    private fun loadGastosFromRoom(userId: String) {
        val app = application as FinanceApplication
        val repository = app.repository

        // Calcular inicio del mes
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis

        lifecycleScope.launch {
            try {
                // Usar repeatOnLifecycle para evitar "Job was cancelled"
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    // Obtener gastos del mes actual
                    val endOfMonth = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                    }.timeInMillis

                    repository.getGastosByDateRange(userId, startOfMonth, endOfMonth)
                        .collect { gastosEntity ->
                            // Convertir entities a domain models
                            gastos.clear()
                            gastos.addAll(gastosEntity.toGastosDomain())
                            
                            // Actualizar UI
                            updateGastosUI()
                            updateChartData()
                            updateBalanceUI()
                            updateProgressUI()
                        }
                }
            } catch (e: Exception) {
                // Solo mostrar error si no es por cancelación
                if (e !is kotlinx.coroutines.CancellationException) {
                    showToast("Error al cargar gastos: ${e.message}")
                }
                gastos.clear()
                updateGastosUI()
                updateChartData()
                updateBalanceUI()
                updateProgressUI()
            }
        }
    }

    /**
     * Carga los ingresos del mes actual desde Room Database.
     * Usa Flow para recibir actualizaciones automáticas cuando cambian los datos.
     */
    private fun loadIngresosFromRoom(userId: String) {
        val app = application as FinanceApplication
        val repository = app.repository

        // Calcular inicio del mes
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis

        lifecycleScope.launch {
            try {
                // Usar repeatOnLifecycle para evitar "Job was cancelled"
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    // Obtener ingresos del mes actual
                    val endOfMonth = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                    }.timeInMillis

                    repository.getIngresosByDateRange(userId, startOfMonth, endOfMonth)
                        .collect { ingresosEntity ->
                            // Convertir entities a domain models
                            ingresos.clear()
                            ingresos.addAll(ingresosEntity.toIngresosDomain())
                            
                            // Actualizar UI
                            updateBalanceUI()
                        }
                }
            } catch (e: Exception) {
                // Solo mostrar error si no es por cancelación
                if (e !is kotlinx.coroutines.CancellationException) {
                    showToast("Error al cargar ingresos: ${e.message}")
                }
                ingresos.clear()
                updateBalanceUI()
            }
        }
    }

    private fun updateBudgetUI() {
        // Calcular presupuesto diario usando los días REALES del mes actual (igual que en ConfigurarPresupuestoInicialActivity)
        val calendar = Calendar.getInstance()
        val diasDelMes = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val presupuestoDiario = if (presupuestoMensual > 0) (presupuestoMensual / diasDelMes).toInt() else 0
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
        // Calcular presupuesto diario usando los días REALES del mes actual
        val calendar = Calendar.getInstance()
        val diasDelMes = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val presupuestoDiario = if (presupuestoMensual > 0) (presupuestoMensual / diasDelMes).toInt() else 0
        
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
        val intent = Intent(this, ConfigurarPresupuestoInicialActivity::class.java)
        startActivity(intent)
    }
    
    private fun navigateToProfile() {
        val intent = Intent(this, UsuarioActivity::class.java)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
