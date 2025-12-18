package com.example.finance

import android.content.Context
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
    
    companion object {
        private const val PREFS_NAME = "FinanceAppPrefs"
        private const val KEY_PRESUPUESTO_CONFIGURADO = "presupuestoConfigurado"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigurarPresupuestoInicialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        
        // Verificar sesión
        if (auth.currentUser == null) {
            navigateToLogin()
            return
        }
        
        repository = (application as FinanceApplication).repository

        setupInputListener()
        setupClickListeners()
        loadExistingBudget()
    }
    
    /**
     * FIN-39: Cargar presupuesto existente si ya fue configurado.
     * Permite editar el presupuesto en lugar de solo crear uno nuevo.
     */
    private fun loadExistingBudget() {
        val userId = auth.currentUser?.uid ?: return
        
        lifecycleScope.launch {
            try {
                val presupuestoExistente = repository.getPresupuestoMensual(userId)
                if (presupuestoExistente > 0) {
                    presupuestoMensual = presupuestoExistente.toLong()
                    val formatted = currencyFormat.format(presupuestoMensual)
                    binding.etPresupuestoMensual.setText(formatted)
                    binding.etPresupuestoMensual.setSelection(formatted.length)
                    updateDailyBudget()
                    updateButtonState()
                    
                    // Cambiar texto del botón a "Actualizar"
                    binding.btnGuardar.text = "Actualizar presupuesto"
                }
            } catch (e: Exception) {
                // No hacer nada, simplemente no cargar presupuesto
            }
        }
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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

    /**
     * FIN-38: Cálculo automático de presupuesto diario.
     * Calcula el presupuesto diario basado en los días reales del mes actual.
     * Muestra información útil sobre días restantes y promedio diario.
     */
    private fun updateDailyBudget() {
        if (presupuestoMensual > 0) {
            // Calcular días del mes actual
            val calendar = Calendar.getInstance()
            val diasDelMes = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val diaActual = calendar.get(Calendar.DAY_OF_MONTH)
            val diasRestantes = diasDelMes - diaActual + 1
            
            // FIN-38: Cálculo automático de presupuesto diario usando días reales
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

    /**
     * FIN-39: Guardar configuración de presupuesto en base de datos.
     * Guarda el presupuesto en Room Database vinculado al usuario de Firebase Auth.
     * Valida datos antes de guardar y maneja errores apropiadamente.
     */
    private fun guardarPresupuesto() {
        // Validación: Presupuesto debe ser mayor a 0
        if (presupuestoMensual <= 0) {
            Toast.makeText(this, "Por favor ingresa un presupuesto mensual válido", Toast.LENGTH_SHORT).show()
            return
        }

        // Validación: Usuario debe estar autenticado
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            navigateToLogin()
            return
        }

        // Mostrar progreso
        binding.btnGuardar.isEnabled = false
        val originalText = binding.btnGuardar.text
        binding.btnGuardar.text = "Guardando..."

        // FIN-39: Guardar en Room Database usando coroutines
        lifecycleScope.launch {
            try {
                // Guardar o actualizar presupuesto en Room
                repository.updatePresupuestoMensual(userId, presupuestoMensual.toDouble())
                
                // Marcar que el presupuesto fue configurado
                markPresupuestoConfigurado()
                
                Toast.makeText(
                    this@ConfigurarPresupuestoInicialActivity, 
                    "✓ Presupuesto guardado exitosamente", 
                    Toast.LENGTH_SHORT
                ).show()
                
                // Navegar al Dashboard con animación
                navigateToDashboard()
            } catch (e: Exception) {
                // Restaurar estado del botón en caso de error
                binding.btnGuardar.isEnabled = true
                binding.btnGuardar.text = originalText
                
                Toast.makeText(
                    this@ConfigurarPresupuestoInicialActivity, 
                    "Error al guardar: ${e.message}", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    /**
     * Marca en SharedPreferences que el usuario ya configuró su presupuesto.
     */
    private fun markPresupuestoConfigurado() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PRESUPUESTO_CONFIGURADO, true)
            .apply()
    }

    /**
     * FIN-40: Opción "Configurar después".
     * Permite al usuario omitir este paso y continuar sin configurar presupuesto.
     */
    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun formatCurrency(amount: Double): String {
        return "Bs ${currencyFormat.format(amount.toLong())}"
    }
    
    /**
     * Verifica si el usuario ya configuró su presupuesto.
     */
    fun isPresupuestoConfigurado(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PRESUPUESTO_CONFIGURADO, false)
    }
}
