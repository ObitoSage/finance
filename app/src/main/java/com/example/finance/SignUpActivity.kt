package com.example.finance

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.finance.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest

class SignUpActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    
    // Estados de validación
    private var isNameValid = false
    private var isEmailValid = false
    private var isPasswordValid = false
    private var doPasswordsMatch = false
    private var termsAccepted = false
    
    // Estados de campos tocados
    private var nameTouched = false
    private var emailTouched = false
    private var passwordTouched = false
    private var confirmPasswordTouched = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        
        setupListeners()
    }
    
    private fun setupListeners() {
        // Botón volver
        binding.btnBack.setOnClickListener {
            navigateToLogin()
        }
        
        // Validación de nombre
        binding.etName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateName()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        binding.etName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                nameTouched = true
                validateName()
            }
        }
        
        // Validación de email
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateEmail()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                emailTouched = true
                validateEmail()
            }
        }
        
        // Validación de contraseña
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validatePassword()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                passwordTouched = true
                validatePassword()
            }
        }
        
        // Validación de confirmar contraseña
        binding.etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateConfirmPassword()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        binding.etConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                confirmPasswordTouched = true
                validateConfirmPassword()
            }
        }
        
        // Mostrar/ocultar contraseña
        binding.ivTogglePassword.setOnClickListener {
            togglePasswordVisibility(binding.etPassword, binding.ivTogglePassword)
        }
        
        binding.ivToggleConfirmPassword.setOnClickListener {
            togglePasswordVisibility(binding.etConfirmPassword, binding.ivToggleConfirmPassword)
        }
        
        // Checkbox de términos
        binding.cbTerms.setOnCheckedChangeListener { _, isChecked ->
            termsAccepted = isChecked
            updateSubmitButton()
        }
        
        // Botón de registro
        binding.btnSignUp.setOnClickListener {
            handleSignUp()
        }
        
        // Ir a login
        binding.tvGoToLogin.setOnClickListener {
            navigateToLogin()
        }
    }
    
    private fun validateName() {
        val name = binding.etName.text.toString().trim()
        isNameValid = name.length >= 3
        
        if (nameTouched) {
            if (isNameValid) {
                binding.ivNameCheck.visibility = View.VISIBLE
                binding.tvNameError.visibility = View.GONE
            } else {
                binding.ivNameCheck.visibility = View.GONE
                binding.tvNameError.visibility = View.VISIBLE
            }
        }
        
        updateSubmitButton()
    }
    
    private fun validateEmail() {
        val email = binding.etEmail.text.toString().trim()
        isEmailValid = email.contains("@") && email.contains(".")
        
        if (emailTouched && email.isNotEmpty()) {
            if (isEmailValid) {
                binding.ivEmailCheck.visibility = View.VISIBLE
                binding.tvEmailError.visibility = View.GONE
            } else {
                binding.ivEmailCheck.visibility = View.GONE
                binding.tvEmailError.visibility = View.VISIBLE
            }
        }
        
        updateSubmitButton()
    }
    
    private fun validatePassword() {
        val password = binding.etPassword.text.toString()
        isPasswordValid = password.length >= 6
        
        if (passwordTouched && password.isNotEmpty()) {
            binding.tvPasswordError.visibility = if (isPasswordValid) View.GONE else View.VISIBLE
        }
        
        // Re-validar confirmar contraseña
        if (confirmPasswordTouched) {
            validateConfirmPassword()
        }
        
        updateSubmitButton()
    }
    
    private fun validateConfirmPassword() {
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        doPasswordsMatch = password == confirmPassword && confirmPassword.isNotEmpty()
        
        if (confirmPasswordTouched && confirmPassword.isNotEmpty()) {
            binding.tvConfirmPasswordError.visibility = if (doPasswordsMatch) View.GONE else View.VISIBLE
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
        
        binding.btnSignUp.isEnabled = isFormValid
        
        if (isFormValid) {
            binding.btnSignUp.backgroundTintList = resources.getColorStateList(R.color.primary_dark, null)
            binding.btnSignUp.setTextColor(resources.getColor(R.color.white, null))
        } else {
            binding.btnSignUp.backgroundTintList = resources.getColorStateList(R.color.gray_200, null)
            binding.btnSignUp.setTextColor(resources.getColor(R.color.gray_400, null))
        }
    }
    
    private fun handleSignUp() {
        // Marcar todos los campos como tocados
        nameTouched = true
        emailTouched = true
        passwordTouched = true
        confirmPasswordTouched = true
        
        // Re-validar todos los campos
        validateName()
        validateEmail()
        validatePassword()
        validateConfirmPassword()
        
        if (!isNameValid || !isEmailValid || !isPasswordValid || !doPasswordsMatch || !termsAccepted) {
            Toast.makeText(this, "Por favor completa todos los campos correctamente", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Obtener valores
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
        // Desactivar botón durante registro
        binding.btnSignUp.isEnabled = false
        
        // Registrar con Firebase
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Actualizar perfil con nombre
                    val user = auth.currentUser
                    val profileUpdates = userProfileChangeRequest {
                        displayName = name
                    }
                    
                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            binding.btnSignUp.isEnabled = true
                            if (profileTask.isSuccessful) {
                                Toast.makeText(this, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show()
                                navigateToBienvenida()
                            } else {
                                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                navigateToBienvenida()
                            }
                        }
                } else {
                    binding.btnSignUp.isEnabled = true
                    val errorMessage = task.exception?.message ?: "Error al registrar usuario"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }
    
    private fun navigateToBienvenida() {
        val intent = Intent(this, BienvenidaActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
