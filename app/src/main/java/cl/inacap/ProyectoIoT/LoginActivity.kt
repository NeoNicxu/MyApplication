package cl.inacap.ProyectoIoT

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cl.inacap.ProyectoIoT.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Si ya hay sesión activa previa, ingresa directamente
        if (auth.currentUser != null) {
            irAPantallaPrincipal()
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Debe completar correo y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Bloquear interfaz mientras autentica
            mostrarCarga(true)

            // Autenticación segura bajo TLS con Firebase Auth (ISO 27400)
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    mostrarCarga(false)
                    Toast.makeText(this, "Acceso autorizado", Toast.LENGTH_SHORT).show()
                    irAPantallaPrincipal()
                }
                .addOnFailureListener { exception ->
                    mostrarCarga(false)
                    Toast.makeText(this, "Fallo de autenticación: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun mostrarCarga(cargando: Boolean) {
        binding.btnLogin.isEnabled = !cargando
        binding.progressBar.visibility = if (cargando) View.VISIBLE else View.GONE
    }

    private fun irAPantallaPrincipal() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}