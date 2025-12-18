package com.example.finance

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finance.databinding.ActivityNuevaMetaBinding
import com.example.finance.dataClass.Meta
import com.example.finance.dataBase.EntityMappers.toEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class NuevaMetaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNuevaMetaBinding
    private lateinit var auth: FirebaseAuth
    private var colorSeleccionado: String = ""
    private var iconoSeleccionado: String = ""
    private var fechaLimiteTimestamp: Long? = null
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNuevaMetaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupUI()
        setupClickListeners()
        setupSpinners()
    }

    private fun setupUI() {
        // Configurar colores iniciales
        updateColorPreview()
        
        // Deshabilitar botón guardar inicialmente
        binding.btnGuardar.isEnabled = false
        binding.btnGuardar.alpha = 0.5f
        
        // Agregar TextWatchers para validación en tiempo real
        binding.etNombre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validarCampos()
            }
        })
        
        binding.etMontoObjetivo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validarCampos()
            }
        })
    }

    private fun setupSpinners() {
        // Spinner de íconos con opción por defecto
        val iconos = arrayOf("Seleccionar ícono", "Target", "Avión", "Alcancía", "Casa", "Auto", "Estudios", "Salud", "Otro")
        val iconosAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, iconos)
        iconosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerIcono.adapter = iconosAdapter
        
        // Listener para cambiar el ícono
        binding.spinnerIcono.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                iconoSeleccionado = when (position) {
                    0 -> "" // Sin selección
                    1 -> "target"
                    2 -> "plane"
                    3 -> "piggybank"
                    4 -> "home"
                    5 -> "car"
                    6 -> "book"
                    7 -> "health"
                    else -> "other"
                }
                validarCampos()
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupClickListeners() {
        // Botón volver
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Botón guardar
        binding.btnGuardar.setOnClickListener {
            guardarMeta()
        }

        // Selector de colores
        binding.colorRed.setOnClickListener { seleccionarColor("#EF4444", it) }
        binding.colorBlue.setOnClickListener { seleccionarColor("#3B82F6", it) }
        binding.colorGreen.setOnClickListener { seleccionarColor("#10B981", it) }
        binding.colorYellow.setOnClickListener { seleccionarColor("#F59E0B", it) }
        binding.colorPurple.setOnClickListener { seleccionarColor("#8B5CF6", it) }
        binding.colorPink.setOnClickListener { seleccionarColor("#EC4899", it) }

        // Selector de fecha límite
        binding.etFechaLimite.setOnClickListener {
            mostrarDatePicker()
        }
    }

    private fun seleccionarColor(color: String, view: View) {
        colorSeleccionado = color
        
        // Resetear todos los bordes
        binding.colorRed.alpha = 0.5f
        binding.colorBlue.alpha = 0.5f
        binding.colorGreen.alpha = 0.5f
        binding.colorYellow.alpha = 0.5f
        binding.colorPurple.alpha = 0.5f
        binding.colorPink.alpha = 0.5f
        
        // Marcar el seleccionado
        view.alpha = 1f
        
        updateColorPreview()
        validarCampos()
    }

    private fun validarCampos() {
        val nombre = binding.etNombre.text.toString().trim()
        val montoStr = binding.etMontoObjetivo.text.toString().trim()
        val monto = montoStr.toDoubleOrNull()
        
        // Validar que todos los campos estén completos
        val nombreValido = nombre.isNotEmpty()
        val montoValido = monto != null && monto > 0
        val colorValido = colorSeleccionado.isNotEmpty()
        val iconoValido = iconoSeleccionado.isNotEmpty()
        
        // Habilitar/deshabilitar botón según validación
        val todosCompletos = nombreValido && montoValido && colorValido && iconoValido
        binding.btnGuardar.isEnabled = todosCompletos
        binding.btnGuardar.alpha = if (todosCompletos) 1f else 0.5f
    }

    private fun updateColorPreview() {
        if (colorSeleccionado.isNotEmpty()) {
            binding.previewCard.setCardBackgroundColor(Color.parseColor(colorSeleccionado + "20"))
            binding.previewIcon.setColorFilter(Color.parseColor(colorSeleccionado))
        } else {
            binding.previewCard.setCardBackgroundColor(Color.parseColor("#3F3F46"))
            binding.previewIcon.setColorFilter(Color.parseColor("#F0E7D5"))
        }
    }

    private fun mostrarDatePicker() {
        val calendar = Calendar.getInstance()
        
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                fechaLimiteTimestamp = calendar.timeInMillis
                binding.etFechaLimite.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // No permitir fechas pasadas
        datePicker.datePicker.minDate = System.currentTimeMillis()
        datePicker.show()
    }

    private fun guardarMeta() {
        // Validar campos
        val nombre = binding.etNombre.text.toString().trim()
        if (nombre.isEmpty()) {
            binding.etNombre.error = "Ingresa un nombre para tu meta"
            return
        }

        val montoStr = binding.etMontoObjetivo.text.toString().trim()
        if (montoStr.isEmpty()) {
            binding.etMontoObjetivo.error = "Ingresa el monto objetivo"
            return
        }

        val monto = montoStr.toDoubleOrNull()
        if (monto == null || monto <= 0) {
            binding.etMontoObjetivo.error = "Ingresa un monto válido"
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            showToast("Error: Usuario no autenticado")
            return
        }

        // Crear objeto Meta
        val nuevaMeta = Meta(
            id = "",
            nombre = nombre,
            icono = iconoSeleccionado,
            ahorrado = 0.0,
            objetivo = monto,
            color = colorSeleccionado,
            fechaLimite = fechaLimiteTimestamp
        )

        // Guardar en base de datos
        val app = application as FinanceApplication
        val repository = app.repository

        lifecycleScope.launch {
            try {
                val metaEntity = nuevaMeta.toEntity(userId)
                repository.insertMeta(metaEntity)
                
                showToast("Meta '$nombre' creada exitosamente")
                finish()
            } catch (e: Exception) {
                showToast("Error al guardar meta: ${e.message}")
            }
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
