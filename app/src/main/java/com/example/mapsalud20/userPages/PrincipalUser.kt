package com.example.mapsalud20.userPages

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsalud20.ConfiguracionApp
import com.example.mapsalud20.EditarPerfil
import com.example.mapsalud20.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.mapsalud20.databinding.UserPrincipalBinding
import com.google.android.material.snackbar.Snackbar


class PrincipalUser : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: UserPrincipalBinding
    private lateinit var mMap: GoogleMap

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        verificarSnackbar(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UserPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.mapContainer, mapFragment)
            .commit()

        mapFragment.getMapAsync(this)

        initListeners()
        verificarSnackbar(intent)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val ubicacionCentral = LatLng(-0.201326, -78.507298)
        val centroIñaquito = LatLng(-0.198326, -78.497298)

        mMap.addMarker(
            MarkerOptions()
                .position(centroIñaquito)
                .title("Centro de Salud Iñaquito")
                .snippet("Av. Amazonas y Gaspar de Villarroel\nAbierto 24h")
        )

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionCentral, 14f))

        mMap.setOnMarkerClickListener { marker ->
            binding.cardInfoHospital.visibility = View.VISIBLE
            binding.txtNombreHospitalCuadro.text = marker.title
            true
        }

        mMap.setOnMapClickListener {
            binding.cardInfoHospital.visibility = View.GONE
        }

        binding.cardInfoHospital.setOnClickListener {
            val intent = Intent(this, DatosHospital::class.java)
            intent.putExtra("NOMBRE_HOSPITAL", binding.txtNombreHospitalCuadro.text.toString())
            startActivity(intent)
        }
    }

    private fun initListeners() {
        binding.imgProfile.setOnClickListener {
            startActivity(Intent(this, EditarPerfil::class.java))
        }

        binding.btnConfiguracion.setOnClickListener {
            startActivity(Intent(this, ConfiguracionApp::class.java))
        }

        binding.btnZoomIn.setOnClickListener {
            if(::mMap.isInitialized) mMap.animateCamera(CameraUpdateFactory.zoomIn())
        }

        binding.btnZoomOut.setOnClickListener {
            if(::mMap.isInitialized) mMap.animateCamera(CameraUpdateFactory.zoomOut())
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