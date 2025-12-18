package com.example.finance

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finance.databinding.ActivityUsuarioBinding
import com.example.finance.dataBase.repository.FinanceRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class UsuarioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsuarioBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var repository: FinanceRepository

    // Launcher para editar perfil
    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Los cambios fueron guardados exitosamente, recargar datos
            loadUserData()
            showToast("Contraseña actualizada correctamente")
        }
    }

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
        repository = (application as FinanceApplication).repository

        // Verificar sesión
        if (auth.currentUser == null) {
            navigateToLogin()
            return
        }

        setupClickListeners()
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
            editProfileLauncher.launch(intent)
        }

        // Botón cerrar sesión
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return
        
        // Recargar datos del usuario desde Firebase para obtener cambios recientes
        currentUser.reload().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = currentUser.uid

                // Obtener email
                userEmail = currentUser.email ?: "usuario@ejemplo.com"
                binding.tvEmail.text = userEmail

                // Obtener nombre de usuario desde Firebase Auth
                userName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Usuario"
                updateUserNameUI()

                // Obtener fecha de creación de la cuenta
                val metadata = currentUser.metadata
                if (metadata != null) {
                    val creationTimestamp = metadata.creationTimestamp
                    memberSince = formatMemberSince(Date(creationTimestamp))
                    updateMemberSinceUI()
                }

                // Cargar estadísticas del usuario desde Room
                loadUserStatistics(userId)
            }
        }
    }

    private fun loadUserStatistics(userId: String) {
        lifecycleScope.launch {
            try {
                // Cargar total de gastos desde Room
                totalGastado = repository.getTotalGastos(userId)
                binding.tvTotalGastado.text = formatCurrency(totalGastado)

                // Cargar total de ingresos desde Room
                totalIngresos = repository.getTotalIngresos(userId)

                // Actualizar ahorro
                updateAhorroTotal()

                // Cargar metas reales desde Room
                loadMetasData(userId)
            } catch (e: Exception) {
                totalGastado = 0.0
                totalIngresos = 0.0
                binding.tvTotalGastado.text = formatCurrency(0.0)
                binding.tvTotalAhorrado.text = formatCurrency(0.0)
                binding.tvMetasCompletadas.text = "0"
                binding.tvMetasActivas.text = "0"
            }
        }
    }

    private fun loadMetasData(userId: String) {
        lifecycleScope.launch {
            try {
                // Obtener todas las metas del usuario
                val todasLasMetas = repository.getAllMetasList(userId)
                
                // Separar metas completadas y activas
                metasCompletadas = todasLasMetas.count { it.ahorrado >= it.objetivo }
                metasActivas = todasLasMetas.count { it.ahorrado < it.objetivo }
                
                // Actualizar UI
                binding.tvMetasCompletadas.text = metasCompletadas.toString()
                binding.tvMetasActivas.text = metasActivas.toString()
            } catch (e: Exception) {
                metasCompletadas = 0
                metasActivas = 0
                binding.tvMetasCompletadas.text = "0"
                binding.tvMetasActivas.text = "0"
            }
        }
    }

    private fun updateAhorroTotal() {
        val totalAhorrado = totalIngresos - totalGastado
        binding.tvTotalAhorrado.text = formatCurrency(if (totalAhorrado >= 0) totalAhorrado else 0.0)
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
