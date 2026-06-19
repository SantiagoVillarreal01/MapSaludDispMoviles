package ec.edu.mapsalud.userPages

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ec.edu.mapsalud.ConfiguracionApp
import ec.edu.mapsalud.EditarPerfil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import ec.edu.mapsalud.R
import ec.edu.mapsalud.databinding.UserPrincipalBinding
import ec.edu.mapsalud.dto.MedicalCenterDtoRemote
import ec.edu.mapsalud.dto.Paciente
import ec.edu.mapsalud.remote.impl.MedicalCenterRemoteImpl
import ec.edu.mapsalud.remote.inter.MedicalCenterRemote
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices


class PrincipalUser : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: UserPrincipalBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val centerRepository: MedicalCenterRemote = MedicalCenterRemoteImpl()
    private var centroSeleccionado: MedicalCenterDtoRemote? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        verificarSnackbar(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UserPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapContainer) as? SupportMapFragment

        mapFragment?.getMapAsync(this) ?: run {
            Log.e("MAP_ERROR", "El FragmentContainerView no se pudo encontrar o castear")
        }

        initListeners()
        cargarDatosUsuario()
        verificarSnackbar(intent)
    }

    private fun cargarDatosUsuario() {
        val uid = auth.currentUser?.uid ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Buscamos el documento del usuario en la colección "users"
                val document = db.collection("usuarios").document(uid).get().await()

                // Lo mapeamos directamente a tu nueva Data Class Paciente
                val paciente = document.toObject(Paciente::class.java)

                withContext(Dispatchers.Main) {
                    if (paciente != null) {
                        // Accedemos a los datos usando la nueva estructura anidada (paciente.info...)
                        val nombres = paciente.info.nombres.trim()
                        val apellidos = paciente.info.apellidos.trim()

                        // Extraemos solo el primer nombre para un saludo más amigable en la UI
                        val primerNombre = nombres.split(" ").firstOrNull() ?: ""
                        val primerApellido = apellidos.split(" ").firstOrNull() ?: ""

                        binding.txtUserName.text = "$primerNombre $primerApellido"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.txtUserName.text = "Paciente"
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        cargarCentrosEnMapa()

        configurarUbicacionEnTiempoReal()

        mMap.setOnMarkerClickListener { marker ->
            val centro = marker.tag as? MedicalCenterDtoRemote
            if (centro != null) {
                centroSeleccionado = centro
                binding.cardInfoHospital.visibility = View.VISIBLE
                binding.txtNombreHospitalCuadro.text = centro.name
                binding.txtDireccionHospitalCuadro.text = centro.address
            }
            true
        }

        mMap.setOnMapClickListener {
            binding.cardInfoHospital.visibility = View.GONE
            centroSeleccionado = null
        }

        binding.cardInfoHospital.setOnClickListener {
            centroSeleccionado?.let { centro ->
                val intent = Intent(this, DatosHospital::class.java)
                intent.putExtra("ID_CENTRO", centro.id)
                intent.putExtra("NOMBRE_HOSPITAL", centro.name)
                startActivity(intent)
            }
        }
    }

    private fun cargarCentrosEnMapa() {
        lifecycleScope.launch(Dispatchers.Main) {

            val result = withContext(Dispatchers.IO) {
                centerRepository.getAllCenters()
            }

            result.onSuccess { listaCentros ->
                if (listaCentros.isEmpty()) {
                    Toast.makeText(this@PrincipalUser, "No hay centros registrados", Toast.LENGTH_SHORT).show()
                    return@onSuccess
                }
                for (centro in listaCentros) {
                    val latLng = LatLng(centro.latitude, centro.longitude)
                    val marker = mMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(centro.name)
                    )
                    marker?.tag = centro
                }
            }.onFailure { error ->
                Toast.makeText(this@PrincipalUser, "Error al cargar el mapa: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun configurarUbicacionEnTiempoReal() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1000
            )
            return
        }

        mMap.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLocation = LatLng(location.latitude, location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
            } else {
                val ubicacionPorDefecto = LatLng(-0.201326, -78.507298)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionPorDefecto, 13f))
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            configurarUbicacionEnTiempoReal()
        }
    }

    private fun initListeners() {
        binding.imgProfile.setOnClickListener {
            startActivity(Intent(this, EditarPerfil::class.java))
        }

        binding.btnConfiguracion.setOnClickListener {
            startActivity(Intent(this, ConfiguracionApp::class.java))
        }

        binding.btnMenuNuevaCita.setOnClickListener {
            startActivity(Intent(this, NuevaCita::class.java))
        }

        binding.btnMenuReagendar.setOnClickListener {
            startActivity(Intent(this, ReagendarCita::class.java))
        }

        binding.btnMenuDiagnosticos.setOnClickListener {
            startActivity(Intent(this, Diagnosticos::class.java))
        }

        binding.btnMenuCancelar.setOnClickListener {
            startActivity(Intent(this, CancelarCita::class.java))
        }
    }

    private fun verificarSnackbar(intent: Intent?) {
        val mensaje = intent?.getStringExtra("MENSAJE_SNACKBAR")
        if (!mensaje.isNullOrEmpty()) {
            Snackbar.make(binding.root, mensaje, Snackbar.LENGTH_LONG)
                .setBackgroundTint(resources.getColor(R.color.teal_700))
                .show()
        }
    }
}