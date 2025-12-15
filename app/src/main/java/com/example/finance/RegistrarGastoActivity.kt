package com.example.finance

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finance.dataBase.entities.GastoEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*


class RegistrarGastoActivity : AppCompatActivity() {

    // Views
    private lateinit var tvMonto: TextView
    private lateinit var gridCategorias: GridLayout
    private lateinit var gridTeclado: GridLayout
    private lateinit var etNota: EditText
    private lateinit var btnGuardar: Button
    private lateinit var btnBack: ImageButton

    // Variables
    private var montoActual = "0"
    private var categoriaSeleccionada = ""

    // Categorías disponibles con sus emojis
    private val categorias = listOf(
        Pair("Comida afuera", "🍽️"),
        Pair("Transporte", "🚗"),
        Pair("Mercado", "🛒"),
        Pair("Entretenimiento", "🎬"),
        Pair("Salud", "❤️"),
        Pair("Servicios", "⚡"),
        Pair("Ropa", "👕"),
        Pair("Café", "☕"),
        Pair("Transferencias", "↔️"),
        Pair("Otros", "⋯")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar_gasto)

        initViews()
        setupCategorias()
        setupTeclado()
        setupListeners()
        updateUI()
    }

    /**
     * Inicializa las referencias a las vistas.
     */
    private fun initViews() {
        tvMonto = findViewById(R.id.tvMonto)
        gridCategorias = findViewById(R.id.gridCategorias)
        gridTeclado = findViewById(R.id.gridTeclado)
        etNota = findViewById(R.id.etNota)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnBack = findViewById(R.id.btnBack)
    }

    /**
     * Configura los listeners de los botones.
     */
    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        
        btnGuardar.setOnClickListener {
            guardarGasto()
        }

        etNota.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateUI()
            }
        })
    }

    /**
     * Crea dinámicamente el grid de categorías.
     */
    private fun setupCategorias() {
        categorias.forEach { (nombre, emoji) ->
            val button = Button(this).apply {
                text = "$emoji\n$nombre"
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(8, 16, 8, 16)
                setBackgroundResource(R.drawable.bg_categoria_default)
                setTextColor(Color.parseColor("#212842"))
                
                // Parámetros del layout
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
                layoutParams = params

                // Click listener
                setOnClickListener {
                    seleccionarCategoria(nombre)
                }
            }
            gridCategorias.addView(button)
        }
    }

    /**
     * Selecciona una categoría y actualiza la UI.
     */
    private fun seleccionarCategoria(categoria: String) {
        categoriaSeleccionada = categoria
        
        // Actualizar todos los botones de categoría
        for (i in 0 until gridCategorias.childCount) {
            val button = gridCategorias.getChildAt(i) as Button
            val categoriaActual = categorias.getOrNull(i)?.first ?: ""
            
            if (categoriaActual == categoria) {
                button.setBackgroundResource(R.drawable.bg_categoria_selected)
                button.setTextColor(Color.WHITE)
            } else {
                button.setBackgroundResource(R.drawable.bg_categoria_default)
                button.setTextColor(Color.parseColor("#212842"))
            }
        }
        
        updateUI()
    }

    /**
     * Crea dinámicamente el teclado numérico.
     */
    private fun setupTeclado() {
        val teclas = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "←")
        
        teclas.forEach { tecla ->
            val button = Button(this).apply {
                text = tecla
                textSize = 24f
                setBackgroundResource(R.drawable.bg_input_field)
                setTextColor(Color.parseColor("#212842"))
                
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 140
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
                layoutParams = params

                setOnClickListener {
                    when (tecla) {
                        "←" -> handleDelete()
                        else -> handleNumberClick(tecla)
                    }
                }
            }
            gridTeclado.addView(button)
        }
    }

    /**
     * Maneja el click en un número o punto decimal.
     */
    private fun handleNumberClick(num: String) {
        montoActual = if (montoActual == "0") {
            num
        } else {
            montoActual + num
        }
        updateUI()
    }

    /**
     * Maneja el botón de borrar.
     */
    private fun handleDelete() {
        montoActual = if (montoActual.length > 1) {
            montoActual.dropLast(1)
        } else {
            "0"
        }
        updateUI()
    }

    /**
     * Actualiza la UI según el estado actual.
     */
    private fun updateUI() {
        // Formatear el monto con separadores de miles
        val montoFormateado = try {
            val numero = montoActual.toDoubleOrNull() ?: 0.0
            NumberFormat.getNumberInstance(Locale("es", "CO")).format(numero)
        } catch (e: Exception) {
            montoActual
        }
        
        tvMonto.text = "$ $montoFormateado"
        
        // Habilitar botón guardar si hay categoría y monto válido
        val montoValido = (montoActual.toDoubleOrNull() ?: 0.0) > 0
        btnGuardar.isEnabled = categoriaSeleccionada.isNotEmpty() && montoValido
        btnGuardar.alpha = if (btnGuardar.isEnabled) 1.0f else 0.5f
    }

    /**
     * Guarda el gasto en Room Database.
     */
    private fun guardarGasto() {
        // Obtener el userId de Firebase Auth
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val monto = montoActual.toDoubleOrNull() ?: 0.0
        if (monto <= 0) {
            Toast.makeText(this, "Por favor ingresa un monto válido", Toast.LENGTH_SHORT).show()
            return
        }

        if (categoriaSeleccionada.isEmpty()) {
            Toast.makeText(this, "Por favor selecciona una categoría", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear el objeto GastoEntity
        val gasto = GastoEntity(
            categoria = categoriaSeleccionada,
            descripcion = etNota.text.toString().ifEmpty { categoriaSeleccionada },
            monto = monto,
            fecha = System.currentTimeMillis(),
            userId = userId
        )

        // Guardar en Room Database usando coroutines
        lifecycleScope.launch {
            try {
                val app = application as FinanceApplication
                val gastoId = app.repository.insertGasto(gasto)
                
                Toast.makeText(
                    this@RegistrarGastoActivity,
                    "Gasto guardado correctamente (ID: $gastoId)",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Cerrar la activity
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@RegistrarGastoActivity,
                    "Error al guardar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
