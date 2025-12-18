package com.example.finance

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finance.databinding.ActivityRegistrarGastoBinding
import com.example.finance.dataBase.entities.GastoEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class RegistrarGastoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrarGastoBinding
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
        binding = ActivityRegistrarGastoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCategorias()
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnGuardar.setOnClickListener {
            guardarGasto()
        }

        // Habilitar/deshabilitar botón guardar según los campos
        binding.etMonto.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateUI()
            }
        })

        binding.etNota.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateUI()
            }
        })
    }

    private fun setupCategorias() {
        categorias.forEach { (nombre, emoji) ->
            val button = Button(this).apply {
                text = "$emoji\n$nombre"
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(8, 16, 8, 16)
                setBackgroundResource(R.drawable.bg_categoria_default)
                setTextColor(Color.parseColor("#212842"))
                
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
                layoutParams = params

                setOnClickListener {
                    seleccionarCategoria(nombre)
                }
            }
            binding.gridCategorias.addView(button)
        }
    }

    private fun seleccionarCategoria(categoria: String) {
        categoriaSeleccionada = categoria
        
        // Actualizar todos los botones de categoría
        for (i in 0 until binding.gridCategorias.childCount) {
            val button = binding.gridCategorias.getChildAt(i) as Button
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

    private fun updateUI() {
        // Habilitar botón guardar si hay categoría y monto válido
        val montoValido = (binding.etMonto.text.toString().toDoubleOrNull() ?: 0.0) > 0
        binding.btnGuardar.isEnabled = categoriaSeleccionada.isNotEmpty() && montoValido
        binding.btnGuardar.alpha = if (binding.btnGuardar.isEnabled) 1.0f else 0.5f
    }

    private fun guardarGasto() {
        // Obtener el userId de Firebase Auth
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val monto = binding.etMonto.text.toString().toDoubleOrNull() ?: 0.0
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
            descripcion = binding.etNota.text.toString().ifEmpty { categoriaSeleccionada },
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
