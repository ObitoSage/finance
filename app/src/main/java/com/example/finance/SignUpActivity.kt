package com.example.finance

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class SignUpActivity : AppCompatActivity() {
    
    // Views
    private lateinit var btnBack: CardView
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var ivNameCheck: ImageView
    private lateinit var ivEmailCheck: ImageView
    private lateinit var ivTogglePassword: ImageView
    private lateinit var ivToggleConfirmPassword: ImageView
    private lateinit var tvNameError: TextView
    private lateinit var tvEmailError: TextView
    private lateinit var tvPasswordError: TextView
    private lateinit var tvConfirmPasswordError: TextView
    private lateinit var cbTerms: CheckBox
    private lateinit var btnSignUp: Button
    private lateinit var tvGoToLogin: TextView
    
    // Validation states
    private var isNameValid = false
    private var isEmailValid = false
    private var isPasswordValid = false
    private var doPasswordsMatch = false
    private var termsAccepted = false
    
    // Touched states
    private var nameTouched = false
    private var emailTouched = false
    private var passwordTouched = false
    private var confirmPasswordTouched = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        
        initViews()
        setupListeners()
    }
    
    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        ivNameCheck = findViewById(R.id.ivNameCheck)
        ivEmailCheck = findViewById(R.id.ivEmailCheck)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword)
        tvNameError = findViewById(R.id.tvNameError)
        tvEmailError = findViewById(R.id.tvEmailError)
        tvPasswordError = findViewById(R.id.tvPasswordError)
        tvConfirmPasswordError = findViewById(R.id.tvConfirmPasswordError)
        cbTerms = findViewById(R.id.cbTerms)
        btnSignUp = findViewById(R.id.btnSignUp)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)
    }
    
    private fun setupListeners() {
        // Back button
        btnBack.setOnClickListener {
            navigateToLogin()
        }
        
        // Name validation
        etName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateName()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        etName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                nameTouched = true
                validateName()
            }
        }
        
        // Email validation
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateEmail()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                emailTouched = true
                validateEmail()
            }
        }
        
        // Password validation
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validatePassword()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                passwordTouched = true
                validatePassword()
            }
        }
        
        // Confirm password validation
        etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateConfirmPassword()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        etConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                confirmPasswordTouched = true
                validateConfirmPassword()
            }
        }
        
        // Toggle password visibility
        ivTogglePassword.setOnClickListener {
            togglePasswordVisibility(etPassword, ivTogglePassword)
        }
        
        ivToggleConfirmPassword.setOnClickListener {
            togglePasswordVisibility(etConfirmPassword, ivToggleConfirmPassword)
        }
        
        // Terms checkbox
        cbTerms.setOnCheckedChangeListener { _, isChecked ->
            termsAccepted = isChecked
            updateSubmitButton()
        }
        
        // Submit button
        btnSignUp.setOnClickListener {
            handleSignUp()
        }
        
        // Navigate to login
        tvGoToLogin.setOnClickListener {
            navigateToLogin()
        }
    }
    
    private fun validateName() {
        val name = etName.text.toString().trim()
        isNameValid = name.length >= 3
        
        if (nameTouched) {
            if (isNameValid) {
                ivNameCheck.visibility = View.VISIBLE
                tvNameError.visibility = View.GONE
            } else {
                ivNameCheck.visibility = View.GONE
                tvNameError.visibility = View.VISIBLE
            }
        }
        
        updateSubmitButton()
    }
    
    private fun validateEmail() {
        val email = etEmail.text.toString().trim()
        isEmailValid = email.contains("@") && email.contains(".")
        
        if (emailTouched && email.isNotEmpty()) {
            if (isEmailValid) {
                ivEmailCheck.visibility = View.VISIBLE
                tvEmailError.visibility = View.GONE
            } else {
                ivEmailCheck.visibility = View.GONE
                tvEmailError.visibility = View.VISIBLE
            }
        }
        
        updateSubmitButton()
    }
    
    private fun validatePassword() {
        val password = etPassword.text.toString()
        isPasswordValid = password.length >= 6
        
        if (passwordTouched && password.isNotEmpty()) {
            tvPasswordError.visibility = if (isPasswordValid) View.GONE else View.VISIBLE
        }
        
        // Re-validate confirm password
        if (confirmPasswordTouched) {
            validateConfirmPassword()
        }
        
        updateSubmitButton()
    }
    
    private fun validateConfirmPassword() {
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()
        doPasswordsMatch = password == confirmPassword && confirmPassword.isNotEmpty()
        
        if (confirmPasswordTouched && confirmPassword.isNotEmpty()) {
            tvConfirmPasswordError.visibility = if (doPasswordsMatch) View.GONE else View.VISIBLE
        }
        
        updateSubmitButton()
    }
    
    private fun togglePasswordVisibility(editText: EditText, imageView: ImageView) {
        if (editText.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        editText.setSelection(editText.text.length)
    }
    
    private fun updateSubmitButton() {
        val isFormValid = isNameValid && isEmailValid && isPasswordValid && doPasswordsMatch && termsAccepted
        
        btnSignUp.isEnabled = isFormValid
        
        if (isFormValid) {
            btnSignUp.backgroundTintList = resources.getColorStateList(R.color.primary_dark, null)
            btnSignUp.setTextColor(resources.getColor(R.color.white, null))
        } else {
            btnSignUp.backgroundTintList = resources.getColorStateList(R.color.gray_200, null)
            btnSignUp.setTextColor(resources.getColor(R.color.gray_400, null))
        }
    }
    
    private fun handleSignUp() {
        // Mark all fields as touched
        nameTouched = true
        emailTouched = true
        passwordTouched = true
        confirmPasswordTouched = true
        
        // Re-validate all fields
        validateName()
        validateEmail()
        validatePassword()
        validateConfirmPassword()
        
        if (!isNameValid || !isEmailValid || !isPasswordValid || !doPasswordsMatch || !termsAccepted) {
            Toast.makeText(this, "Por favor completa todos los campos correctamente", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get values
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        
        // Navigate to Bienvenida
        val intent = Intent(this, BienvenidaActivity::class.java)
        intent.putExtra("USER_NAME", name)
        intent.putExtra("USER_EMAIL", email)
        startActivity(intent)
        finish()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
