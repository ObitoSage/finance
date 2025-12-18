package com.example.finance

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import com.example.finance.databinding.ActivityBienvenidaBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Activity de bienvenida que se muestra después del primer login.
 * Presenta las características de la app y redirige al flujo apropiado.
 */
class BienvenidaActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityBienvenidaBinding
    private lateinit var auth: FirebaseAuth
    
    companion object {
        private const val PREFS_NAME = "FinanceAppPrefs"
        private const val KEY_FIRST_TIME = "isFirstTime"
    }
    
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
        
        // Inicializar vistas con alpha 0 para animaciones
        initializeViewsForAnimation()
        
        setupWelcomeMessage()
        setupListeners()
        
        // Iniciar animaciones después de un pequeño delay
        binding.root.postDelayed({
            startWelcomeAnimations()
        }, 100)
    }
    
    private fun initializeViewsForAnimation() {
        binding.apply {
            cvIcon.alpha = 0f
            cvIcon.translationY = -50f
            
            tvWelcomeTitle.alpha = 0f
            tvWelcomeTitle.translationY = 20f
            
            tvWelcomeSubtitle.alpha = 0f
            tvWelcomeSubtitle.translationY = 20f
            
            llFeatures.alpha = 0f
            llFeatures.translationY = 30f
            
            btnContinue.alpha = 0f
            btnContinue.scaleX = 0.8f
            btnContinue.scaleY = 0.8f
        }
    }
    
    private fun startWelcomeAnimations() {
        // Animación del icono principal
        val iconFadeIn = ObjectAnimator.ofFloat(binding.cvIcon, "alpha", 0f, 1f).apply {
            duration = 600
            interpolator = FastOutSlowInInterpolator()
        }
        val iconSlideIn = ObjectAnimator.ofFloat(binding.cvIcon, "translationY", -50f, 0f).apply {
            duration = 600
            interpolator = OvershootInterpolator()
        }
        
        // Animación del título
        val titleFadeIn = ObjectAnimator.ofFloat(binding.tvWelcomeTitle, "alpha", 0f, 1f).apply {
            duration = 500
            startDelay = 200
            interpolator = FastOutSlowInInterpolator()
        }
        val titleSlideUp = ObjectAnimator.ofFloat(binding.tvWelcomeTitle, "translationY", 20f, 0f).apply {
            duration = 500
            startDelay = 200
            interpolator = FastOutSlowInInterpolator()
        }
        
        // Animación del subtítulo
        val subtitleFadeIn = ObjectAnimator.ofFloat(binding.tvWelcomeSubtitle, "alpha", 0f, 1f).apply {
            duration = 500
            startDelay = 400
            interpolator = FastOutSlowInInterpolator()
        }
        val subtitleSlideUp = ObjectAnimator.ofFloat(binding.tvWelcomeSubtitle, "translationY", 20f, 0f).apply {
            duration = 500
            startDelay = 400
            interpolator = FastOutSlowInInterpolator()
        }
        
        // Animación de características
        val featuresFadeIn = ObjectAnimator.ofFloat(binding.llFeatures, "alpha", 0f, 1f).apply {
            duration = 600
            startDelay = 600
            interpolator = FastOutSlowInInterpolator()
        }
        val featuresSlideUp = ObjectAnimator.ofFloat(binding.llFeatures, "translationY", 30f, 0f).apply {
            duration = 600
            startDelay = 600
            interpolator = FastOutSlowInInterpolator()
        }
        
        // Animación del botón
        val buttonFadeIn = ObjectAnimator.ofFloat(binding.btnContinue, "alpha", 0f, 1f).apply {
            duration = 500
            startDelay = 900
            interpolator = FastOutSlowInInterpolator()
        }
        val buttonScaleX = ObjectAnimator.ofFloat(binding.btnContinue, "scaleX", 0.8f, 1f).apply {
            duration = 500
            startDelay = 900
            interpolator = OvershootInterpolator()
        }
        val buttonScaleY = ObjectAnimator.ofFloat(binding.btnContinue, "scaleY", 0.8f, 1f).apply {
            duration = 500
            startDelay = 900
            interpolator = OvershootInterpolator()
        }
        
        // Ejecutar todas las animaciones
        AnimatorSet().apply {
            playTogether(
                iconFadeIn, iconSlideIn,
                titleFadeIn, titleSlideUp,
                subtitleFadeIn, subtitleSlideUp,
                featuresFadeIn, featuresSlideUp,
                buttonFadeIn, buttonScaleX, buttonScaleY
            )
            start()
        }
    }
    
    private fun setupWelcomeMessage() {
        val user = auth.currentUser
        val userName = user?.displayName ?: user?.email?.substringBefore("@") ?: "Usuario"
        val firstName = userName.split(" ")[0].replaceFirstChar { it.uppercase() }
        binding.tvWelcomeTitle.text = getString(R.string.welcome_title, firstName)
    }
    
    private fun setupListeners() {
        binding.btnContinue.setOnClickListener {
            // Marcar que el usuario ya completó el onboarding
            markOnboardingComplete()
            
            // Animación de salida del botón
            animateButtonPress(it) {
                checkPresupuestoAndNavigate()
            }
        }
    }
    
    /**
     * Verifica si el usuario tiene presupuesto configurado y navega apropiadamente.
     * - Si NO tiene presupuesto: va a ConfigurarPresupuestoInicialActivity
     * - Si SÍ tiene presupuesto: va directamente a DashboardActivity
     */
    private fun checkPresupuestoAndNavigate() {
        val userId = auth.currentUser?.uid ?: return
        val app = application as FinanceApplication
        val repository = app.repository
        
        lifecycleScope.launch {
            try {
                val presupuesto = repository.getPresupuestoMensual(userId)
                
                val intent = if (presupuesto > 0) {
                    // Usuario ya tiene presupuesto, ir directo al Dashboard
                    Intent(this@BienvenidaActivity, DashboardActivity::class.java)
                } else {
                    // Usuario no tiene presupuesto, ir a configurarlo
                    Intent(this@BienvenidaActivity, ConfigurarPresupuestoInicialActivity::class.java)
                }
                
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            } catch (e: Exception) {
                // En caso de error, ir a configurar presupuesto
                val intent = Intent(this@BienvenidaActivity, ConfigurarPresupuestoInicialActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }
    }
    
    private fun animateButtonPress(view: View, onComplete: () -> Unit) {
        val scaleDown = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f),
                ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f)
            )
            duration = 100
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        val scaleUp = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)
            )
            duration = 100
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        scaleDown.start()
        scaleDown.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                scaleUp.start()
                scaleUp.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        onComplete()
                    }
                })
            }
        })
    }
    
    private fun markOnboardingComplete() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FIRST_TIME, false).apply()
    }
    
    private fun isFirstTime(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_FIRST_TIME, true)
    }
}
