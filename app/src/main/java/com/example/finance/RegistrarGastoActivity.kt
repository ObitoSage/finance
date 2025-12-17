package com.example.finance

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.finance.databinding.ActivityRegistrarGastoBinding
import com.example.finance.dataBase.entities.GastoEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class RegistrarGastoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrarGastoBinding
    private lateinit var auth: FirebaseAuth

    // Estado
    private var montoActual = "0"
    private var categoriaSeleccionada = ""

    // Formato de moneda
    @Suppress("DEPRECATION")
    private val numberFormat = NumberFormat.getNumberInstance(Locale("es", "CO"))

    // Categorías disponibles con sus emojis
    private val categorias = listOf(
        "Comida afuera" to "🍽️",
        "Transporte" to "🚗",
        "Mercado" to "🛒",
        "Entretenimiento" to "🎬",
        "Salud" to "❤️",
        "Servicios" to "⚡",
        "Ropa" to "👕",
        "Café" to "☕",
        "Transferencias" to "↔️",
        "Otros" to "⋯"
    )

    // Teclas del teclado numérico
    private val teclas = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "←")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrarGastoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupCategorias()
        setupTeclado()
        setupClickListeners()
        updateUI()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnGuardar.setOnClickListener { guardarGasto() }

        binding.etNota.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updateUI() }
        })
    }

    private fun setupCategorias() {
        categorias.forEach { (nombre, emoji) ->
            val button = createCategoriaButton(nombre, emoji)
            binding.gridCategorias.addView(button)
        }
    }

    private fun createCategoriaButton(nombre: String, emoji: String): Button {
        return Button(this).apply {
            text = "$emoji\n$nombre"
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(8, 16, 8, 16)
            setBackgroundResource(R.drawable.bg_categoria_default)
            setTextColor(ContextCompat.getColor(context, R.color.primary_dark))

            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }

            setOnClickListener { seleccionarCategoria(nombre) }
        }
    }

    private fun seleccionarCategoria(categoria: String) {
        categoriaSeleccionada = categoria
        
        // Actualizar estado visual de todos los botones
        for (i in 0 until binding.gridCategorias.childCount) {
            val button = binding.gridCategorias.getChildAt(i) as Button
            val isSelected = categorias.getOrNull(i)?.first == categoria

            button.setBackgroundResource(
                if (isSelected) R.drawable.bg_categoria_selected
                else R.drawable.bg_categoria_default
            )
            button.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (isSelected) android.R.color.white else R.color.primary_dark
                )
            )
        }
        
        updateUI()
    }

    private fun setupTeclado() {
        teclas.forEach { tecla ->
            val button = createTeclaButton(tecla)
            binding.gridTeclado.addView(button)
        }
    }

    private fun createTeclaButton(tecla: String): Button {
        return Button(this).apply {
            text = tecla
            textSize = 24f
            setBackgroundResource(R.drawable.bg_input_field)
            setTextColor(ContextCompat.getColor(context, R.color.primary_dark))

            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 140
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }

            setOnClickListener {
                when (tecla) {
                    "←" -> handleDelete()
                    else -> handleNumberClick(tecla)
                }
            }
        }
    }

    private fun handleNumberClick(num: String) {
        montoActual = if (montoActual == "0") num else montoActual + num
        updateUI()
    }

    private fun handleDelete() {
        montoActual = if (montoActual.length > 1) montoActual.dropLast(1) else "0"
        updateUI()
    }

    private fun updateUI() {
        // Formatear el monto
        val montoFormateado = formatearMonto(montoActual)
        binding.tvMonto.text = "$ $montoFormateado"

        // Habilitar botón guardar si hay categoría y monto válido
        val montoValido = (montoActual.toDoubleOrNull() ?: 0.0) > 0
        val isFormValid = categoriaSeleccionada.isNotEmpty() && montoValido

        binding.btnGuardar.apply {
            isEnabled = isFormValid
            alpha = if (isFormValid) 1.0f else 0.5f
        }
    }

    private fun formatearMonto(monto: String): String {
        return try {
            val numero = monto.toDoubleOrNull() ?: 0.0
            numberFormat.format(numero)
        } catch (_: Exception) {
            monto
        }
    }

    private fun guardarGasto() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            showToast("Error: Usuario no autenticado")
            return
        }

        val monto = montoActual.toDoubleOrNull() ?: 0.0
        if (monto <= 0) {
            showToast("Por favor ingresa un monto válido")
            return
        }

        if (categoriaSeleccionada.isEmpty()) {
            showToast("Por favor selecciona una categoría")
            return
        }

        val gasto = GastoEntity(
            categoria = categoriaSeleccionada,
            descripcion = binding.etNota.text.toString().ifEmpty { categoriaSeleccionada },
            monto = monto,
            fecha = System.currentTimeMillis(),
            userId = userId
        )

        lifecycleScope.launch {
            try {
                val app = application as FinanceApplication
                app.repository.insertGasto(gasto)

                showToast("Gasto guardado correctamente")
                finish()
            } catch (e: Exception) {
                showToast("Error al guardar: ${e.message}")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
