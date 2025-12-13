package com.example.finance

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.finance.databinding.ActivityNuevaMetaBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class NuevaMetaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNuevaMetaBinding
    private var colorSeleccionado: String = "#EF4444"
    private var iconoSeleccionado: String = "target"
    private var fechaLimiteTimestamp: Long? = null
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNuevaMetaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupClickListeners()
        setupSpinners()
    }

    private fun setupUI() {
        // Configurar colores iniciales
        updateColorPreview()
    }

    private fun setupSpinners() {
        // Spinner de íconos
        val iconos = arrayOf("Target", "Avión", "Alcancía", "Casa", "Auto", "Estudios", "Salud", "Otro")
        val iconosAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, iconos)
        iconosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerIcono.adapter = iconosAdapter
        
        // Listener para cambiar el ícono
        binding.spinnerIcono.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                iconoSeleccionado = when (position) {
                    0 -> "target"
                    1 -> "plane"
                    2 -> "piggybank"
                    3 -> "home"
                    4 -> "car"
                    5 -> "book"
                    6 -> "health"
                    else -> "other"
                }
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
    }

    private fun updateColorPreview() {
        binding.previewCard.setCardBackgroundColor(Color.parseColor(colorSeleccionado + "20"))
        binding.previewIcon.setColorFilter(Color.parseColor(colorSeleccionado))
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

        // TODO: Guardar en base de datos
        // Por ahora solo mostrar mensaje
        showToast("Meta '$nombre' creada exitosamente")
        finish()
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
