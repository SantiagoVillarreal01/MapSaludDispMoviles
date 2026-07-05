package ec.edu.mapsalud.patientPages

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
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import ec.edu.mapsalud.R
import ec.edu.mapsalud.dto.MedicoConsultorio
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl
import ec.edu.mapsalud.remote.impl.UsuariosRepositoryImpl
import ec.edu.mapsalud.usercases.consultoriosUC.GetOfficesByCenterAndSpecialtyUC
import ec.edu.mapsalud.utils.ThemeUtils
import ec.edu.mapsalud.viewmodel.ConsultorioViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope

class Especialistas : AppCompatActivity() {

    private lateinit var binding: UserEspecialistasBinding
    private lateinit var doctorAdapter: DoctorAdapter
    private val consultorioVM by viewModels<ConsultorioViewModel>()
    private val consultorioRepository = ConsultorioRepositoryImpl()
    private val usuarioRepository = UsuariosRepositoryImpl()
    private var idCentroActual: String = ""
    private var especialidadSeleccionada: String = ""

    private var cargarDoctoresJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = UserEspecialistasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idCentroActual = intent.getStringExtra("ID_CENTRO") ?: ""
        especialidadSeleccionada = intent.getStringExtra("ESPECIALIDAD_FILTRO") ?: "General"

        binding.txtTituloEspecialistas.text = "Especialistas en\n$especialidadSeleccionada"

        binding.btnRegresar.setOnClickListener {
            finish()
        }

        configurarRecyclerView()
        initObservers()
        cargarConsultoriosYDoctores()
    }

    private fun configurarRecyclerView() {
        doctorAdapter = DoctorAdapter { item ->
            val intent = Intent(this, ReservarCita::class.java)

            val primerNombre = item.doctor.info.nombres.split(" ").firstOrNull() ?: ""
            val primerApellido = item.doctor.info.apellidos.split(" ").firstOrNull() ?: ""
            val nombreFormateado = "$primerNombre $primerApellido".trim()

            intent.putExtra("ID_OFFICE", item.office.id)
            intent.putExtra("ID_DOCTOR", item.doctor.info.id)
            intent.putExtra("NOMBRE_DOCTOR", nombreFormateado)

            intent.putExtra("URL_FOTO_DOCTOR", item.doctor.info.imageUrl ?: "")

            startActivity(intent)
        }
        binding.recyclerViewEspecialistas.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewEspecialistas.adapter = doctorAdapter
    }

    private fun cargarConsultoriosYDoctores() {
        consultorioVM.cargarConsultoriosPorEspecialidad(
            idCenter = idCentroActual,
            specialty = especialidadSeleccionada,
            getOfficesUC = GetOfficesByCenterAndSpecialtyUC(consultorioRepository)
        )
    }

    private fun initObservers() {
        consultorioVM.officesList.observe(this) { offices ->

            cargarDoctoresJob?.cancel()

            if (offices.isEmpty()) {
                Toast.makeText(this@Especialistas, "No hay consultorios disponibles", Toast.LENGTH_LONG).show()
                (binding.recyclerViewEspecialistas.adapter as DoctorAdapter).updateData(emptyList())
                return@observe
            }
            cargarDoctoresJob = lifecycleScope.launch {
                val combinedItems = coroutineScope {
                    offices.map { office ->
                        async {
                            if (office.idDoctor.isNotEmpty()) {
                                val doctorResult = usuarioRepository.getDoctorById(office.idDoctor).getOrNull()
                                if (doctorResult != null) {
                                    MedicoConsultorio(office, doctorResult)
                                } else null
                            } else null
                        }
                    }.awaitAll().filterNotNull()
                }
                doctorAdapter.updateData(combinedItems)
            }
        }
    }

    inner class DoctorAdapter(
        private val onReservarClick: (MedicoConsultorio) -> Unit
    ) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {
        private var lastPosition = -1
        private val diffCallback = object : DiffUtil.ItemCallback<MedicoConsultorio>() {
            override fun areItemsTheSame(oldItem: MedicoConsultorio, newItem: MedicoConsultorio): Boolean {
                return oldItem.office.id == newItem.office.id
            }

            override fun areContentsTheSame(oldItem: MedicoConsultorio, newItem: MedicoConsultorio): Boolean {
                return oldItem == newItem
            }
        }
        private val differ = AsyncListDiffer(this, diffCallback)

        fun updateData(newItems: List<MedicoConsultorio>) {
            lastPosition = -1
            differ.submitList(newItems)
        }

        inner class DoctorViewHolder(val itemBinding: CuadroDoctorBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(item: MedicoConsultorio) {

                val context = itemBinding.root.context
                Glide.with(context).clear(itemBinding.imgDoctor)

                val primerNombre = item.doctor.info.nombres.split(" ").firstOrNull() ?: ""
                val primerApellido = item.doctor.info.apellidos.split(" ").firstOrNull() ?: ""

                itemBinding.txtNombreDoctor.text = "Dr. $primerNombre $primerApellido"
                itemBinding.txtExperiencia.text = "💼 ${item.doctor.anosExperiencia} años de experiencia"
                itemBinding.txtEspecialidadDoctor.text = item.office.specialty

                val diasTxt = item.office.availableDays.joinToString(", ")
                itemBinding.txtDisponibilidad.text = "📅 $diasTxt (${item.office.openingTime} - ${item.office.closingTime})"

                val fotoUrl = item.doctor.info.imageUrl ?: ""

                if (fotoUrl.isNotEmpty()) {
                    Glide.with(context)
                        .load(fotoUrl)
                        .centerCrop()
                        .placeholder(R.drawable.user)
                        .error(R.drawable.user)
                        .into(itemBinding.imgDoctor)
                } else {
                    itemBinding.imgDoctor.setImageResource(R.drawable.user)
                }

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
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return

            val item = differ.currentList[currentPosition]
            holder.bind(item)

            holder.itemBinding.root.animate().setListener(null).cancel()
            holder.itemBinding.root.translationY = 0f
            holder.itemBinding.root.alpha = 1f
            holder.itemBinding.root.scaleX = 1f
            holder.itemBinding.root.scaleY = 1f

            if (currentPosition > lastPosition) {
                val view = holder.itemBinding.root
                view.translationY = 120f
                view.alpha = 0f

                view.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setStartDelay(currentPosition * 40L)
                    .setDuration(320L)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .setListener(null)
                    .start()

                lastPosition = currentPosition
            }
        }

        override fun getItemCount() = differ.currentList.size

        override fun onViewDetachedFromWindow(holder: DoctorViewHolder) {
            holder.itemBinding.root.clearAnimation()
            super.onViewDetachedFromWindow(holder)
        }
    }
}