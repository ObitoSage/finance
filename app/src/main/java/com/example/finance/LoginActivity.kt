package com.example.finance

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class LoginActivity : AppCompatActivity() {
    
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var tvForgotPassword: TextView
    private lateinit var cvError: CardView
    private lateinit var tvError: TextView
    private lateinit var btnLogin: Button
    private lateinit var tvGoToSignUp: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        initViews()
        setupListeners()
    }
    
    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        cvError = findViewById(R.id.cvError)
        tvError = findViewById(R.id.tvError)
        btnLogin = findViewById(R.id.btnLogin)
        tvGoToSignUp = findViewById(R.id.tvGoToSignUp)
    }
    
    private fun setupListeners() {
        // Toggle password visibility
        ivTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }
        
        // Forgot password
        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Recuperar contraseña (próximamente)", Toast.LENGTH_SHORT).show()
        }
        
        // Login button
        btnLogin.setOnClickListener {
            handleLogin()
        }
        
        // Navigate to sign up
        tvGoToSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    private fun togglePasswordVisibility() {
        if (etPassword.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        etPassword.setSelection(etPassword.text.length)
    }
    
    private fun handleLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        
        // Hide previous error
        cvError.visibility = View.GONE
        
        // Validation
        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.login_error))
            return
        }
        
        if (!email.contains("@")) {
            showError(getString(R.string.invalid_email))
            return
        }
        
        // Simulate successful login - extract first name from email
        val userName = email.substringBefore("@").split(".")
            .joinToString(" ") { it.capitalize() }
        
        // Navigate to Bienvenida
        val intent = Intent(this, BienvenidaActivity::class.java)
        intent.putExtra("USER_NAME", userName)
        intent.putExtra("USER_EMAIL", email)
        startActivity(intent)
        finish()
    }
    
    private fun showError(message: String) {
        tvError.text = message
        cvError.visibility = View.VISIBLE
    }
}
