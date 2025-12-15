package com.example.finance

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.finance.databinding.ActivityCategoriasBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.*

class CategoriasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriasBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Formato de moneda
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    data class Categoria(
        val id: String,
        val nombre: String,
        val icono: Int,
        val gastado: Double,
        val presupuesto: Double,
        val colorFondo: String,
        val colorIcono: String,
        val colorBarra: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoriasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupClickListeners()
        cargarCategorias()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnConfigurarPresupuesto.setOnClickListener {
            // TODO: Navegar a configurar presupuesto por categorías
            showToast("Configurar presupuesto por categorías (próximamente)")
        }
    }

    private fun cargarCategorias() {
        // Mostrar categorías hardcodeadas directamente
        mostrarCategorias(obtenerCategorias())
    }

    private fun obtenerCategorias(): List<Categoria> {
        return listOf(
            Categoria("1", "Comida afuera", R.drawable.ic_utensils, 720000.0, 800000.0, "#FFE5E5", "#EF4444", "#EF4444"),
            Categoria("2", "Transporte", R.drawable.ic_car, 500000.0, 600000.0, "#E0F2FE", "#3B82F6", "#3B82F6"),
            Categoria("3", "Café", R.drawable.ic_coffee, 180000.0, 200000.0, "#F5E6D3", "#8B4513", "#8B4513"),
            Categoria("4", "Mercado", R.drawable.ic_shopping_bag, 700000.0, 1000000.0, "#D1FAE5", "#10B981", "#10B981"),
            Categoria("5", "Hogar", R.drawable.ic_home, 400000.0, 500000.0, "#FFEDD5", "#F59E0B", "#F59E0B"),
            Categoria("6", "Entretenimiento", R.drawable.ic_heart, 320000.0, 400000.0, "#FCE7F3", "#EC4899", "#EC4899"),
            Categoria("7", "Servicios", R.drawable.ic_zap, 650000.0, 700000.0, "#EDE9FE", "#6366F1", "#6366F1"),
            Categoria("8", "Celular", R.drawable.ic_smartphone, 95000.0, 100000.0, "#CCFBF1", "#14B8A6", "#14B8A6")
        )
    }

    private fun mostrarCategorias(categorias: List<Categoria>) {
        binding.containerCategorias.removeAllViews()
        
        categorias.forEach { categoria ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_categoria, binding.containerCategorias, false)
            
            // Referencias a las vistas
            val iconContainer = itemView.findViewById<FrameLayout>(R.id.iconContainer)
            val ivCategoriaIcono = itemView.findViewById<ImageView>(R.id.ivCategoriaIcono)
            val tvCategoriaNombre = itemView.findViewById<TextView>(R.id.tvCategoriaNombre)
            val tvCategoriaMontos = itemView.findViewById<TextView>(R.id.tvCategoriaMontos)
            val progressBar = itemView.findViewById<View>(R.id.progressBar)
            val tvCategoriaRestante = itemView.findViewById<TextView>(R.id.tvCategoriaRestante)
            
            // Configurar icono
            ivCategoriaIcono.setImageResource(categoria.icono)
            ivCategoriaIcono.setColorFilter(Color.parseColor(categoria.colorIcono))
            
            // Configurar fondo del icono
            val iconBackground = GradientDrawable()
            iconBackground.shape = GradientDrawable.RECTANGLE
            iconBackground.cornerRadius = 12f * resources.displayMetrics.density
            iconBackground.setColor(Color.parseColor(categoria.colorFondo))
            iconContainer.background = iconBackground
            
            // Configurar textos
            tvCategoriaNombre.text = categoria.nombre
            tvCategoriaMontos.text = "${formatCurrency(categoria.gastado)} de ${formatCurrency(categoria.presupuesto)}"
            
            // Configurar barra de progreso
            val porcentaje = if (categoria.presupuesto > 0) {
                ((categoria.gastado / categoria.presupuesto) * 100).coerceAtMost(100.0)
            } else 0.0
            
            val progressBackground = GradientDrawable()
            progressBackground.shape = GradientDrawable.RECTANGLE
            progressBackground.cornerRadius = 4f * resources.displayMetrics.density
            progressBackground.setColor(Color.parseColor(categoria.colorBarra))
            progressBar.background = progressBackground
            
            // Configurar ancho de la barra inmediatamente
            progressBar.viewTreeObserver.addOnGlobalLayoutListener(
                object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        progressBar.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        val parent = progressBar.parent as View
                        val parentWidth = parent.width
                        val newWidth = (parentWidth * porcentaje / 100).toInt()
                        val params = progressBar.layoutParams
                        params.width = newWidth.coerceAtLeast(1) // Mínimo 1dp visible
                        progressBar.layoutParams = params
                    }
                }
            )
            
            // Configurar texto restante
            val restante = categoria.presupuesto - categoria.gastado
            val isAlerta = porcentaje > 85
            
            if (restante >= 0) {
                tvCategoriaRestante.text = "Quedan ${formatCurrency(restante)}"
            } else {
                tvCategoriaRestante.text = "Excediste por ${formatCurrency(Math.abs(restante))}"
            }
            
            tvCategoriaRestante.setTextColor(
                if (isAlerta) ContextCompat.getColor(this, R.color.red_500)
                else ContextCompat.getColor(this, R.color.gray_600)
            )
            
            if (isAlerta) {
                tvCategoriaRestante.setTypeface(null, android.graphics.Typeface.BOLD)
            }
            
            binding.containerCategorias.addView(itemView)
        }
    }

    private fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount)
            .replace("COP", "$")
            .replace("\u00A0", "")
            .trim()
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
