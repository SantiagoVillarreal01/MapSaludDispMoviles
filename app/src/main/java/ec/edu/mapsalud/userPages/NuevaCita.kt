package ec.edu.mapsalud.userPages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ec.edu.mapsalud.databinding.CuadroCentroMedicoBinding
import ec.edu.mapsalud.databinding.UserNuevaCitaBinding
import ec.edu.mapsalud.dto.CenterWithDistance
import ec.edu.mapsalud.remote.impl.MedicalCenterRemoteImpl
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import ec.edu.mapsalud.enum.CenterType
import ec.edu.mapsalud.enum.Specialty


class NuevaCita : AppCompatActivity() {

    private lateinit var binding: UserNuevaCitaBinding
    private lateinit var adapter: CentroMedicoAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val repository = MedicalCenterRemoteImpl()

    // Variables mutables para almacenar la ubicación en tiempo real.
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
            Toast.makeText(this, "Permiso denegado. Usando ubicación predeterminada.", Toast.LENGTH_LONG).show()
            cargarCentrosFiltrados()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UserNuevaCitaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.btnRegresar.setOnClickListener { finish() }

        configurarRecyclerView()
        configurarFiltros()
        verificarPermisosYGeolocalizar()
    }

    private fun verificarPermisosYGeolocalizar() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            obtenerUbicacionActual()
        } else {
            // Lanzamos la petición de permisos al usuario
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
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación actual. Usando predeterminada.", Toast.LENGTH_SHORT).show()
            }
            cargarCentrosFiltrados()
        }.addOnFailureListener {
            Toast.makeText(this, "Error de GPS. Usando ubicación predeterminada.", Toast.LENGTH_SHORT).show()
            cargarCentrosFiltrados()
        }
    }

    private fun configurarFiltros() {
        val tiposCentro = mutableListOf("Todos")
        tiposCentro.addAll(CenterType.values().map { it.nombreMostrar })

        val adapterTipos = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tiposCentro)
        binding.autoCompleteTipoCentro.setAdapter(adapterTipos)
        binding.autoCompleteTipoCentro.setText("Todos", false)

        val especialidades = mutableListOf("Todas")
        especialidades.addAll(Specialty.values().map { it.nombreMostrar }.sorted())

        val adapterEspecialidades = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, especialidades)
        binding.autoCompleteEspecialidad.setAdapter(adapterEspecialidades)
        binding.autoCompleteEspecialidad.setText("Todas", false)

        binding.autoCompleteTipoCentro.addTextChangedListener { cargarCentrosFiltrados() }
        binding.autoCompleteEspecialidad.addTextChangedListener { cargarCentrosFiltrados() }
    }

    private fun configurarRecyclerView() {
        adapter = CentroMedicoAdapter(emptyList()) { seleccion ->
            val intent = Intent(this, Especialistas::class.java)
            intent.putExtra("ID_CENTRO", seleccion.center.id)

            val espSeleccionada = binding.autoCompleteEspecialidad.text.toString()
            val especialidadAEnviar = if (espSeleccionada == "Todas") {
                Specialty.GENERAL.name
            } else {
                Specialty.values().find { it.nombreMostrar == espSeleccionada }?.name ?: Specialty.GENERAL.name
            }

            intent.putExtra("ESPECIALIDAD_FILTRO", especialidadAEnviar)
            startActivity(intent)
        }
        binding.recyclerViewCentros.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewCentros.adapter = adapter
    }

    private fun cargarCentrosFiltrados() {
        val tipoSeleccionado = binding.autoCompleteTipoCentro.text.toString()
        val especialidadSeleccionada = binding.autoCompleteEspecialidad.text.toString()

        val tipoFiltroDb = if (tipoSeleccionado == "Todos") {
            null
        } else {
            CenterType.values().find { it.nombreMostrar == tipoSeleccionado }?.name
        }

        val especialidadFiltroDb = if (especialidadSeleccionada == "Todas") {
            null
        } else {
            Specialty.values().find { it.nombreMostrar == especialidadSeleccionada }?.name
        }

        lifecycleScope.launch {
            val result = repository.getCentersFiltered(
                userLat,
                userLon,
                tipoFiltroDb,
                especialidadFiltroDb
            )

            result.onSuccess { list ->
                val itemsAdapter = list.map { (centro, distancia) ->
                    CenterWithDistance(centro, distancia)
                }
                adapter.actualizarLista(itemsAdapter)
                binding.txtCountCentros.text = "${list.size} encontrados"
            }.onFailure {
                Toast.makeText(this@NuevaCita, "Error al cargar centros: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class CentroMedicoAdapter(
        private var listaCentros: List<CenterWithDistance>,
        private val onClick: (CenterWithDistance) -> Unit
    ) : RecyclerView.Adapter<CentroMedicoAdapter.CentroViewHolder>() {

        fun actualizarLista(nuevaLista: List<CenterWithDistance>) {
            this.listaCentros = nuevaLista
            notifyDataSetChanged()
        }

        inner class CentroViewHolder(val itemBinding: CuadroCentroMedicoBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(item: CenterWithDistance) {
                val centro = item.center
                itemBinding.txtNombreCentro.text = centro.name

                val especialidadesAmigables = centro.specialties.map { espDb ->
                    try {
                        Specialty.valueOf(espDb).nombreMostrar
                    } catch (e: Exception) {
                        espDb
                    }
                }
                itemBinding.txtEspecialidad.text = especialidadesAmigables.joinToString(", ")

                val tipoAmigable = try {
                    CenterType.valueOf(centro.type).nombreMostrar
                } catch (e: Exception) {
                    centro.type
                }
                itemBinding.txtTipoCentro.text = tipoAmigable

                val metrosRedondeados = item.distanceMeters.roundToInt()
                itemBinding.txtDistancia.text = "A ${metrosRedondeados}m de distancia"

                itemBinding.root.setOnClickListener { onClick(item) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CentroViewHolder {
            val itemBinding = CuadroCentroMedicoBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return CentroViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: CentroViewHolder, position: Int) {
            holder.bind(listaCentros[position])
        }

        override fun getItemCount() = listaCentros.size
    }
}