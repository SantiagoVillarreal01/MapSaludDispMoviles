package ec.edu.mapsalud.userPages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ec.edu.mapsalud.databinding.CuadroCentroMedicoBinding
import ec.edu.mapsalud.databinding.UserNuevaCitaBinding
import ec.edu.mapsalud.dto.CenterWithDistance
import ec.edu.mapsalud.remote.impl.CentroMedicoRepositoryImpl
import ec.edu.mapsalud.utils.ThemeUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ec.edu.mapsalud.enum.CenterType
import ec.edu.mapsalud.enum.Specialty
import java.util.Locale
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import ec.edu.mapsalud.R
import ec.edu.mapsalud.usercases.centrosUC.GetCentersFilteredUC
import ec.edu.mapsalud.viewmodel.CentroMedicoViewModel

class NuevaCitaFragment : Fragment(R.layout.user_nueva_cita) {

    private var _binding: UserNuevaCitaBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CentroMedicoAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val centroMedicoVM by viewModels<CentroMedicoViewModel>()
    private val centroMedicoRepository = CentroMedicoRepositoryImpl()

    private var userLat: Double = -0.180653
    private var userLon: Double = -78.467834

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val grantedFine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val grantedCoarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (grantedFine || grantedCoarse) {
            obtenerUbicacionActual()
        } else {
            Toast.makeText(requireContext(), "Permiso denegado. Usando ubicación predeterminada.", Toast.LENGTH_LONG).show()
            cargarCentrosFiltrados()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = UserNuevaCitaBinding.bind(view)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        configurarRecyclerView()
        configurarFiltros()
        initObservers()
        verificarPermisosYGeolocalizar()
        configurarMapaPreview()
    }

    private fun configurarMapaPreview() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapPreviewContainer) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            googleMap.uiSettings.setAllGesturesEnabled(false) // No interactuable
            
            // Aplicar estilo oscuro si es necesario
            if (ThemeUtils.isDark(requireContext())) {
                googleMap.setMapStyle(com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(requireContext(), ec.edu.mapsalud.R.raw.map_style_dark))
            }

            val currentPos = LatLng(userLat, userLon)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPos, 14f))
        }

        binding.mapOverlay.setOnClickListener {
            // Navegar al Fragmento del Mapa (HomeMapFragment)
            (activity as? PrincipalUser)?.let { principal ->
                principal.cambiarFragment(HomeMapFragment())
                // No actualizamos estilo menu aquí si no hay botón específico para el mapa "puro"
                // O podríamos tener el mapa como una opción oculta o secundaria.
            }
        }
    }

    private fun verificarPermisosYGeolocalizar() {
        val hasFine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            obtenerUbicacionActual()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacionActual() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                userLat = location.latitude
                userLon = location.longitude
                actualizarMapaPreview()
            } else {
                Toast.makeText(requireContext(), "No se pudo obtener la ubicación actual.", Toast.LENGTH_SHORT).show()
            }
            cargarCentrosFiltrados()
        }.addOnFailureListener {
            cargarCentrosFiltrados()
        }
    }

    private fun actualizarMapaPreview() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapPreviewContainer) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            // Aplicar estilo oscuro si es necesario
            if (ThemeUtils.isDark(requireContext())) {
                googleMap.setMapStyle(com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(requireContext(), ec.edu.mapsalud.R.raw.map_style_dark))
            }
            val currentPos = LatLng(userLat, userLon)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPos, 14f))
        }
    }

    private fun configurarFiltros() {
        val tiposCentro = mutableListOf("Todos")
        tiposCentro.addAll(CenterType.values().map { it.nombreMostrar })

        val adapterTipos = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tiposCentro)
        binding.autoCompleteTipoCentro.setAdapter(adapterTipos)
        binding.autoCompleteTipoCentro.setText("Todos", false)

        val especialidades = mutableListOf("Todas")
        especialidades.addAll(Specialty.values().map { it.nombreMostrar }.sorted())

        val adapterEspecialidades = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, especialidades)
        binding.autoCompleteEspecialidad.setAdapter(adapterEspecialidades)
        binding.autoCompleteEspecialidad.setText("Todas", false)

        binding.autoCompleteTipoCentro.setOnItemClickListener { _, _, _, _ -> cargarCentrosFiltrados() }
        binding.autoCompleteEspecialidad.setOnItemClickListener { _, _, _, _ -> cargarCentrosFiltrados() }
    }

    private fun configurarRecyclerView() {
        adapter = CentroMedicoAdapter(emptyList()) { seleccion ->
            if (seleccion.distanceMeters > 1000) {
                val distanciaTexto = if (seleccion.distanceMeters >= 1000) {
                    String.format(Locale.getDefault(), "%.1f km", seleccion.distanceMeters / 1000)
                } else {
                    "${seleccion.distanceMeters.roundToInt()} m"
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Centro Alejado")
                    .setMessage("Este centro se encuentra a $distanciaTexto de tu ubicación. ¿Deseas continuar?")
                    .setPositiveButton("Sí, continuar") { _, _ -> navegarAEspecialistas(seleccion) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } else {
                navegarAEspecialistas(seleccion)
            }
        }
        binding.recyclerViewCentros.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewCentros.adapter = adapter
    }

    private fun initObservers() {
        centroMedicoVM.filteredCenters.observe(viewLifecycleOwner) { list ->
            val itemsAdapter = list.map { CenterWithDistance(it.first, it.second) }
            adapter.actualizarLista(itemsAdapter)
            binding.txtCountCentros.text = "${list.size} encontrados"
        }
    }

    private fun navegarAEspecialistas(seleccion: CenterWithDistance) {
        val intent = Intent(requireContext(), Especialistas::class.java)
        intent.putExtra("ID_CENTRO", seleccion.center.id)
        val espSeleccionada = binding.autoCompleteEspecialidad.text.toString()
        val especialidadAEnviar = if (espSeleccionada == "Todas") Specialty.GENERAL.name else Specialty.values().find { it.nombreMostrar == espSeleccionada }?.name ?: Specialty.GENERAL.name
        intent.putExtra("ESPECIALIDAD_FILTRO", especialidadAEnviar)
        startActivity(intent)
    }

    private fun cargarCentrosFiltrados() {
        val tipoSeleccionado = binding.autoCompleteTipoCentro.text.toString()
        val especialidadSeleccionada = binding.autoCompleteEspecialidad.text.toString()

        val tipoFiltroDb = if (tipoSeleccionado == "Todos") null else CenterType.values().find { it.nombreMostrar == tipoSeleccionado }?.name
        val especialidadFiltroDb = if (especialidadSeleccionada == "Todas") null else Specialty.values().find { it.nombreMostrar == especialidadSeleccionada }?.name
        centroMedicoVM.filtrarCentrosPorUbicacion(
            userLat = userLat,
            userLon = userLon,
            type = tipoFiltroDb,
            specialty = especialidadFiltroDb,
            radiusInMeters = 500000.0,
            getCentersFilteredUC = GetCentersFilteredUC(centroMedicoRepository)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class CentroMedicoAdapter(
        private var listaCentros: List<CenterWithDistance>,
        private val onClick: (CenterWithDistance) -> Unit
    ) : RecyclerView.Adapter<CentroMedicoAdapter.CentroViewHolder>() {

        fun actualizarLista(nuevaLista: List<CenterWithDistance>) {
            this.listaCentros = nuevaLista
            notifyDataSetChanged()
        }

        inner class CentroViewHolder(val itemBinding: CuadroCentroMedicoBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(item: CenterWithDistance) {
                val centro = item.center
                itemBinding.txtNombreCentro.text = centro.name
                val especialidadesAmigables = centro.specialties.map { try { Specialty.valueOf(it).nombreMostrar } catch (e: Exception) { it } }
                itemBinding.txtEspecialidad.text = especialidadesAmigables.joinToString(", ")
                val tipoAmigable = try { CenterType.valueOf(centro.type).nombreMostrar } catch (e: Exception) { centro.type }
                itemBinding.txtTipoCentro.text = tipoAmigable
                val distanciaTexto = if (item.distanceMeters >= 1000) String.format(Locale.getDefault(), "A %.1f km de distancia", item.distanceMeters / 1000) else "A ${item.distanceMeters.roundToInt()} m de distancia"
                itemBinding.txtDistancia.text = distanciaTexto
                itemBinding.root.setOnClickListener { onClick(item) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = CentroViewHolder(CuadroCentroMedicoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: CentroViewHolder, position: Int) = holder.bind(listaCentros[position])
        override fun getItemCount() = listaCentros.size
    }
}
