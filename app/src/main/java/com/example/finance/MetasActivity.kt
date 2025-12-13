package com.example.finance

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finance.adapters.MetaAdapter
import com.example.finance.databinding.ActivityMetasBinding
import com.example.finance.dataClass.Meta

class MetasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMetasBinding
    private lateinit var metaAdapter: MetaAdapter
    private val metas = mutableListOf<Meta>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMetasBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        // TODO: Cargar desde base de datos
        // Por ahora, metas de ejemplo
        metas.clear()
        metas.addAll(
            listOf(
                Meta(
                    id = "1",
                    nombre = "Fondo de emergencia",
                    icono = "target",
                    ahorrado = 1200000.0,
                    objetivo = 5000000.0,
                    color = "#EF4444"
                ),
                Meta(
                    id = "2",
                    nombre = "Vacaciones",
                    icono = "plane",
                    ahorrado = 850000.0,
                    objetivo = 3000000.0,
                    color = "#3B82F6"
                ),
                Meta(
                    id = "3",
                    nombre = "Inversión",
                    icono = "piggybank",
                    ahorrado = 2400000.0,
                    objetivo = 10000000.0,
                    color = "#10B981"
                )
            )
        )
        
        metaAdapter.submitList(metas)
        updateEmptyState()
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
        // TODO: Implementar diálogo para agregar dinero a la meta
        showToast("Agregar dinero a: ${meta.nombre}")
    }

    override fun onResume() {
        super.onResume()
        // Recargar metas cuando se vuelve a la pantalla
        loadMetas()
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
