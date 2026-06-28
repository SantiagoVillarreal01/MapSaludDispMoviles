package ec.edu.mapsalud.userPages

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import ec.edu.mapsalud.R
import ec.edu.mapsalud.databinding.FragmentHomeMapBinding
import ec.edu.mapsalud.dto.MedicalCenterDtoRemote
import ec.edu.mapsalud.remote.impl.MedicalCenterRemoteImpl
import ec.edu.mapsalud.remote.inter.MedicalCenterRemote
import ec.edu.mapsalud.utils.ThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeMapFragment : Fragment(R.layout.fragment_home_map), OnMapReadyCallback {

    private var _binding: FragmentHomeMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val centerRepository: MedicalCenterRemote = MedicalCenterRemoteImpl()
    private var centroSeleccionado: MedicalCenterDtoRemote? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeMapBinding.bind(view)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainerFull) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        initListeners()
    }

    private fun updateMapStyle(googleMap: GoogleMap) {
        try {
            if (ThemeUtils.isDark(requireContext())) {
                googleMap.setMapStyle(
                    com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(
                        requireContext(), ec.edu.mapsalud.R.raw.map_style_dark
                    )
                )
            } else {
                googleMap.setMapStyle(null) // Estilo normal
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initListeners() {
        binding.cardInfoHospitalMap.setOnClickListener {
            centroSeleccionado?.let { centro ->
                val intent = Intent(requireContext(), DatosHospital::class.java)
                intent.putExtra("ID_CENTRO", centro.id)
                intent.putExtra("NOMBRE_HOSPITAL", centro.name)
                startActivity(intent)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        updateMapStyle(googleMap)

        cargarCentrosEnMapa()
        configurarUbicacionEnTiempoReal()

        mMap.setOnMarkerClickListener { marker ->
            val centro = marker.tag as? MedicalCenterDtoRemote
            if (centro != null) {
                centroSeleccionado = centro
                binding.cardInfoHospitalMap.visibility = View.VISIBLE
                binding.txtNombreHospitalMap.text = centro.name
                binding.txtDireccionHospitalMap.text = centro.address
            }
            true
        }

        mMap.setOnMapClickListener {
            binding.cardInfoHospitalMap.visibility = View.GONE
            centroSeleccionado = null
        }
    }

    private fun cargarCentrosEnMapa() {
        lifecycleScope.launch(Dispatchers.Main) {
            val result = withContext(Dispatchers.IO) {
                centerRepository.getAllCenters()
            }

            result.onSuccess { listaCentros ->
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
                Toast.makeText(requireContext(), "Error al cargar el mapa: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun configurarUbicacionEnTiempoReal() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        mMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLocation = LatLng(location.latitude, location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
