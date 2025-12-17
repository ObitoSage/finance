package com.example.finance

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finance.databinding.ActivityConfigurarPresupuestoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class ConfigurarPresupuestoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigurarPresupuestoBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    private val presupuestos = mutableMapOf<String, Double>()
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    data class CategoriaConfig(
        val nombre: String,
        val icono: Int,
        val color: String
    )

    private val categoriasConfig = listOf(
        CategoriaConfig("Comida afuera", R.drawable.ic_utensils, "#EF4444"),
        CategoriaConfig("Transporte", R.drawable.ic_car, "#3B82F6"),
        CategoriaConfig("Café", R.drawable.ic_coffee, "#8B4513"),
        CategoriaConfig("Mercado", R.drawable.ic_shopping_bag, "#10B981"),
        CategoriaConfig("Hogar", R.drawable.ic_home, "#F59E0B"),
        CategoriaConfig("Entretenimiento", R.drawable.ic_heart, "#EC4899"),
        CategoriaConfig("Servicios", R.drawable.ic_zap, "#6366F1"),
        CategoriaConfig("Celular", R.drawable.ic_smartphone, "#14B8A6")
    )

    // Presupuestos por defecto
    private val presupuestosPorDefecto = mapOf(
        "Comida afuera" to 800000.0,
        "Transporte" to 600000.0,
        "Café" to 200000.0,
        "Mercado" to 1000000.0,
        "Hogar" to 500000.0,
        "Entretenimiento" to 400000.0,
        "Servicios" to 700000.0,
        "Celular" to 100000.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigurarPresupuestoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Actualizar título con mes actual
        val calendario = Calendar.getInstance()
        val mesNombre = calendario.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("es", "ES"))
            ?.replaceFirstChar { it.uppercase() } ?: "Mes"
        val anio = calendario.get(Calendar.YEAR)
        binding.tvTitulo.text = "Presupuesto de $mesNombre de $anio"

        setupClickListeners()
        cargarPresupuestos()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnUsarMesPasado.setOnClickListener {
            usarPresupuestosMesPasado()
        }

        binding.btnGuardar.setOnClickListener {
            guardarPresupuestos()
        }
    }

    private fun cargarPresupuestos() {
        // Cargar presupuestos guardados en Firestore
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                db.collection("users").document(userId)
                    .collection("presupuestos")
                    .document("categorias")
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            // Cargar presupuestos guardados
                            categoriasConfig.forEach { config ->
                                val monto = document.getDouble(config.nombre) ?: presupuestosPorDefecto[config.nombre] ?: 0.0
                                presupuestos[config.nombre] = monto
                            }
                        } else {
                            // Usar valores por defecto
                            presupuestos.putAll(presupuestosPorDefecto)
                        }
                        mostrarCategorias()
                    }
                    .addOnFailureListener {
                        // Usar valores por defecto si falla
                        presupuestos.putAll(presupuestosPorDefecto)
                        mostrarCategorias()
                    }
            } catch (e: Exception) {
                presupuestos.putAll(presupuestosPorDefecto)
                mostrarCategorias()
            }
        }
    }

    private fun mostrarCategorias() {
        binding.containerCategorias.removeAllViews()

        categoriasConfig.forEach { config ->
            val itemView = LayoutInflater.from(this).inflate(
                R.layout.item_presupuesto_categoria,
                binding.containerCategorias,
                false
            )

            val iconContainer = itemView.findViewById<FrameLayout>(R.id.iconContainer)
            val ivIcono = itemView.findViewById<ImageView>(R.id.ivIcono)
            val tvNombre = itemView.findViewById<TextView>(R.id.tvNombre)
            val etPresupuesto = itemView.findViewById<EditText>(R.id.etPresupuesto)

            // Configurar icono
            ivIcono.setImageResource(config.icono)
            ivIcono.setColorFilter(Color.parseColor(config.color))

            // Configurar fondo del icono
            val iconBackground = GradientDrawable()
            iconBackground.shape = GradientDrawable.RECTANGLE
            iconBackground.cornerRadius = 12f * resources.displayMetrics.density
            val colorWithAlpha = Color.parseColor(config.color)
            iconBackground.setColor(Color.argb(25, Color.red(colorWithAlpha), Color.green(colorWithAlpha), Color.blue(colorWithAlpha)))
            iconContainer.background = iconBackground

            // Configurar nombre
            tvNombre.text = config.nombre

            // Configurar campo de presupuesto
            val presupuestoActual = presupuestos[config.nombre] ?: 0.0
            etPresupuesto.setText(presupuestoActual.toInt().toString())

            // Listener para cambios
            etPresupuesto.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val monto = s.toString().replace("[^0-9]".toRegex(), "").toDoubleOrNull() ?: 0.0
                    presupuestos[config.nombre] = monto
                }
            })

            binding.containerCategorias.addView(itemView)
        }
    }

    private fun usarPresupuestosMesPasado() {
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                // Calcular mes pasado
                val calendario = Calendar.getInstance()
                calendario.add(Calendar.MONTH, -1)
                val mesPasado = calendario.get(Calendar.MONTH)
                val anioPasado = calendario.get(Calendar.YEAR)

                // Intentar cargar presupuestos del mes pasado
                db.collection("users").document(userId)
                    .collection("presupuestos")
                    .document("categorias_${mesPasado}_${anioPasado}")
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            categoriasConfig.forEach { config ->
                                val monto = document.getDouble(config.nombre) ?: presupuestosPorDefecto[config.nombre] ?: 0.0
                                presupuestos[config.nombre] = monto
                            }
                            mostrarCategorias()
                            showToast("Presupuestos del mes pasado cargados")
                        } else {
                            showToast("No hay presupuestos guardados del mes pasado")
                        }
                    }
                    .addOnFailureListener {
                        showToast("Error al cargar presupuestos del mes pasado")
                    }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    private fun guardarPresupuestos() {
        val userId = auth.currentUser?.uid ?: return
        
        // Verificar que haya datos para guardar
        if (presupuestos.isEmpty()) {
            Toast.makeText(this, "No hay presupuestos para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar mensaje de inicio
        Toast.makeText(this, "Guardando presupuestos...", Toast.LENGTH_SHORT).show()

        // Guardar en Firestore
        db.collection("users").document(userId)
            .collection("presupuestos")
            .document("categorias")
            .set(presupuestos)
            .addOnSuccessListener {
                Toast.makeText(this, "✓ Presupuesto guardado exitosamente", Toast.LENGTH_LONG).show()
                // Ir a CategoriasActivity después de un pequeño delay
                binding.root.postDelayed({
                    val intent = Intent(this, CategoriasActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                }, 1000)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount)
            .replace("COP", "$")
            .replace("\u00A0", "")
            .trim()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
