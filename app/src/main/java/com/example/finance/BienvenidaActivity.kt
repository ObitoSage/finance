package com.example.finance

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.finance.databinding.ActivityBienvenidaBinding
import com.google.firebase.auth.FirebaseAuth

class BienvenidaActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityBienvenidaBinding
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBienvenidaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        
        // Verificar si hay sesión activa
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Sin sesión, redirigir a login
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        setupWelcomeMessage()
        setupListeners()
    }
    
    private fun setupWelcomeMessage() {
        val user = auth.currentUser
        val userName = user?.displayName ?: user?.email?.substringBefore("@") ?: "Usuario"
        val firstName = userName.split(" ")[0].replaceFirstChar { it.uppercase() }
        binding.tvWelcomeTitle.text = getString(R.string.welcome_title, firstName)
    }
    
    private fun setupListeners() {
        binding.btnContinue.setOnClickListener {
            // Navegar al Dashboard
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        binding.btnLogout.setOnClickListener {
            logout()
        }
    }
    
    private fun logout() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
