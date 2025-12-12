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
import com.example.finance.dataBase.entities.IngresoEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

/**
 * Activity para registrar un nuevo ingreso.
 * 
 * Funcionalidades:
 * - Campo de entrada para el monto con formato
 * - Selección de categoría mediante grid
 * - Campo opcional para descripción
 * - Guarda el ingreso en Room Database vinculado al usuario de Firebase Auth
 * - Valida que haya categoría y monto antes de guardar
 */
class RegistrarIngresoActivity : AppCompatActivity() {

    // Views
    private lateinit var etMonto: EditText
    private lateinit var gridCategorias: GridLayout
    private lateinit var etDescripcion: EditText
    private lateinit var btnGuardar: Button
    private lateinit var btnBack: ImageButton

    // Variables
    private var categoriaSeleccionada = ""
    private var montoSinFormato = ""

    // Categorías disponibles con sus emojis
    private val categorias = listOf(
        Pair("salario", Pair("Salario", "💼")),
        Pair("freelance", Pair("Freelance", "📈")),
        Pair("bonificacion", Pair("Bonificación", "🎁")),
        Pair("venta", Pair("Venta", "🏷️")),
        Pair("otro", Pair("Otro", "💵"))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar_ingreso)

        initViews()
        setupCategorias()
        setupListeners()
        updateUI()
    }

    /**
     * Inicializa las referencias a las vistas.
     */
    private fun initViews() {
        etMonto = findViewById(R.id.etMonto)
        gridCategorias = findViewById(R.id.gridCategorias)
        etDescripcion = findViewById(R.id.etDescripcion)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnBack = findViewById(R.id.btnBack)
    }

    /**
     * Configura los listeners de los elementos.
     */
    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        
        btnGuardar.setOnClickListener {
            guardarIngreso()
        }

        // TextWatcher para formatear el monto mientras se escribe
        etMonto.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                etMonto.removeTextChangedListener(this)
                
                try {
                    val original = s.toString()
                    val cleanString = original.replace("[^\\d]".toRegex(), "")
                    
                    if (cleanString.isNotEmpty()) {
                        montoSinFormato = cleanString
                        val formatted = formatMonto(cleanString)
                        etMonto.setText(formatted)
                        etMonto.setSelection(formatted.length)
                    } else {
                        montoSinFormato = ""
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                etMonto.addTextChangedListener(this)
                updateUI()
            }
        })

        etDescripcion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateUI()
            }
        })
    }

    /**
     * Formatea el monto con separadores de miles.
     */
    private fun formatMonto(value: String): String {
        return try {
            val numero = value.toLongOrNull() ?: 0L
            NumberFormat.getNumberInstance(Locale("es", "CO")).format(numero)
        } catch (e: Exception) {
            value
        }
    }

    /**
     * Crea dinámicamente el grid de categorías.
     */
    private fun setupCategorias() {
        categorias.forEach { (id, categoriaData) ->
            val (nombre, emoji) = categoriaData
            
            val button = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(16, 16, 16, 16)
                setBackgroundResource(R.drawable.bg_categoria_default)
                
                // Parámetros del layout
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
                layoutParams = params

                // Icono
                val iconContainer = TextView(this@RegistrarIngresoActivity).apply {
                    text = emoji
                    textSize = 20f
                    gravity = Gravity.CENTER
                    setBackgroundResource(R.drawable.bg_icon_circle)
                    setPadding(12, 12, 12, 12)
                }
                addView(iconContainer)

                // Texto
                val textView = TextView(this@RegistrarIngresoActivity).apply {
                    text = nombre
                    textSize = 17f
                    setTextColor(Color.parseColor("#212842"))
                    setPadding(12, 0, 0, 0)
                }
                addView(textView)

                // Click listener
                setOnClickListener {
                    seleccionarCategoria(id)
                }
            }
            
            gridCategorias.addView(button)
        }
    }

    /**
     * Selecciona una categoría y actualiza la UI.
     */
    private fun seleccionarCategoria(categoriaId: String) {
        categoriaSeleccionada = categoriaId
        
        // Actualizar todos los botones de categoría
        for (i in 0 until gridCategorias.childCount) {
            val layout = gridCategorias.getChildAt(i) as LinearLayout
            val currentCategoriaId = categorias.getOrNull(i)?.first ?: ""
            
            if (currentCategoriaId == categoriaId) {
                layout.setBackgroundResource(R.drawable.bg_categoria_selected)
                // Cambiar color del texto
                val textView = layout.getChildAt(1) as TextView
                textView.setTextColor(Color.WHITE)
            } else {
                layout.setBackgroundResource(R.drawable.bg_categoria_default)
                val textView = layout.getChildAt(1) as TextView
                textView.setTextColor(Color.parseColor("#212842"))
            }
        }
        
        updateUI()
    }

    /**
     * Actualiza la UI según el estado actual.
     */
    private fun updateUI() {
        // Habilitar botón guardar si hay categoría y monto válido
        val montoValido = (montoSinFormato.toLongOrNull() ?: 0L) > 0
        btnGuardar.isEnabled = categoriaSeleccionada.isNotEmpty() && montoValido
        btnGuardar.alpha = if (btnGuardar.isEnabled) 1.0f else 0.5f
    }

    /**
     * Guarda el ingreso en Room Database.
     */
    private fun guardarIngreso() {
        // Obtener el userId de Firebase Auth
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val monto = montoSinFormato.toDoubleOrNull() ?: 0.0
        if (monto <= 0) {
            Toast.makeText(this, "Por favor ingresa un monto válido", Toast.LENGTH_SHORT).show()
            return
        }

        if (categoriaSeleccionada.isEmpty()) {
            Toast.makeText(this, "Por favor selecciona una categoría", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener el nombre de la categoría
        val categoriaNombre = categorias.find { it.first == categoriaSeleccionada }?.second?.first 
            ?: categoriaSeleccionada

        // Crear el objeto IngresoEntity
        val ingreso = IngresoEntity(
            categoria = categoriaNombre,
            descripcion = etDescripcion.text.toString().ifEmpty { "Sin descripción" },
            monto = monto,
            fecha = System.currentTimeMillis(),
            userId = userId
        )

        // Guardar en Room Database usando coroutines
        lifecycleScope.launch {
            try {
                val app = application as FinanceApplication
                val ingresoId = app.repository.insertIngreso(ingreso)
                
                Toast.makeText(
                    this@RegistrarIngresoActivity,
                    "Ingreso guardado correctamente (ID: $ingresoId)",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Cerrar la activity
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@RegistrarIngresoActivity,
                    "Error al guardar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
