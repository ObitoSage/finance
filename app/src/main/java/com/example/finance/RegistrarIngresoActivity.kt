package com.example.finance

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.finance.databinding.ActivityRegistrarIngresoBinding
import com.example.finance.dataBase.entities.IngresoEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class RegistrarIngresoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrarIngresoBinding
    private lateinit var auth: FirebaseAuth

    // Estado
    private var categoriaSeleccionada = ""
    private var montoSinFormato = ""

    // Formato de moneda
    @Suppress("DEPRECATION")
    private val numberFormat = NumberFormat.getNumberInstance(Locale("es", "CO"))

    // Categorías: id -> (nombre, emoji)
    private val categorias = listOf(
        Triple("salario", "Salario", "💼"),
        Triple("freelance", "Freelance", "📈"),
        Triple("bonificacion", "Bonificación", "🎁"),
        Triple("venta", "Venta", "🏷️"),
        Triple("otro", "Otro", "💵")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrarIngresoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupCategorias()
        setupClickListeners()
        setupMontoTextWatcher()
        updateUI()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnGuardar.setOnClickListener { guardarIngreso() }

        binding.etDescripcion.addTextChangedListener(SimpleTextWatcher { updateUI() })
    }

    private fun setupMontoTextWatcher() {
        binding.etMonto.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                isUpdating = true

                try {
                    val cleanString = s.toString().replace("[^\\d]".toRegex(), "")

                    if (cleanString.isNotEmpty()) {
                        montoSinFormato = cleanString
                        val formatted = formatearMonto(cleanString)
                        binding.etMonto.setText(formatted)
                        binding.etMonto.setSelection(formatted.length)
                    } else {
                        montoSinFormato = ""
                    }
                } catch (_: Exception) {
                    // Ignorar errores de formato
                }
                
                isUpdating = false
                updateUI()
            }
        })
    }

    private fun setupCategorias() {
        categorias.forEach { (id, nombre, emoji) ->
            val layout = createCategoriaLayout(id, nombre, emoji)
            binding.gridCategorias.addView(layout)
        }
    }

    private fun createCategoriaLayout(id: String, nombre: String, emoji: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundResource(R.drawable.bg_categoria_default)

            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }

            // Icono emoji
            addView(TextView(context).apply {
                text = emoji
                textSize = 20f
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_icon_circle)
                setPadding(12, 12, 12, 12)
            })

            // Texto nombre
            addView(TextView(context).apply {
                text = nombre
                textSize = 17f
                setTextColor(ContextCompat.getColor(context, R.color.primary_dark))
                setPadding(12, 0, 0, 0)
            })

            setOnClickListener { seleccionarCategoria(id) }
        }
    }

    private fun seleccionarCategoria(categoriaId: String) {
        categoriaSeleccionada = categoriaId
        
        // Actualizar estado visual de todos los layouts
        for (i in 0 until binding.gridCategorias.childCount) {
            val layout = binding.gridCategorias.getChildAt(i) as LinearLayout
            val isSelected = categorias.getOrNull(i)?.first == categoriaId

            layout.setBackgroundResource(
                if (isSelected) R.drawable.bg_categoria_selected
                else R.drawable.bg_categoria_default
            )

            // Actualizar color del texto (segundo hijo del layout)
            val textView = layout.getChildAt(1) as TextView
            textView.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (isSelected) android.R.color.white else R.color.primary_dark
                )
            )
        }
        
        updateUI()
    }

    private fun updateUI() {
        val montoValido = (montoSinFormato.toLongOrNull() ?: 0L) > 0
        val isFormValid = categoriaSeleccionada.isNotEmpty() && montoValido

        binding.btnGuardar.apply {
            isEnabled = isFormValid
            alpha = if (isFormValid) 1.0f else 0.5f
        }
    }

    private fun formatearMonto(value: String): String {
        return try {
            val numero = value.toLongOrNull() ?: 0L
            numberFormat.format(numero)
        } catch (_: Exception) {
            value
        }
    }

    private fun guardarIngreso() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            showToast("Error: Usuario no autenticado")
            return
        }

        val monto = montoSinFormato.toDoubleOrNull() ?: 0.0
        if (monto <= 0) {
            showToast("Por favor ingresa un monto válido")
            return
        }

        if (categoriaSeleccionada.isEmpty()) {
            showToast("Por favor selecciona una categoría")
            return
        }

        // Obtener el nombre visible de la categoría
        val categoriaNombre = categorias.find { it.first == categoriaSeleccionada }?.second
            ?: categoriaSeleccionada

        val ingreso = IngresoEntity(
            categoria = categoriaNombre,
            descripcion = binding.etDescripcion.text.toString().ifEmpty { "Sin descripción" },
            monto = monto,
            fecha = System.currentTimeMillis(),
            userId = userId
        )

        lifecycleScope.launch {
            try {
                val app = application as FinanceApplication
                app.repository.insertIngreso(ingreso)

                showToast("Ingreso guardado correctamente")
                finish()
            } catch (e: Exception) {
                showToast("Error al guardar: ${e.message}")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * TextWatcher simplificado para casos donde solo se necesita afterTextChanged
     */
    private class SimpleTextWatcher(private val onChanged: () -> Unit) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) { onChanged() }
    }
}
