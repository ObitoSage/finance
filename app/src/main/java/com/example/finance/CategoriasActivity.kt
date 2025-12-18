package com.example.finance

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.finance.databinding.ActivityCategoriasBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class CategoriasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriasBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Formato de moneda
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    data class Categoria(
        val nombre: String,
        val icono: Int,
        val gastado: Double,
        val presupuesto: Double,
        val colorFondo: String,
        val colorIcono: String,
        val colorBarra: String
    )

    // Mapeo de categorías con sus iconos y colores
    private val categoriaConfig = mapOf(
        "Comida afuera" to Triple(R.drawable.ic_utensils, "#FFE5E5", "#EF4444"),
        "Transporte" to Triple(R.drawable.ic_car, "#E0F2FE", "#3B82F6"),
        "Café" to Triple(R.drawable.ic_coffee, "#F5E6D3", "#8B4513"),
        "Mercado" to Triple(R.drawable.ic_shopping_bag, "#D1FAE5", "#10B981"),
        "Hogar" to Triple(R.drawable.ic_home, "#FFEDD5", "#F59E0B"),
        "Entretenimiento" to Triple(R.drawable.ic_heart, "#FCE7F3", "#EC4899"),
        "Servicios" to Triple(R.drawable.ic_zap, "#EDE9FE", "#6366F1"),
        "Celular" to Triple(R.drawable.ic_smartphone, "#CCFBF1", "#14B8A6"),
        "Salud" to Triple(R.drawable.ic_heart, "#FCE7F3", "#F472B6"),
        "Educación" to Triple(R.drawable.ic_book, "#EDE9FE", "#8B5CF6"),
        "Ropa" to Triple(R.drawable.ic_shopping_bag, "#FCE7F3", "#EC4899"),
        "Otros" to Triple(R.drawable.ic_more_horizontal, "#F3F4F6", "#6B7280")
    )

    // Presupuestos por defecto (en pesos colombianos)
    private val presupuestosPorDefecto = mapOf(
        "Comida afuera" to 800000.0,
        "Transporte" to 600000.0,
        "Café" to 200000.0,
        "Mercado" to 1000000.0,
        "Hogar" to 500000.0,
        "Entretenimiento" to 400000.0,
        "Servicios" to 700000.0,
        "Celular" to 100000.0,
        "Salud" to 500000.0,
        "Educación" to 600000.0,
        "Ropa" to 300000.0,
        "Otros" to 500000.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoriasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupClickListeners()
        cargarCategorias()
    }

    override fun onResume() {
        super.onResume()
        // Recargar categorías cuando se regresa a esta pantalla
        cargarCategorias()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnConfigurarPresupuesto.setOnClickListener {
            val intent = Intent(this, ConfigurarPresupuestoActivity::class.java)
            startActivity(intent)
        }
    }

    private fun cargarCategorias() {
        val userId = auth.currentUser?.uid ?: return
        val app = application as FinanceApplication
        val repository = app.repository

        // Cargar presupuestos guardados desde Firestore
        db.collection("users").document(userId)
            .collection("presupuestos")
            .document("categorias")
            .get()
            .addOnSuccessListener { documentPresupuestos ->
                lifecycleScope.launch {
                    procesarCategorias(userId, repository, documentPresupuestos)
                }
            }
            .addOnFailureListener {
                lifecycleScope.launch {
                    procesarCategorias(userId, repository, null)
                }
            }
    }

    private suspend fun procesarCategorias(
        userId: String,
        repository: com.example.finance.dataBase.repository.FinanceRepository,
        documentPresupuestos: com.google.firebase.firestore.DocumentSnapshot?
    ) {
        try {
            // Obtener todos los gastos del mes actual
            val calendario = Calendar.getInstance()
            val mesActual = calendario.get(Calendar.MONTH)
            val anioActual = calendario.get(Calendar.YEAR)

            val todosLosGastos = repository.getAllGastosList(userId)

            // Filtrar gastos del mes actual
            val gastosDelMes = todosLosGastos.filter { gasto ->
                val fechaGasto = Calendar.getInstance().apply {
                    timeInMillis = gasto.fecha
                }
                fechaGasto.get(Calendar.MONTH) == mesActual &&
                fechaGasto.get(Calendar.YEAR) == anioActual
            }

            // Agrupar gastos por categoría
            val gastosPorCategoria = gastosDelMes.groupBy { it.categoria }
                .mapValues { entry -> entry.value.sumOf { it.monto } }

            // Crear lista de categorías con datos reales
            val categorias = mutableListOf<Categoria>()

            // Agregar categorías que tienen gastos
            gastosPorCategoria.forEach { (nombreCategoria, gastado) ->
                val config = categoriaConfig[nombreCategoria] ?: categoriaConfig["Otros"]!!
                
                // Usar presupuesto guardado o por defecto
                val presupuesto = if (documentPresupuestos != null && documentPresupuestos.exists()) {
                    documentPresupuestos.getDouble(nombreCategoria) ?: presupuestosPorDefecto[nombreCategoria] ?: 500000.0
                } else {
                    presupuestosPorDefecto[nombreCategoria] ?: 500000.0
                }

                categorias.add(
                    Categoria(
                        nombre = nombreCategoria,
                        icono = config.first,
                        gastado = gastado,
                        presupuesto = presupuesto,
                        colorFondo = config.second,
                        colorIcono = config.third,
                        colorBarra = config.third
                    )
                )
            }

            // Agregar categorías principales que no tienen gastos (para mostrarlas vacías)
            val categoriasDefinidas = listOf(
                "Comida afuera", "Transporte", "Café", "Mercado", 
                "Hogar", "Entretenimiento", "Servicios", "Celular"
            )

            categoriasDefinidas.forEach { nombreCategoria ->
                if (!gastosPorCategoria.containsKey(nombreCategoria)) {
                    val config = categoriaConfig[nombreCategoria]!!
                    
                    // Usar presupuesto guardado o por defecto
                    val presupuesto = if (documentPresupuestos != null && documentPresupuestos.exists()) {
                        documentPresupuestos.getDouble(nombreCategoria) ?: presupuestosPorDefecto[nombreCategoria]!!
                    } else {
                        presupuestosPorDefecto[nombreCategoria]!!
                    }

                    categorias.add(
                        Categoria(
                            nombre = nombreCategoria,
                            icono = config.first,
                            gastado = 0.0,
                            presupuesto = presupuesto,
                            colorFondo = config.second,
                            colorIcono = config.third,
                            colorBarra = config.third
                        )
                    )
                }
            }

            // Ordenar por mayor gastado primero
            val categoriasOrdenadas = categorias.sortedByDescending { it.gastado }

            // Mostrar categorías
            mostrarCategorias(categoriasOrdenadas)

        } catch (e: Exception) {
            showToast("Error al cargar categorías: ${e.message}")
        }
    }

    private fun obtenerCategorias(): List<Categoria> {
        // Esta función ya no se usa, se reemplazó por cargarCategorias()
        return emptyList()
    }

    private fun mostrarCategorias(categorias: List<Categoria>) {
        binding.containerCategorias.removeAllViews()
        
        categorias.forEach { categoria ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_categoria, binding.containerCategorias, false)
            
            // Referencias a las vistas
            val iconContainer = itemView.findViewById<FrameLayout>(R.id.iconContainer)
            val ivCategoriaIcono = itemView.findViewById<ImageView>(R.id.ivCategoriaIcono)
            val tvCategoriaNombre = itemView.findViewById<TextView>(R.id.tvCategoriaNombre)
            val tvCategoriaMontos = itemView.findViewById<TextView>(R.id.tvCategoriaMontos)
            val progressBar = itemView.findViewById<View>(R.id.progressBar)
            val tvCategoriaRestante = itemView.findViewById<TextView>(R.id.tvCategoriaRestante)
            
            // Configurar icono
            ivCategoriaIcono.setImageResource(categoria.icono)
            ivCategoriaIcono.setColorFilter(Color.parseColor(categoria.colorIcono))
            
            // Configurar fondo del icono
            val iconBackground = GradientDrawable()
            iconBackground.shape = GradientDrawable.RECTANGLE
            iconBackground.cornerRadius = 12f * resources.displayMetrics.density
            iconBackground.setColor(Color.parseColor(categoria.colorFondo))
            iconContainer.background = iconBackground
            
            // Configurar textos
            tvCategoriaNombre.text = categoria.nombre
            tvCategoriaMontos.text = "${formatCurrency(categoria.gastado)} de ${formatCurrency(categoria.presupuesto)}"
            
            // Configurar barra de progreso
            val porcentaje = if (categoria.presupuesto > 0) {
                ((categoria.gastado / categoria.presupuesto) * 100).coerceAtMost(100.0)
            } else 0.0
            
            val progressBackground = GradientDrawable()
            progressBackground.shape = GradientDrawable.RECTANGLE
            progressBackground.cornerRadius = 4f * resources.displayMetrics.density
            progressBackground.setColor(Color.parseColor(categoria.colorBarra))
            progressBar.background = progressBackground
            
            // Configurar ancho de la barra inmediatamente
            progressBar.viewTreeObserver.addOnGlobalLayoutListener(
                object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        progressBar.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        val parent = progressBar.parent as View
                        val parentWidth = parent.width
                        val newWidth = (parentWidth * porcentaje / 100).toInt()
                        val params = progressBar.layoutParams
                        params.width = newWidth.coerceAtLeast(1) // Mínimo 1dp visible
                        progressBar.layoutParams = params
                    }
                }
            )
            
            // Configurar texto restante
            val restante = categoria.presupuesto - categoria.gastado
            val isAlerta = porcentaje > 85
            
            if (restante >= 0) {
                tvCategoriaRestante.text = "Quedan ${formatCurrency(restante)}"
            } else {
                tvCategoriaRestante.text = "Excediste por ${formatCurrency(Math.abs(restante))}"
            }
            
            tvCategoriaRestante.setTextColor(
                if (isAlerta) ContextCompat.getColor(this, R.color.red_500)
                else ContextCompat.getColor(this, R.color.gray_600)
            )
            
            if (isAlerta) {
                tvCategoriaRestante.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            
            binding.containerCategorias.addView(itemView)
        }
    }

    private fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount)
            .replace("COP", "$")
            .replace("\u00A0", "")
            .trim()
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
