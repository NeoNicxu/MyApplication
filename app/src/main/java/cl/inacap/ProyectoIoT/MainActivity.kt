package cl.inacap.ProyectoIoT

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cl.inacap.ProyectoIoT.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Validar sesión activa
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.tvUserLogged.text = "Operador: ${auth.currentUser?.email}"

        // Conexión al nodo de la base de datos
        dbRef = FirebaseDatabase.getInstance().getReference("puerta_acceso")

        // Monitoreo continuo de sensores
        escucharSensores()

        // Control de cerrojo
        binding.btnLock.setOnClickListener {
            enviarComando("LOCK")
        }

        binding.btnUnlock.setOnClickListener {
            enviarComando("UNLOCK")
        }

        // Cierre de sesión
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun escucharSensores() {
        // Lectura en tiempo real de los switches mecánicos
        dbRef.child("sensores").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val swMarco = snapshot.child("sw_marco").getValue(Boolean::class.java) ?: false
                val swCerrojo = snapshot.child("sw_cerrojo").getValue(Boolean::class.java) ?: false

                actualizarEstadoVisual(swMarco, swCerrojo)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Error de sincronización: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Monitoreo de alerta por puerta abierta prolongada
        dbRef.child("alertas/puerta_olvidada").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val alertaActiva = snapshot.getValue(Boolean::class.java) ?: false
                binding.cardAlerta.visibility = if (alertaActiva) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun actualizarEstadoVisual(swMarco: Boolean, swCerrojo: Boolean) {
        binding.tvSwMarco.text = if (swMarco) "Switch Marco: Hoja ajustada (Cerrado)" else "Switch Marco: Desacoplado (Abierto)"
        binding.tvSwCerrojo.text = if (swCerrojo) "Switch Cerrojo: Pasador asegurado" else "Switch Cerrojo: Sin seguro"

        // Estados del sistema y habilitación de controles
        when {
            !swMarco -> {
                binding.tvEstadoGeneral.text = "PUERTA ABIERTA"
                binding.tvEstadoGeneral.setTextColor(Color.parseColor("#BA1A1A"))
                binding.btnLock.isEnabled = false
                binding.btnUnlock.isEnabled = false
            }
            swMarco && !swCerrojo -> {
                binding.tvEstadoGeneral.text = "ARRIMADA (SIN SEGURO)"
                binding.tvEstadoGeneral.setTextColor(Color.parseColor("#E65100"))
                binding.btnLock.isEnabled = true
                binding.btnUnlock.isEnabled = false
            }
            swMarco && swCerrojo -> {
                binding.tvEstadoGeneral.text = "CERRADA Y BLOQUEADA"
                binding.tvEstadoGeneral.setTextColor(Color.parseColor("#1B5E20"))
                binding.btnLock.isEnabled = false
                binding.btnUnlock.isEnabled = true
            }
        }
    }

    private fun enviarComando(accion: String) {
        // Conversión a formato de fecha local
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val fechaLegible = formato.format(Date())

        // Carga de datos del actuador con auditoría temporal
        val payload = mapOf(
            "comando" to accion,
            "accionado_por" to (auth.currentUser?.email ?: "desconocido"),
            "timestamp" to ServerValue.TIMESTAMP,
            "fecha_hora" to fechaLegible
        )

        dbRef.child("actuador").setValue(payload).addOnSuccessListener {
            Toast.makeText(this, "Comando $accion enviado con éxito", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Error al enviar: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}