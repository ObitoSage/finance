package com.example.finance

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finance.databinding.ActivityUsuarioBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class UsuarioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsuarioBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Datos del usuario
    private var userName: String = ""
    private var userEmail: String = ""
    private var memberSince: String = ""
    private var totalGastado: Double = 0.0
    private var totalIngresos: Double = 0.0
    private var metasCompletadas: Int = 0
    private var metasActivas: Int = 0

    // Formato de moneda
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsuarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Verificar sesión
        if (auth.currentUser == null) {
            navigateToLogin()
            return
        }

        setupClickListeners()
        loadUserData()
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos cuando regresamos de editar perfil
        loadUserData()
    }

    private fun setupClickListeners() {
        // Botón volver
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Botón editar perfil
        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditarPerfilActivity::class.java)
            startActivity(intent)
        }

        // Botón cerrar sesión
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Obtener email
        userEmail = currentUser.email ?: "usuario@ejemplo.com"
        binding.tvEmail.text = userEmail

        // Obtener nombre de usuario desde Firebase Auth o Firestore
        userName = currentUser.displayName ?: ""

        // Si no hay displayName, cargar desde Firestore
        if (userName.isEmpty()) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userName = document.getString("nombre") ?: "Usuario"
                        updateUserNameUI()
                        
                        // Cargar fecha de registro si existe
                        val timestamp = document.getTimestamp("fechaRegistro")
                        if (timestamp != null) {
                            memberSince = formatMemberSince(timestamp.toDate())
                            updateMemberSinceUI()
                        }
                    } else {
                        userName = "Usuario"
                        updateUserNameUI()
                    }
                }
                .addOnFailureListener {
                    userName = "Usuario"
                    updateUserNameUI()
                }
        } else {
            updateUserNameUI()
        }

        // Obtener fecha de creación de la cuenta
        val metadata = currentUser.metadata
        if (metadata != null) {
            val creationTimestamp = metadata.creationTimestamp
            memberSince = formatMemberSince(Date(creationTimestamp))
            updateMemberSinceUI()
        }

        // Cargar estadísticas del usuario
        loadUserStatistics(userId)
    }

    private fun loadUserStatistics(userId: String) {
        // Cargar total de gastos
        db.collection("gastos")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                totalGastado = documents.sumOf { it.getDouble("monto") ?: 0.0 }
                binding.tvTotalGastado.text = formatCurrency(totalGastado)
                // Actualizar ahorro después de cargar gastos
                updateAhorroTotal()
            }
            .addOnFailureListener {
                totalGastado = 0.0
                binding.tvTotalGastado.text = formatCurrency(0.0)
            }

        // Cargar total de ingresos
        db.collection("ingresos")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                totalIngresos = documents.sumOf { it.getDouble("monto") ?: 0.0 }
                // Actualizar ahorro después de cargar ingresos
                updateAhorroTotal()
            }
            .addOnFailureListener {
                totalIngresos = 0.0
                binding.tvTotalAhorrado.text = formatCurrency(0.0)
            }

        // Cargar metas desde Firestore
        loadMetas(userId)
    }

    private fun updateAhorroTotal() {
        val totalAhorrado = totalIngresos - totalGastado
        binding.tvTotalAhorrado.text = formatCurrency(if (totalAhorrado >= 0) totalAhorrado else 0.0)
    }

    private fun loadMetas(userId: String) {
        // Verificar si existe la colección de metas
        db.collection("metas")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Si no hay metas, mostrar 0
                    metasCompletadas = 0
                    metasActivas = 0
                    binding.tvMetasCompletadas.text = "0"
                    binding.tvMetasActivas.text = "0"
                } else {
                    // Contar metas completadas y activas
                    metasCompletadas = 0
                    metasActivas = 0
                    
                    for (document in documents) {
                        val completada = document.getBoolean("completada") ?: false
                        val activa = document.getBoolean("activa") ?: true
                        
                        if (completada) {
                            metasCompletadas++
                        } else if (activa) {
                            metasActivas++
                        }
                    }
                    
                    binding.tvMetasCompletadas.text = metasCompletadas.toString()
                    binding.tvMetasActivas.text = metasActivas.toString()
                }
            }
            .addOnFailureListener {
                // Si hay error o no existe la colección, mostrar 0
                metasCompletadas = 0
                metasActivas = 0
                binding.tvMetasCompletadas.text = "0"
                binding.tvMetasActivas.text = "0"
            }
    }

    private fun updateUserNameUI() {
        binding.tvUserName.text = userName
        binding.tvFullName.text = userName
    }

    private fun updateMemberSinceUI() {
        binding.tvMemberSince.text = getString(R.string.profile_member_since_value, memberSince)
        binding.tvMemberDate.text = memberSince
    }

    private fun formatMemberSince(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        val months = arrayOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        
        val month = months[calendar.get(Calendar.MONTH)]
        val year = calendar.get(Calendar.YEAR)
        
        return "$month $year"
    }

    private fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount)
            .replace("COP", "$")
            .replace("\u00A0", "")
            .trim()
    }

    private fun showLogoutDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Cerrar sesión")
        builder.setMessage("¿Estás seguro de que quieres cerrar sesión?")
        builder.setPositiveButton("Cerrar sesión") { _, _ ->
            logout()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun logout() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
