package com.example.finance

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.finance.databinding.ActivityEditarPerfilBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class EditarPerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditarPerfilBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Datos originales del usuario
    private var nombreOriginal: String = ""
    private var emailOriginal: String = ""

    // Estado del formulario
    private var hayCambios: Boolean = false
    private var nombreValido: Boolean = true  // Inicialmente true
    private var emailValido: Boolean = true   // Inicialmente true
    private var passwordValida: Boolean = true // Por defecto true porque es opcional
    private var passwordsCoinciden: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditarPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Verificar sesión
        if (auth.currentUser == null) {
            finish()
            return
        }

        cargarDatosUsuario()
        setupClickListeners()
        setupTextWatchers()
        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBack()
            }
        })
    }

    private fun cargarDatosUsuario() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Cargar email
        emailOriginal = currentUser.email ?: ""
        binding.etEmail.setText(emailOriginal)

        // Cargar nombre
        nombreOriginal = currentUser.displayName ?: ""

        // Si no hay displayName, cargar desde Firestore
        if (nombreOriginal.isEmpty()) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        nombreOriginal = document.getString("nombre") ?: ""
                        binding.etNombre.setText(nombreOriginal)
                    }
                }
                .addOnFailureListener {
                    showToast("Error al cargar los datos del perfil")
                }
        } else {
            binding.etNombre.setText(nombreOriginal)
        }
    }

    private fun setupClickListeners() {
        // Botón volver
        binding.btnBack.setOnClickListener {
            handleBack()
        }

        // Botón cancelar
        binding.btnCancelar.setOnClickListener {
            handleCancelar()
        }

        // Botón guardar
        binding.btnGuardar.setOnClickListener {
            handleGuardar()
        }

        // Diálogo de cancelar - continuar editando
        binding.btnContinuarEditando.setOnClickListener {
            ocultarDialogoCancelar()
        }

        // Diálogo de cancelar - descartar
        binding.btnDescartar.setOnClickListener {
            finish()
        }
    }

    private fun setupTextWatchers() {
        // TextWatcher para el nombre
        binding.etNombre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validarNombre()
                verificarCambios()
                actualizarEstadoBotonGuardar()
            }
        })

        // TextWatcher para el email
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validarEmail()
                verificarCambios()
                actualizarEstadoBotonGuardar()
            }
        })

        // TextWatcher para nueva contraseña
        binding.etNuevaPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validarPassword()
                mostrarRequisitosPassword()
                verificarCambios()
                actualizarEstadoBotonGuardar()
            }
        })

        // TextWatcher para confirmar contraseña
        binding.etConfirmarPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validarConfirmarPassword()
                mostrarRequisitosPassword()
                verificarCambios()
                actualizarEstadoBotonGuardar()
            }
        })
    }

    private fun validarNombre() {
        val nombre = binding.etNombre.text.toString().trim()

        when {
            nombre.isEmpty() -> {
                binding.tilNombre.error = "El nombre es obligatorio"
                binding.tilNombre.endIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_alert_circle)
                binding.tilNombre.setEndIconTintList(ContextCompat.getColorStateList(this, R.color.error_red))
                nombreValido = false
            }
            nombre.length < 3 -> {
                binding.tilNombre.error = "El nombre debe tener al menos 3 caracteres"
                binding.tilNombre.endIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_alert_circle)
                binding.tilNombre.setEndIconTintList(ContextCompat.getColorStateList(this, R.color.error_red))
                nombreValido = false
            }
            else -> {
                binding.tilNombre.error = null
                binding.tilNombre.endIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_check)
                binding.tilNombre.setEndIconTintList(ContextCompat.getColorStateList(this, R.color.success_green))
                nombreValido = true
            }
        }
    }

    private fun validarEmail() {
        val email = binding.etEmail.text.toString().trim()
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"

        when {
            email.isEmpty() -> {
                binding.tilEmail.error = "El email es obligatorio"
                binding.tilEmail.endIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_alert_circle)
                binding.tilEmail.setEndIconTintList(ContextCompat.getColorStateList(this, R.color.error_red))
                emailValido = false
            }
            !email.matches(emailPattern.toRegex()) -> {
                binding.tilEmail.error = "Ingresa un email válido"
                binding.tilEmail.endIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_alert_circle)
                binding.tilEmail.setEndIconTintList(ContextCompat.getColorStateList(this, R.color.error_red))
                emailValido = false
            }
            else -> {
                binding.tilEmail.error = null
                binding.tilEmail.endIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_check)
                binding.tilEmail.setEndIconTintList(ContextCompat.getColorStateList(this, R.color.success_green))
                emailValido = true
            }
        }
    }

    private fun validarPassword() {
        val password = binding.etNuevaPassword.text.toString()

        when {
            password.isEmpty() -> {
                binding.tilNuevaPassword.error = null
                passwordValida = true
            }
            password.length < 6 -> {
                binding.tilNuevaPassword.error = "La contraseña debe tener al menos 6 caracteres"
                passwordValida = false
            }
            else -> {
                binding.tilNuevaPassword.error = null
                passwordValida = true
            }
        }

        // También validar que coincidan
        validarConfirmarPassword()
    }

    private fun validarConfirmarPassword() {
        val password = binding.etNuevaPassword.text.toString()
        val confirmarPassword = binding.etConfirmarPassword.text.toString()

        when {
            password.isEmpty() && confirmarPassword.isEmpty() -> {
                binding.tilConfirmarPassword.error = null
                passwordsCoinciden = true
            }
            password != confirmarPassword -> {
                binding.tilConfirmarPassword.error = "Las contraseñas no coinciden"
                passwordsCoinciden = false
            }
            else -> {
                binding.tilConfirmarPassword.error = null
                passwordsCoinciden = true
            }
        }
    }

    private fun mostrarRequisitosPassword() {
        val password = binding.etNuevaPassword.text.toString()
        val confirmarPassword = binding.etConfirmarPassword.text.toString()

        if (password.isNotEmpty() || confirmarPassword.isNotEmpty()) {
            binding.cardPasswordRequirements.visibility = View.VISIBLE

            // Requisito 1: Mínimo 6 caracteres
            val cumpleMinLength = password.length >= 6
            if (cumpleMinLength) {
                binding.iconMinLength.setBackgroundColor(ContextCompat.getColor(this, R.color.success_green))
                binding.checkMinLength.visibility = View.VISIBLE
                binding.tvMinLength.setTextColor(ContextCompat.getColor(this, R.color.success_green))
            } else {
                binding.iconMinLength.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_300))
                binding.checkMinLength.visibility = View.GONE
                binding.tvMinLength.setTextColor(ContextCompat.getColor(this, R.color.gray_600))
            }

            // Requisito 2: Las contraseñas coinciden
            val cumpleMatch = password == confirmarPassword && password.isNotEmpty()
            if (cumpleMatch) {
                binding.iconPasswordMatch.setBackgroundColor(ContextCompat.getColor(this, R.color.success_green))
                binding.checkPasswordMatch.visibility = View.VISIBLE
                binding.tvPasswordMatch.setTextColor(ContextCompat.getColor(this, R.color.success_green))
            } else {
                binding.iconPasswordMatch.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_300))
                binding.checkPasswordMatch.visibility = View.GONE
                binding.tvPasswordMatch.setTextColor(ContextCompat.getColor(this, R.color.gray_600))
            }
        } else {
            binding.cardPasswordRequirements.visibility = View.GONE
        }
    }

    private fun verificarCambios() {
        val nombre = binding.etNombre.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val nuevaPassword = binding.etNuevaPassword.text.toString()
        val confirmarPassword = binding.etConfirmarPassword.text.toString()

        hayCambios = nombre != nombreOriginal ||
                email != emailOriginal ||
                nuevaPassword.isNotEmpty() ||
                confirmarPassword.isNotEmpty()
    }

    private fun actualizarEstadoBotonGuardar() {
        // Validar solo los campos que fueron modificados
        val nombre = binding.etNombre.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val nuevaPassword = binding.etNuevaPassword.text.toString()
        val confirmarPassword = binding.etConfirmarPassword.text.toString()
        
        // Verificar si cada campo modificado es válido
        val nombreCambiadoYValido = nombre != nombreOriginal && nombreValido
        val emailCambiadoYValido = email != emailOriginal && emailValido
        val passwordCambiadaYValida = (nuevaPassword.isNotEmpty() || confirmarPassword.isNotEmpty()) && passwordValida && passwordsCoinciden
        
        // El formulario es válido si al menos un campo cambió y es válido
        val algunCambioValido = nombreCambiadoYValido || emailCambiadoYValido || passwordCambiadaYValida
        
        // También verificar que los campos que NO cambiaron sigan siendo válidos
        val camposNoModificadosValidos = 
            (nombre == nombreOriginal || nombreValido) &&
            (email == emailOriginal || emailValido) &&
            (nuevaPassword.isEmpty() && confirmarPassword.isEmpty() || (passwordValida && passwordsCoinciden))
        
        binding.btnGuardar.isEnabled = hayCambios && algunCambioValido && camposNoModificadosValidos

        if (binding.btnGuardar.isEnabled) {
            binding.btnGuardar.backgroundTintList = ContextCompat.getColorStateList(this, R.color.teal_500)
        } else {
            binding.btnGuardar.backgroundTintList = ContextCompat.getColorStateList(this, R.color.gray_200)
        }
    }

    private fun handleBack() {
        if (hayCambios) {
            mostrarDialogoCancelar()
        } else {
            finish()
        }
    }

    private fun handleCancelar() {
        if (hayCambios) {
            mostrarDialogoCancelar()
        } else {
            finish()
        }
    }

    private fun handleGuardar() {
        // Validar todos los campos
        validarNombre()
        validarEmail()
        validarPassword()
        validarConfirmarPassword()

        val formularioValido = nombreValido && emailValido && passwordValida && passwordsCoinciden

        if (!formularioValido) {
            showToast("Por favor corrige los errores en el formulario")
            return
        }

        val nombre = binding.etNombre.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val nuevaPassword = binding.etNuevaPassword.text.toString()

        // Deshabilitar botón mientras se guarda
        binding.btnGuardar.isEnabled = false

        // Contador de operaciones pendientes
        var operacionesPendientes = 0
        var operacionesExitosas = 0
        
        // Contar operaciones a realizar
        if (nombre != nombreOriginal) operacionesPendientes++
        if (email != emailOriginal) operacionesPendientes++
        if (nuevaPassword.isNotEmpty()) operacionesPendientes++
        
        // Función para verificar si todas las operaciones terminaron
        fun verificarCompletado() {
            operacionesExitosas++
            if (operacionesExitosas >= operacionesPendientes) {
                mostrarExitoYRegresar()
            }
        }

        // Actualizar nombre
        if (nombre != nombreOriginal) {
            actualizarNombre(nombre) { exito ->
                if (exito) verificarCompletado()
                else binding.btnGuardar.isEnabled = true
            }
        }

        // Actualizar email
        if (email != emailOriginal) {
            actualizarEmail(email) { exito ->
                if (exito) verificarCompletado()
                else binding.btnGuardar.isEnabled = true
            }
        }

        // Actualizar contraseña si se proporcionó
        if (nuevaPassword.isNotEmpty()) {
            actualizarPassword(nuevaPassword) { exito ->
                if (exito) verificarCompletado()
                else binding.btnGuardar.isEnabled = true
            }
        }
    }

    private fun actualizarNombre(nuevoNombre: String, callback: (Boolean) -> Unit) {
        val currentUser = auth.currentUser ?: run {
            callback(false)
            return
        }
        val userId = currentUser.uid

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(nuevoNombre)
            .build()

        currentUser.updateProfile(profileUpdates)
            .addOnSuccessListener {
                // Actualizar en Firestore
                db.collection("users").document(userId)
                    .update("nombre", nuevoNombre)
                    .addOnSuccessListener {
                        nombreOriginal = nuevoNombre
                        verificarCambios()
                        callback(true)
                    }
                    .addOnFailureListener {
                        showToast("Error al actualizar el nombre en la base de datos")
                        callback(false)
                    }
            }
            .addOnFailureListener {
                showToast("Error al actualizar el nombre")
                callback(false)
            }
    }

    private fun actualizarEmail(nuevoEmail: String, callback: (Boolean) -> Unit) {
        val currentUser = auth.currentUser ?: run {
            callback(false)
            return
        }

        currentUser.updateEmail(nuevoEmail)
            .addOnSuccessListener {
                emailOriginal = nuevoEmail
                verificarCambios()
                callback(true)
            }
            .addOnFailureListener { exception ->
                showToast("Error al actualizar el email: ${exception.message}")
                callback(false)
            }
    }

    private fun actualizarPassword(nuevaPassword: String, callback: (Boolean) -> Unit) {
        val currentUser = auth.currentUser ?: run {
            callback(false)
            return
        }

        currentUser.updatePassword(nuevaPassword)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener { exception ->
                // Si falla por reautenticación requerida
                if (exception.message?.contains("requires recent authentication") == true) {
                    showToast("Por favor, vuelve a iniciar sesión para cambiar tu contraseña")
                } else {
                    showToast("Error al actualizar la contraseña: ${exception.message}")
                }
                callback(false)
            }
    }

    private fun mostrarExitoYRegresar() {
        binding.cardSuccessMessage.visibility = View.VISIBLE

        // Scroll al inicio para mostrar el mensaje
        binding.scrollView.smoothScrollTo(0, 0)

        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 1500)
    }

    private fun mostrarDialogoCancelar() {
        binding.dialogOverlay.visibility = View.VISIBLE
    }

    private fun ocultarDialogoCancelar() {
        binding.dialogOverlay.visibility = View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
