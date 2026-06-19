package ec.edu.mapsalud.userPages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ec.edu.mapsalud.databinding.CuadroDoctorBinding
import ec.edu.mapsalud.databinding.UserEspecialistasBinding
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import ec.edu.mapsalud.dto.DoctorOfficeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ec.edu.mapsalud.remote.impl.OfficeRemoteImpl
import ec.edu.mapsalud.remote.inter.OfficeRemote

class Especialistas : AppCompatActivity() {

    private lateinit var binding: UserEspecialistasBinding
    private val repository: OfficeRemote = OfficeRemoteImpl()
    private var idCentroActual: String = ""
    private var especialidadSeleccionada: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UserEspecialistasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idCentroActual = intent.getStringExtra("ID_CENTRO") ?: ""
        especialidadSeleccionada = intent.getStringExtra("ESPECIALIDAD_FILTRO") ?: "General"

        binding.txtTituloEspecialistas.text = "Especialistas en\n$especialidadSeleccionada"

        configurarRecyclerView()
        cargarConsultoriosYDoctores()
    }

    private fun configurarRecyclerView() {
        binding.recyclerViewEspecialistas.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewEspecialistas.adapter = DoctorAdapter(emptyList()) { item ->
            val intent = Intent(this, ReservarCita::class.java)

            val primerNombre = item.doctor.info.nombres.split(" ").firstOrNull() ?: ""
            val primerApellido = item.doctor.info.apellidos.split(" ").firstOrNull() ?: ""
            val nombreFormateado = "$primerNombre $primerApellido".trim()

            intent.putExtra("ID_OFFICE", item.office.id)
            intent.putExtra("ID_DOCTOR", item.doctor.info.id)
            intent.putExtra("NOMBRE_DOCTOR", nombreFormateado)

            startActivity(intent)
        }
    }

    private fun cargarConsultoriosYDoctores() {
        lifecycleScope.launch(Dispatchers.Main) {

            val officesResult = withContext(Dispatchers.IO) {
                repository.getOfficesByCenterAndSpecialty(idCentroActual, especialidadSeleccionada)
            }

            officesResult.onSuccess { offices ->
                if (offices.isEmpty()) {
                    Toast.makeText(this@Especialistas, "No hay consultorios disponibles", Toast.LENGTH_LONG).show()
                    return@onSuccess
                }

                val combinedItems = withContext(Dispatchers.IO) {
                    offices.map { office ->
                        async {
                            // Verificamos que el office tenga un idDoctor válido
                            if (office.idDoctor.isNotEmpty()) {
                                val doctorResult = repository.getDoctorById(office.idDoctor).getOrNull()
                                if (doctorResult != null) {
                                    DoctorOfficeItem(office, doctorResult)
                                } else null
                            } else null
                        }
                    }.awaitAll().filterNotNull()
                }

                (binding.recyclerViewEspecialistas.adapter as DoctorAdapter).updateData(combinedItems)

            }.onFailure { error ->
                Toast.makeText(this@Especialistas, "Error de conexión: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class DoctorAdapter(
        private var items: List<DoctorOfficeItem>,
        private val onReservarClick: (DoctorOfficeItem) -> Unit
    ) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {

        fun updateData(newItems: List<DoctorOfficeItem>) {
            this.items = newItems
            notifyDataSetChanged()
        }

        inner class DoctorViewHolder(val itemBinding: CuadroDoctorBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(item: DoctorOfficeItem) {
                val primerNombre = item.doctor.info.nombres.split(" ").firstOrNull() ?: ""
                val primerApellido = item.doctor.info.apellidos.split(" ").firstOrNull() ?: ""

                itemBinding.txtNombreDoctor.text = "Dr. $primerNombre $primerApellido"
                itemBinding.txtExperiencia.text = "💼 ${item.doctor.anosExperiencia} años de experiencia"

                itemBinding.txtEspecialidadDoctor.text = item.office.specialty

                val diasTxt = item.office.availableDays.joinToString(", ")
                itemBinding.txtDisponibilidad.text = "📅 $diasTxt (${item.office.openingTime} - ${item.office.closingTime})"

                itemBinding.btnReservar.setOnClickListener {
                    onReservarClick(item)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
            val itemBinding = CuadroDoctorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return DoctorViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size
    }
}