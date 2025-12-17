package com.example.finance

import android.app.Activity
import android.content.Intent
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
import com.google.firebase.firestore.SetOptions

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

        // Ocultar campos de nombre y email - solo editar contraseña
        binding.tilNombre.visibility = View.GONE
        binding.tvNombreLabel.visibility = View.GONE
        binding.layoutEmail.visibility = View.GONE
        binding.tvEmailLabel.visibility = View.GONE
        binding.separatorPersonalInfo.visibility = View.GONE
        
        // Actualizar título
        binding.tvTitle.text = "Cambiar contraseña"
        binding.tvSubtitle.text = "Actualiza tu contraseña de acceso"
        
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

        // Cargar email (campo inmutable)
        emailOriginal = currentUser.email ?: ""
        binding.tvEmail.text = emailOriginal

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
        // Email no se puede editar, siempre es válido
        emailValido = true
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
        val nuevaPassword = binding.etNuevaPassword.text.toString()
        val confirmarPassword = binding.etConfirmarPassword.text.toString()

        hayCambios = nuevaPassword.isNotEmpty() || confirmarPassword.isNotEmpty()
    }

    private fun actualizarEstadoBotonGuardar() {
        // El botón se habilita si hay cambios y la contraseña es válida
        val todosLosCamposValidos = passwordValida && passwordsCoinciden
        
        binding.btnGuardar.isEnabled = hayCambios && todosLosCamposValidos

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
        // Validar contraseña
        validarPassword()
        validarConfirmarPassword()

        val formularioValido = passwordValida && passwordsCoinciden

        if (!formularioValido) {
            showToast("Por favor corrige los errores en el formulario")
            return
        }

        val nuevaPassword = binding.etNuevaPassword.text.toString()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            showToast("Error: No hay sesión activa")
            return
        }

        if (nuevaPassword.isEmpty()) {
            showToast("Debes ingresar una nueva contraseña")
            return
        }

        // Deshabilitar botón
        binding.btnGuardar.isEnabled = false

        // Solicitar contraseña actual para reautenticación
        mostrarDialogoPasswordActual { passwordActual ->
            if (passwordActual != null) {
                reautenticarYCambiarPassword(nuevaPassword, passwordActual)
            } else {
                binding.btnGuardar.isEnabled = true
            }
        }
    }

    private fun reautenticarYCambiarPassword(nuevaPassword: String, passwordActual: String) {
        val currentUser = auth.currentUser ?: return
        val email = currentUser.email ?: ""
        val credential = EmailAuthProvider.getCredential(email, passwordActual)

        currentUser.reauthenticate(credential)
            .addOnSuccessListener {
                // Reautenticación exitosa, cambiar contraseña
                actualizarPassword(nuevaPassword) { exito ->
                    if (exito) {
                        mostrarExitoYRegresar()
                    } else {
                        binding.btnGuardar.isEnabled = true
                    }
                }
            }
            .addOnFailureListener { exception ->
                binding.btnGuardar.isEnabled = true
                showToast("Contraseña actual incorrecta")
            }
    }

    private fun mostrarDialogoPasswordActual(callback: (String?) -> Unit) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Confirmación requerida")
        builder.setMessage("Para cambiar tu email o contraseña, ingresa tu contraseña actual:")

        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton("Confirmar") { dialog, _ ->
            val password = input.text.toString()
            if (password.isNotEmpty()) {
                callback(password)
            } else {
                showToast("Debes ingresar tu contraseña actual")
                callback(null)
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            callback(null)
            dialog.dismiss()
        }

        builder.show()
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
                // Intentar actualizar en Firestore (crear documento si no existe)
                val userData = hashMapOf(
                    "nombre" to nuevoNombre,
                    "email" to currentUser.email
                )
                
                db.collection("users").document(userId)
                    .set(userData, SetOptions.merge())
                    .addOnSuccessListener {
                        nombreOriginal = nuevoNombre
                        verificarCambios()
                        callback(true)
                    }
                    .addOnFailureListener {
                        // Aunque falle Firestore, el nombre se actualizó en Auth
                        nombreOriginal = nuevoNombre
                        verificarCambios()
                        callback(true)
                    }
            }
            .addOnFailureListener {
                showToast("Error al actualizar el nombre")
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
                showToast("Error al actualizar la contraseña: ${exception.message}")
                callback(false)
            }
    }

    private fun mostrarExitoYRegresar() {
        // Asegurarse de que la tarjeta de éxito sea visible
        binding.cardSuccessMessage.visibility = View.VISIBLE
        binding.cardSuccessMessage.alpha = 0f
        binding.cardSuccessMessage.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        // Scroll al inicio para mostrar el mensaje
        binding.scrollView.smoothScrollTo(0, 0)

        Handler(Looper.getMainLooper()).postDelayed({
            // Regresar a UsuarioActivity con resultado exitoso
            setResult(Activity.RESULT_OK)
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
