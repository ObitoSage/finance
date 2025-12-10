package com.example.finance

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.finance.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        
        // Verificar si ya hay sesión activa
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToBienvenida()
            return
        }
        
        setupListeners()
    }
    
    private fun setupListeners() {
        // Mostrar/ocultar contraseña
        binding.ivTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }
        
        // Recuperar contraseña
        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Recuperar contraseña (próximamente)", Toast.LENGTH_SHORT).show()
        }
        
        // Botón de login
        binding.btnLogin.setOnClickListener {
            handleLogin()
        }
        
        // Ir a registro
        binding.tvGoToSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    private fun togglePasswordVisibility() {
        if (binding.etPassword.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
            binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            binding.etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        binding.etPassword.setSelection(binding.etPassword.text.length)
    }
    
    private fun handleLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
        // Ocultar error previo
        binding.cvError.visibility = View.GONE
        
        // Validaciones
        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.login_error))
            return
        }
        
        if (!email.contains("@")) {
            showError(getString(R.string.invalid_email))
            return
        }
        
        // Desactivar botón durante login
        binding.btnLogin.isEnabled = false
        
        // Autenticar con Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.btnLogin.isEnabled = true
                if (task.isSuccessful) {
                    Toast.makeText(this, "Hola", Toast.LENGTH_SHORT).show()
                    navigateToBienvenida()
                } else {
                    val errorMessage = task.exception?.message ?: "Error al iniciar sesión"
                    showError(errorMessage)
                }
            }
    }
    
    private fun navigateToBienvenida() {
        val intent = Intent(this, BienvenidaActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun showError(message: String) {
        binding.tvError.text = message
        binding.cvError.visibility = View.VISIBLE
    }
}
