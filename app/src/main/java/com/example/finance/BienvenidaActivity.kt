package com.example.finance

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BienvenidaActivity : AppCompatActivity() {
    
    private lateinit var tvWelcomeTitle: TextView
    private lateinit var btnContinue: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bienvenida)
        
        initViews()
        setupWelcomeMessage()
        setupListeners()
    }
    
    private fun initViews() {
        tvWelcomeTitle = findViewById(R.id.tvWelcomeTitle)
        btnContinue = findViewById(R.id.btnContinue)
    }
    
    private fun setupWelcomeMessage() {
        val userName = intent.getStringExtra("USER_NAME") ?: "Usuario"
        val firstName = userName.split(" ")[0]
        tvWelcomeTitle.text = getString(R.string.welcome_title, firstName)
    }
    
    private fun setupListeners() {
        btnContinue.setOnClickListener {
            
            // Temporal: mostrar mensaje
            android.widget.Toast.makeText(
                this, 
                "Configuración de presupuesto próximamente", 
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}
