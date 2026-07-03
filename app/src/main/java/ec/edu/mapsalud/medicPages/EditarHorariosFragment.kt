package ec.edu.mapsalud.medicPages

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import ec.edu.mapsalud.R
import ec.edu.mapsalud.databinding.MedicFragmentEditarBinding
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import ec.edu.mapsalud.dto.OfficeDtoRemote
import ec.edu.mapsalud.remote.impl.CentroMedicoRepositoryImpl
import ec.edu.mapsalud.remote.impl.ConsultorioRepositoryImpl
import ec.edu.mapsalud.remote.inter.CentroMedicoRepository
import ec.edu.mapsalud.remote.inter.ConsultorioRepository
import ec.edu.mapsalud.usercases.consultoriosUC.GetOfficesByDoctorUC
import ec.edu.mapsalud.usercases.consultoriosUC.UpdateOfficeHorariesUC
import ec.edu.mapsalud.viewmodel.CentroMedicoViewModel
import ec.edu.mapsalud.viewmodel.ConsultorioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class EditarHorariosFragment : Fragment(R.layout.medic_fragment_editar) {

    private var _binding: MedicFragmentEditarBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

    private val consultorioVM by viewModels<ConsultorioViewModel>()
    private val officeRepository = ConsultorioRepositoryImpl()
    private val centroMedicoRepository = CentroMedicoRepositoryImpl()
    private var diasSeleccionados = mutableSetOf<String>()
    private var listaConsultorios = listOf<OfficeDtoRemote>()
    private var consultorioSeleccionado: OfficeDtoRemote? = null

    private val mapaBotonesDias by lazy {
        mapOf(
            binding.btnEditDiaL to "Lunes",
            binding.btnEditDiaM to "Martes",
            binding.btnEditDiaMi to "Miércoles",
            binding.btnEditDiaJ to "Jueves",
            binding.btnEditDiaV to "Viernes",
            binding.btnEditDiaS to "Sábado",
            binding.btnEditDiaD to "Domingo"
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = MedicFragmentEditarBinding.bind(view)

        configurarListeners()
        initObservers()
        cargarConsultoriosDelMedico()
    }

    private fun cargarConsultoriosDelMedico() {
        val uidMedico = auth.currentUser?.uid ?: return
        consultorioVM.cargarConsultoriosPorDoctor(uidMedico, GetOfficesByDoctorUC(officeRepository))
    }

    private fun initObservers() {
        consultorioVM.officesList.observe(viewLifecycleOwner) { offices ->
            listaConsultorios = offices

            if (listaConsultorios.isEmpty()) {
                Toast.makeText(requireContext(), "No tienes consultorios registrados.", Toast.LENGTH_SHORT).show()
                return@observe
            }
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val nombresMostrar = coroutineScope {
                        listaConsultorios.map { office ->
                            async {
                                val centerResult = centroMedicoRepository.getCenterById(office.idCenter)
                                val center = centerResult.getOrNull()
                                val centerName = center?.name ?: "Centro Desconocido"
                                "${office.specialty} - $centerName"
                            }
                        }.awaitAll()
                    }
                    configurarDropdown(nombresMostrar)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error al cargar datos", Toast.LENGTH_SHORT).show()
                }
            }
        }
        consultorioVM.operationResult.observe(viewLifecycleOwner) { exito ->
            binding.btnEditarHorarios.isEnabled = true

            if (exito) {
                val consultorio = consultorioSeleccionado
                if (consultorio != null) {
                    val nuevaApertura = binding.editApertura.text.toString().trim()
                    val nuevoCierre = binding.editCierre.text.toString().trim()

                    consultorioSeleccionado = consultorio.copy(
                        availableDays = diasSeleccionados.toList(),
                        openingTime = nuevaApertura,
                        closingTime = nuevoCierre
                    )
                }
                Toast.makeText(requireContext(), "Horarios actualizados exitosamente", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Error al actualizar los horarios", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun configurarDropdown(nombresMostrar: List<String>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            nombresMostrar
        )
        binding.spinnerConsultorios.setAdapter(adapter)

        binding.spinnerConsultorios.setOnItemClickListener { _, _, position, _ ->
            consultorioSeleccionado = listaConsultorios[position]
            poblarDatosDelConsultorioSeleccionado()
        }
    }

    private fun poblarDatosDelConsultorioSeleccionado() {
        consultorioSeleccionado?.let { office ->
            diasSeleccionados.clear()
            diasSeleccionados.addAll(office.availableDays)

            binding.editApertura.setText(office.openingTime)
            binding.editCierre.setText(office.closingTime)

            actualizarUIBotonesDias()
        }
    }

    private fun configurarListeners() {
        mapaBotonesDias.forEach { (textView, dia) ->
            textView.setOnClickListener {
                if (diasSeleccionados.contains(dia)) {
                    diasSeleccionados.remove(dia)
                } else {
                    diasSeleccionados.add(dia)
                }
                actualizarUIBotonesDias()
            }
        }

        binding.editApertura.setOnClickListener {
            mostrarSelectorHora("Hora de apertura") { hora, minuto ->
                binding.editApertura.setText(formatearHoraAMPM(hora, minuto))
            }
        }

        binding.editCierre.setOnClickListener {
            mostrarSelectorHora("Hora de cierre") { hora, minuto ->
                binding.editCierre.setText(formatearHoraAMPM(hora, minuto))
            }
        }

        binding.btnEditarHorarios.setOnClickListener {
            guardarCambiosEnFirestore()
        }
    }

    private fun actualizarUIBotonesDias() {
        mapaBotonesDias.forEach { (textView, dia) ->
            val estaSeleccionado = diasSeleccionados.contains(dia)

            textView.setBackgroundResource(if (estaSeleccionado) R.drawable.chip_day_selected else R.drawable.chip_day_unselected)
            textView.setTextColor(Color.parseColor(if (estaSeleccionado) "#FFFFFF" else "#757575"))
        }
    }

    private fun guardarCambiosEnFirestore() {
        val consultorio = consultorioSeleccionado
        if (consultorio == null) {
            Toast.makeText(requireContext(), "Seleccione un consultorio", Toast.LENGTH_SHORT).show()
            return
        }

        val nuevaApertura = binding.editApertura.text.toString().trim()
        val nuevoCierre = binding.editCierre.text.toString().trim()

        if (diasSeleccionados.isEmpty() || nuevaApertura.isEmpty() || nuevoCierre.isEmpty()) {
            Toast.makeText(requireContext(), "Complete todos los campos y seleccione al menos un día", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnEditarHorarios.isEnabled = false

        consultorioVM.actualizarHorarios(
            idOffice = consultorio.id,
            availableDays = diasSeleccionados.toList(),
            openingTime = nuevaApertura,
            closingTime = nuevoCierre,
            updateHorariesUC = UpdateOfficeHorariesUC(officeRepository)
        )
    }

    private fun mostrarSelectorHora(titulo: String, onTimeSelected: (Int, Int) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(8)
            .setMinute(0)
            .setTitleText(titulo)
            .build()

        picker.addOnPositiveButtonClickListener {
            onTimeSelected(picker.hour, picker.minute)
        }
        picker.show(parentFragmentManager, "time_picker")
    }

    private fun formatearHoraAMPM(hora: Int, minuto: Int): String {
        val amPm = if (hora >= 12) "PM" else "AM"
        val horaFormateada = if (hora % 12 == 0) 12 else hora % 12
        return String.format("%02d:%02d %s", horaFormateada, minuto, amPm)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
