package com.example.finance

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finance.databinding.ActivityConfigurarPresupuestoInicialBinding
import com.example.finance.dataBase.repository.FinanceRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ConfigurarPresupuestoInicialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigurarPresupuestoInicialBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var repository: FinanceRepository

    private var presupuestoMensual: Long = 0
    
    // Formato de moneda boliviana
    private val currencyFormat = NumberFormat.getNumberInstance(Locale("es", "BO"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigurarPresupuestoInicialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        repository = (application as FinanceApplication).repository

        setupInputListener()
        setupClickListeners()
    }

    private fun setupInputListener() {
        binding.etPresupuestoMensual.addTextChangedListener(object : TextWatcher {
            private var current = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    binding.etPresupuestoMensual.removeTextChangedListener(this)

                    // Limpiar el string de todo excepto dígitos
                    val cleanString = s.toString().replace(Regex("[^\\d]"), "")

                    if (cleanString.isNotEmpty()) {
                        try {
                            presupuestoMensual = cleanString.toLong()
                            
                            // Formatear el número
                            val formatted = currencyFormat.format(presupuestoMensual)
                            current = formatted
                            binding.etPresupuestoMensual.setText(formatted)
                            binding.etPresupuestoMensual.setSelection(formatted.length)
                            
                            // Actualizar UI
                            updateDailyBudget()
                            updateButtonState()
                        } catch (e: NumberFormatException) {
                            presupuestoMensual = 0
                            updateDailyBudget()
                            updateButtonState()
                        }
                    } else {
                        current = ""
                        presupuestoMensual = 0
                        updateDailyBudget()
                        updateButtonState()
                    }

                    binding.etPresupuestoMensual.addTextChangedListener(this)
                }
            }
        })
    }

    private fun setupClickListeners() {
        // Botón guardar
        binding.btnGuardar.setOnClickListener {
            guardarPresupuesto()
        }

        // Botón configurar después
        binding.btnConfigureLater.setOnClickListener {
            navigateToDashboard()
        }

        // Botones de ejemplos
        binding.btnExample1.setOnClickListener {
            setPresupuestoExample(500)
        }

        binding.btnExample2.setOnClickListener {
            setPresupuestoExample(1000)
        }

        binding.btnExample3.setOnClickListener {
            setPresupuestoExample(2000)
        }
    }

    private fun setPresupuestoExample(amount: Int) {
        presupuestoMensual = amount.toLong()
        val formatted = currencyFormat.format(presupuestoMensual)
        binding.etPresupuestoMensual.setText(formatted)
        binding.etPresupuestoMensual.setSelection(formatted.length)
        updateDailyBudget()
        updateButtonState()
    }

    private fun updateDailyBudget() {
        if (presupuestoMensual > 0) {
            // Calcular días del mes actual
            val calendar = Calendar.getInstance()
            val diasDelMes = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val diaActual = calendar.get(Calendar.DAY_OF_MONTH)
            val diasRestantes = diasDelMes - diaActual + 1
            
            // Calcular presupuesto diario usando días reales
            val presupuestoDiario = (presupuestoMensual.toDouble() / diasDelMes).toInt()
            
            // Mostrar card con información
            binding.cardPresupuestoDiario.visibility = View.VISIBLE
            binding.tvPresupuestoDiario.text = formatCurrency(presupuestoDiario.toDouble())
            
            // Actualizar texto informativo con días del mes
            val mesNombre = SimpleDateFormat("MMMM", Locale("es", "ES")).format(Date())
            binding.tvDailyBudgetInfo.text = "Esto es lo que puedes gastar cada día en promedio ($diasDelMes días en $mesNombre)"
            
            // Mostrar días restantes si existe el TextView
            binding.tvDaysRemaining?.text = "Quedan $diasRestantes días del mes"
        } else {
            binding.cardPresupuestoDiario.visibility = View.GONE
        }
    }

    private fun updateButtonState() {
        binding.btnGuardar.isEnabled = presupuestoMensual > 0
    }

    private fun guardarPresupuesto() {
        if (presupuestoMensual <= 0) {
            Toast.makeText(this, "Por favor ingresa un presupuesto mensual válido", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar progreso
        binding.btnGuardar.isEnabled = false
        binding.btnGuardar.text = "Guardando..."

        // Guardar en Room usando coroutines
        lifecycleScope.launch {
            try {
                repository.updatePresupuestoMensual(userId, presupuestoMensual.toDouble())
                Toast.makeText(this@ConfigurarPresupuestoInicialActivity, "Presupuesto guardado exitosamente", Toast.LENGTH_SHORT).show()
                navigateToDashboard()
            } catch (e: Exception) {
                binding.btnGuardar.isEnabled = true
                binding.btnGuardar.text = getString(R.string.start_using_app)
                Toast.makeText(this@ConfigurarPresupuestoInicialActivity, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun formatCurrency(amount: Double): String {
        return "Bs ${currencyFormat.format(amount.toLong())}"
    }
}
