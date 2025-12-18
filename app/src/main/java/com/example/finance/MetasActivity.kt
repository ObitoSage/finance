package com.example.finance

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finance.adapters.MetaAdapter
import com.example.finance.databinding.ActivityMetasBinding
import com.example.finance.dataClass.Meta
import com.example.finance.dataBase.EntityMappers.toEntity
import com.example.finance.dataBase.EntityMappers.toMetasDomain
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class MetasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMetasBinding
    private lateinit var metaAdapter: MetaAdapter
    private lateinit var auth: FirebaseAuth
    private val metas = mutableListOf<Meta>()
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMetasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        
        setupUI()
        setupRecyclerView()
        setupClickListeners()
        loadMetas()
    }

    private fun setupUI() {
        // El título ya está en el layout
    }

    private fun setupRecyclerView() {
        metaAdapter = MetaAdapter(
            onAgregarClick = { meta ->
                agregarDineroAMeta(meta)
            },
            onEliminarClick = { meta ->
                confirmarEliminarMeta(meta)
            }
        )
        
        binding.rvMetas.apply {
            layoutManager = LinearLayoutManager(this@MetasActivity)
            adapter = metaAdapter
        }
    }

    private fun setupClickListeners() {
        // Botón volver
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Botón nueva meta
        binding.btnNuevaMeta.setOnClickListener {
            val intent = Intent(this, NuevaMetaActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadMetas() {
        val userId = auth.currentUser?.uid ?: return
        val app = application as FinanceApplication
        val repository = app.repository

        lifecycleScope.launch {
            try {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    repository.getAllMetas(userId).collect { metasEntity ->
                        // Convertir a lista de dominio
                        val nuevasMetas = metasEntity.toMetasDomain()
                        
                        // Actualizar la lista y el adapter
                        metas.clear()
                        metas.addAll(nuevasMetas)
                        
                        // Enviar una nueva lista al adapter para que detecte el cambio
                        metaAdapter.submitList(nuevasMetas.toList())
                        updateEmptyState()
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    showToast("Error al cargar metas: ${e.message}")
                }
            }
        }
    }

    private fun updateEmptyState() {
        if (metas.isEmpty()) {
            binding.rvMetas.visibility = View.GONE
            // TODO: Mostrar estado vacío
        } else {
            binding.rvMetas.visibility = View.VISIBLE
        }
    }

    private fun agregarDineroAMeta(meta: Meta) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_agregar_dinero)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Referencias a las vistas del diálogo
        val tvNombreMeta = dialog.findViewById<TextView>(R.id.tvNombreMeta)
        val tvProgresoActual = dialog.findViewById<TextView>(R.id.tvProgresoActual)
        val tvPorcentaje = dialog.findViewById<TextView>(R.id.tvPorcentaje)
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)
        val etMonto = dialog.findViewById<TextInputEditText>(R.id.etMonto)
        val btnCancelar = dialog.findViewById<MaterialButton>(R.id.btnCancelar)
        val btnAgregar = dialog.findViewById<MaterialButton>(R.id.btnAgregar)

        // Configurar datos de la meta
        tvNombreMeta.text = meta.nombre
        val progresoText = "${formatCurrency(meta.ahorrado)} de ${formatCurrency(meta.objetivo)}"
        tvProgresoActual.text = progresoText
        
        val porcentaje = meta.getPorcentaje().toInt()
        progressBar.max = 100
        progressBar.progress = porcentaje
        
        // Aplicar color personalizado de la meta
        val colorHex = Color.parseColor(meta.color)
        progressBar.progressDrawable.setColorFilter(colorHex, PorterDuff.Mode.SRC_IN)
        
        // Porcentaje con color de la meta
        tvPorcentaje.text = "$porcentaje% completado"
        tvPorcentaje.setTextColor(colorHex)

        // Botón cancelar
        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        // Botón agregar
        btnAgregar.setOnClickListener {
            val montoStr = etMonto.text.toString().trim()
            
            if (montoStr.isEmpty()) {
                etMonto.error = "Ingresa un monto"
                return@setOnClickListener
            }

            val monto = montoStr.toDoubleOrNull()
            if (monto == null || monto <= 0) {
                etMonto.error = "Ingresa un monto válido"
                return@setOnClickListener
            }

            // Agregar dinero a la meta en la base de datos
            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            val app = application as FinanceApplication
            val repository = app.repository

            lifecycleScope.launch {
                try {
                    val metaId = meta.id.toLongOrNull() ?: return@launch
                    repository.agregarDineroAMeta(metaId, monto)
                    
                    showToast("Se agregaron ${formatCurrency(monto)} a ${meta.nombre}")
                    dialog.dismiss()
                } catch (e: Exception) {
                    showToast("Error al agregar dinero: ${e.message}")
                }
            }
        }

        dialog.show()
    }

    private fun confirmarEliminarMeta(meta: Meta) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Eliminar meta")
        builder.setMessage("¿Estás seguro de que quieres eliminar la meta '${meta.nombre}'?")
        builder.setPositiveButton("Eliminar") { _, _ ->
            eliminarMeta(meta)
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun eliminarMeta(meta: Meta) {
        val app = application as FinanceApplication
        val repository = app.repository

        lifecycleScope.launch {
            try {
                val metaId = meta.id.toLongOrNull() ?: return@launch
                repository.deleteMetaById(metaId)
                showToast("Meta eliminada")
            } catch (e: Exception) {
                showToast("Error al eliminar meta: ${e.message}")
            }
        }
    }
    
    private fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount).replace("COP", "$")
    }

    override fun onResume() {
        super.onResume()
        // Las metas se recargan automáticamente gracias al Flow
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
