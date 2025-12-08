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
            val userName = intent.getStringExtra("USER_NAME") ?: "Usuario"
            val userEmail = intent.getStringExtra("USER_EMAIL") ?: ""
            
            val intent = Intent(this, ConfigurarPresupuestoInicialActivity::class.java)
            intent.putExtra("USER_NAME", userName)
            intent.putExtra("USER_EMAIL", userEmail)
            startActivity(intent)
            finish()
        }
    }
}
