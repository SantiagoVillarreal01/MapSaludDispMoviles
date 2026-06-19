package ec.edu.mapsalud.medicPages

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import ec.edu.mapsalud.R
import ec.edu.mapsalud.databinding.MedicFragmentEditarBinding
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ec.edu.mapsalud.dto.OfficeDtoRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class EditarHorariosFragment : Fragment(R.layout.medic_fragment_editar) {

    private lateinit var binding: MedicFragmentEditarBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
        binding = MedicFragmentEditarBinding.bind(view)

        configurarListeners()
        cargarConsultoriosDelMedico()
    }

    private fun cargarConsultoriosDelMedico() {
        val uidMedico = auth.currentUser?.uid ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val snapshotOffices = db.collection("consultorios")
                    .whereEqualTo("idDoctor", uidMedico)
                    .get().await()

                listaConsultorios = snapshotOffices.toObjects(OfficeDtoRemote::class.java)

                if (listaConsultorios.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No tienes consultorios registrados.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val nombresMostrar = coroutineScope {
                    listaConsultorios.map { office ->
                        async {
                            val centerDoc = db.collection("centros_medicos").document(office.idCenter).get().await()
                            val centerName = centerDoc.getString("name") ?: "Centro Desconocido"
                            "${office.specialty} - $centerName"
                        }
                    }.awaitAll()
                }

                withContext(Dispatchers.Main) {
                    configurarSpinner(nombresMostrar)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error al cargar datos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun configurarSpinner(nombresMostrar: List<String>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            nombresMostrar
        )
        binding.spinnerConsultorios.adapter = adapter

        binding.spinnerConsultorios.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                consultorioSeleccionado = listaConsultorios[position]
                poblarDatosDelConsultorioSeleccionado()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
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

            textView.setBackgroundColor(Color.parseColor(if (estaSeleccionado) "#00695C" else "#E0E0E0"))
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

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val actualizaciones = mapOf(
                    "availableDays" to diasSeleccionados.toList(),
                    "openingTime" to nuevaApertura,
                    "closingTime" to nuevoCierre
                )

                db.collection("consultorios").document(consultorio.id)
                    .update(actualizaciones).await()

                consultorioSeleccionado = consultorio.copy(
                    availableDays = diasSeleccionados.toList(),
                    openingTime = nuevaApertura,
                    closingTime = nuevoCierre
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Horarios actualizados exitosamente", Toast.LENGTH_SHORT).show()
                    binding.btnEditarHorarios.isEnabled = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnEditarHorarios.isEnabled = true
                }
            }
        }
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
}