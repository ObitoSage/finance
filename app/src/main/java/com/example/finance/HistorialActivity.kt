package com.example.finance

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.finance.databinding.ActivityHistorialBinding
import com.example.finance.databinding.ItemTransaccionBinding
import com.example.finance.dataClass.Transaccion
import com.example.finance.dataBase.EntityMappers.toGastosDomain
import com.example.finance.dataBase.EntityMappers.toIngresosDomain
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistorialActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistorialBinding
    private val auth = FirebaseAuth.getInstance()
    
    private var todasTransacciones = listOf<Transaccion>()
    private var transaccionesFiltradas = listOf<Transaccion>()
    private var tipoFiltro = "todos" // "todos", "gastos", "ingresos"
    private var categoriaFiltro: String? = null
    private var busqueda = ""
    
    private val categoryIcons = mapOf(
        // Iconos de gastos
        "Comida afuera" to R.drawable.ic_utensils,
        "Transporte" to R.drawable.ic_car,
        "Café" to R.drawable.ic_coffee,
        "Mercado" to R.drawable.ic_shopping_bag,
        "Hogar" to R.drawable.ic_home,
        "Entretenimiento" to R.drawable.ic_heart,
        "Servicios" to R.drawable.ic_zap,
        "Celular" to R.drawable.ic_smartphone,
        // Iconos de ingresos
        "Salario" to R.drawable.ic_briefcase,
        "Freelance" to R.drawable.ic_trending_up,
        "Bonificación" to R.drawable.ic_gift,
        "Venta" to R.drawable.ic_tag,
        "Otro" to R.drawable.ic_dollar_sign
    )
    
    private val categoryColors = mapOf(
        "Comida afuera" to "#EF4444",
        "Transporte" to "#3B82F6",
        "Café" to "#8B4513",
        "Mercado" to "#10B981",
        "Hogar" to "#F59E0B",
        "Entretenimiento" to "#EC4899",
        "Servicios" to "#6366F1",
        "Celular" to "#14B8A6",
        "Salario" to "#10B981",
        "Freelance" to "#14B8A6",
        "Bonificación" to "#F59E0B",
        "Venta" to "#8B5CF6",
        "Otro" to "#6B7280"
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorialBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        configurarVistas()
        cargarTransacciones()
    }
    
    override fun onResume() {
        super.onResume()
        // Recargar transacciones cuando vuelve a la pantalla
        cargarTransacciones()
    }
    
    private fun configurarVistas() {
        // Botón volver
        binding.btnVolver.setOnClickListener {
            finish()
        }
        
        // Filtros de tipo
        binding.btnTodos.setOnClickListener {
            seleccionarTipoFiltro("todos")
        }
        binding.btnGastos.setOnClickListener {
            seleccionarTipoFiltro("gastos")
        }
        binding.btnIngresos.setOnClickListener {
            seleccionarTipoFiltro("ingresos")
        }
        
        // Búsqueda
        binding.inputBusqueda.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                busqueda = s.toString()
                aplicarFiltros()
            }
        })
        
        // Filtro por categoría
        binding.btnFiltroCategoria.setOnClickListener {
            toggleFiltroCategoria()
        }
    }
    
    private fun seleccionarTipoFiltro(tipo: String) {
        tipoFiltro = tipo
        
        // Actualizar estilos de botones
        binding.btnTodos.backgroundTintList = ContextCompat.getColorStateList(
            this, if (tipo == "todos") R.color.primary_dark else R.color.primary_light
        )
        binding.btnTodos.setTextColor(
            ContextCompat.getColor(this, if (tipo == "todos") android.R.color.white else R.color.primary_dark)
        )
        
        binding.btnGastos.backgroundTintList = ContextCompat.getColorStateList(
            this, if (tipo == "gastos") R.color.primary_dark else R.color.primary_light
        )
        binding.btnGastos.setTextColor(
            ContextCompat.getColor(this, if (tipo == "gastos") android.R.color.white else R.color.primary_dark)
        )
        
        binding.btnIngresos.backgroundTintList = ContextCompat.getColorStateList(
            this, if (tipo == "ingresos") R.color.primary_dark else R.color.primary_light
        )
        binding.btnIngresos.setTextColor(
            ContextCompat.getColor(this, if (tipo == "ingresos") android.R.color.white else R.color.primary_dark)
        )
        
        aplicarFiltros()
    }
    
    private fun toggleFiltroCategoria() {
        if (binding.cardCategorias.visibility == View.VISIBLE) {
            binding.cardCategorias.visibility = View.GONE
        } else {
            mostrarCategorias()
            binding.cardCategorias.visibility = View.VISIBLE
        }
    }
    
    private fun mostrarCategorias() {
        binding.listaCategorias.removeAllViews()
        
        // Obtener todas las categorías únicas
        val categorias = todasTransacciones.map { it.categoria }.distinct().sorted()
        
        // Agregar opción "Todas las categorías"
        agregarOpcionCategoria("Todas las categorías", null)
        
        // Agregar cada categoría
        categorias.forEach { categoria ->
            agregarOpcionCategoria(categoria, categoria)
        }
    }
    
    private fun agregarOpcionCategoria(texto: String, categoria: String?) {
        val itemView = LayoutInflater.from(this).inflate(
            android.R.layout.simple_list_item_1, binding.listaCategorias, false
        ) as TextView
        
        itemView.text = texto
        itemView.textSize = 17f
        itemView.setPadding(32, 24, 32, 24)
        
        // Aplicar estilo si está seleccionado
        val isSeleccionado = categoriaFiltro == categoria
        itemView.setBackgroundColor(
            ContextCompat.getColor(this, if (isSeleccionado) android.R.color.white else android.R.color.transparent)
        )
        itemView.setTypeface(null, if (isSeleccionado) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        
        // Agregar esquinas redondeadas
        val drawable = GradientDrawable()
        drawable.cornerRadius = 32f
        drawable.setColor(
            ContextCompat.getColor(this, if (isSeleccionado) android.R.color.white else android.R.color.transparent)
        )
        itemView.background = drawable
        
        itemView.setOnClickListener {
            categoriaFiltro = categoria
            binding.textoFiltroCategoria.text = categoria ?: "Filtrar por categoría"
            binding.cardCategorias.visibility = View.GONE
            aplicarFiltros()
        }
        
        binding.listaCategorias.addView(itemView)
    }
    
    private fun cargarTransacciones() {
        val userId = auth.currentUser?.uid ?: return
        val app = application as FinanceApplication
        val repository = app.repository
        
        lifecycleScope.launch {
            try {
                // Cargar gastos desde Room Database
                val gastosEntity = repository.getAllGastosList(userId)
                val gastos = gastosEntity.toGastosDomain()
                
                // Cargar ingresos desde Room Database
                val ingresosEntity = repository.getAllIngresosList(userId)
                val ingresos = ingresosEntity.toIngresosDomain()
                
                // Convertir a transacciones
                val transaccionesGastos = gastos.map { gasto ->
                    Transaccion(
                        id = gasto.id.toString(),
                        tipo = "gasto",
                        categoria = gasto.categoria,
                        descripcion = gasto.descripcion,
                        monto = gasto.monto,
                        fecha = gasto.fecha
                    )
                }
                
                val transaccionesIngresos = ingresos.map { ingreso ->
                    Transaccion(
                        id = ingreso.id.toString(),
                        tipo = "ingreso",
                        categoria = ingreso.categoria,
                        descripcion = ingreso.descripcion,
                        monto = ingreso.monto,
                        fecha = ingreso.fecha
                    )
                }
                
                // Combinar y ordenar
                todasTransacciones = (transaccionesGastos + transaccionesIngresos)
                    .sortedByDescending { it.fecha }
                
                aplicarFiltros()
                actualizarResumen(
                    gastos.sumOf { it.monto }, 
                    ingresos.sumOf { it.monto }
                )
            } catch (e: Exception) {
                todasTransacciones = emptyList()
                aplicarFiltros()
                actualizarResumen(0.0, 0.0)
            }
        }
    }
    
    private fun aplicarFiltros() {
        transaccionesFiltradas = todasTransacciones.filter { transaccion ->
            // Filtro por búsqueda
            val matchBusqueda = transaccion.descripcion.lowercase().contains(busqueda.lowercase())
            
            // Filtro por categoría
            val matchCategoria = categoriaFiltro == null || transaccion.categoria == categoriaFiltro
            
            // Filtro por tipo
            val matchTipo = when (tipoFiltro) {
                "gastos" -> transaccion.tipo == "gasto"
                "ingresos" -> transaccion.tipo == "ingreso"
                else -> true
            }
            
            matchBusqueda && matchCategoria && matchTipo
        }
        
        mostrarTransacciones()
    }
    
    private fun mostrarTransacciones() {
        binding.listaTransacciones.removeAllViews()
        
        if (transaccionesFiltradas.isEmpty()) {
            binding.estadoVacio.visibility = View.VISIBLE
            binding.listaTransacciones.visibility = View.GONE
            
            // Actualizar mensaje según filtros activos
            if (busqueda.isNotEmpty() || categoriaFiltro != null) {
                binding.textoVacio.text = "No se encontraron transacciones"
                binding.subtextoVacio.text = "Intenta cambiar los filtros"
            } else {
                binding.textoVacio.text = "Sin transacciones registradas"
                binding.subtextoVacio.text = "Comienza a registrar tus gastos e ingresos"
            }
            return
        }
        
        binding.estadoVacio.visibility = View.GONE
        binding.listaTransacciones.visibility = View.VISIBLE
        
        // Agrupar por fecha
        val transaccionesPorFecha = transaccionesFiltradas.groupBy { formatDate(it.fecha) }
        
        transaccionesPorFecha.forEach { (fecha, transacciones) ->
            agregarGrupoFecha(fecha, transacciones)
        }
    }
    
    private fun agregarGrupoFecha(fecha: String, transacciones: List<Transaccion>) {
        // Calcular total del día
        val totalDia = transacciones.sumOf { 
            if (it.tipo == "ingreso") it.monto else -it.monto 
        }
        
        // Header de fecha
        val headerLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 24
                bottomMargin = 12
            }
            orientation = LinearLayout.HORIZONTAL
        }
        
        val fechaText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            text = fecha.capitalize(Locale.getDefault())
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.gray_500))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        val totalText = TextView(this).apply {
            text = "${if (totalDia >= 0) "+" else ""}${formatCurrency(totalDia)}"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, if (totalDia >= 0) R.color.green_500 else R.color.error_red))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        
        headerLayout.addView(fechaText)
        headerLayout.addView(totalText)
        binding.listaTransacciones.addView(headerLayout)
        
        // Agregar transacciones
        transacciones.forEach { transaccion ->
            agregarItemTransaccion(transaccion)
        }
    }
    
    private fun agregarItemTransaccion(transaccion: Transaccion) {
        val itemBinding = ItemTransaccionBinding.inflate(LayoutInflater.from(this))
        
        // Configurar icono
        val iconoResId = categoryIcons[transaccion.categoria] ?: R.drawable.ic_shopping_bag
        val colorHex = categoryColors[transaccion.categoria] ?: "#6B7280"
        
        itemBinding.iconoCategoria.setImageResource(iconoResId)
        
        // Aplicar color al fondo del icono (15% opacity)
        val colorBase = android.graphics.Color.parseColor(colorHex)
        val colorFondo = android.graphics.Color.argb(
            38, // 15% de 255
            android.graphics.Color.red(colorBase),
            android.graphics.Color.green(colorBase),
            android.graphics.Color.blue(colorBase)
        )
        
        val drawable = GradientDrawable()
        drawable.cornerRadius = 32f
        drawable.setColor(colorFondo)
        itemBinding.iconoFondo.background = drawable
        
        // Aplicar color al icono
        itemBinding.iconoCategoria.setColorFilter(colorBase)
        
        // Configurar indicador de tipo
        val isIngreso = transaccion.tipo == "ingreso"
        itemBinding.iconoTipo.setImageResource(
            if (isIngreso) R.drawable.ic_trending_up else R.drawable.ic_trending_down
        )
        itemBinding.iconoTipo.setColorFilter(
            ContextCompat.getColor(this, if (isIngreso) R.color.green_500 else R.color.error_red)
        )
        
        // Configurar textos
        itemBinding.descripcionTransaccion.text = transaccion.descripcion
        itemBinding.horaTransaccion.text = formatTime(transaccion.fecha)
        itemBinding.categoriaTransaccion.text = transaccion.categoria
        
        // Configurar monto
        itemBinding.montoTransaccion.text = "${if (isIngreso) "+" else "-"}${formatCurrency(transaccion.monto)}"
        itemBinding.montoTransaccion.setTextColor(
            ContextCompat.getColor(this, if (isIngreso) R.color.green_500 else R.color.error_red)
        )
        
        binding.listaTransacciones.addView(itemBinding.root)
    }
    
    private fun actualizarResumen(totalGastos: Double, totalIngresos: Double) {
        val balance = totalIngresos - totalGastos
        
        binding.totalIngresos.text = formatCurrency(totalIngresos)
        binding.totalGastos.text = formatCurrency(totalGastos)
        binding.balance.text = formatCurrency(balance)
        
        // Actualizar color del balance
        binding.balance.setTextColor(
            ContextCompat.getColor(this, if (balance >= 0) R.color.green_500 else R.color.error_red)
        )
        
        // Mostrar/ocultar card de balance
        binding.cardBalance.visibility = if (totalIngresos > 0 || totalGastos > 0) View.VISIBLE else View.GONE
    }
    
    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        format.maximumFractionDigits = 0
        return format.format(amount).replace("COP", "$").trim()
    }
    
    private fun formatDate(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        val weekday = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "lunes"
            Calendar.TUESDAY -> "martes"
            Calendar.WEDNESDAY -> "miércoles"
            Calendar.THURSDAY -> "jueves"
            Calendar.FRIDAY -> "viernes"
            Calendar.SATURDAY -> "sábado"
            Calendar.SUNDAY -> "domingo"
            else -> ""
        }
        
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        val month = when (calendar.get(Calendar.MONTH)) {
            Calendar.JANUARY -> "enero"
            Calendar.FEBRUARY -> "febrero"
            Calendar.MARCH -> "marzo"
            Calendar.APRIL -> "abril"
            Calendar.MAY -> "mayo"
            Calendar.JUNE -> "junio"
            Calendar.JULY -> "julio"
            Calendar.AUGUST -> "agosto"
            Calendar.SEPTEMBER -> "septiembre"
            Calendar.OCTOBER -> "octubre"
            Calendar.NOVEMBER -> "noviembre"
            Calendar.DECEMBER -> "diciembre"
            else -> ""
        }
        
        return "$weekday, $day de $month"
    }
    
    private fun formatTime(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        return String.format("%02d:%02d", hour, minute)
    }
}
