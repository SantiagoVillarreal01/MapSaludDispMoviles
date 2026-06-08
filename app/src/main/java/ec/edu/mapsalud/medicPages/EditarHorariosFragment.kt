package ec.edu.mapsalud.medicPages

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import ec.edu.mapsalud.R
import ec.edu.mapsalud.databinding.MedicFragmentEditarBinding
import kotlin.collections.iterator

class EditarHorariosFragment : Fragment(R.layout.medic_fragment_editar) {

    private lateinit var binding: MedicFragmentEditarBinding
    private val diasSeleccionados = mutableSetOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = MedicFragmentEditarBinding.bind(view)

        configurarBotonesDias()

        binding.editApertura.setOnClickListener {
            mostrarSelectorHora("Seleccionar hora de apertura") { hora, minuto ->
                binding.editApertura.setText(formatearHoraAMPM(hora, minuto))
            }
        }
        binding.editCierre.setOnClickListener {
            mostrarSelectorHora("Seleccionar hora de cierre") { hora, minuto ->
                binding.editCierre.setText(formatearHoraAMPM(hora, minuto))
            }
        }
    }

    private fun configurarBotonesDias() {
        val botonesDias = mapOf(
            binding.btnEditDiaL to "Lunes",
            binding.btnEditDiaM to "Martes",
            binding.btnEditDiaMi to "Miércoles",
            binding.btnEditDiaJ to "Jueves",
            binding.btnEditDiaV to "Viernes",
            binding.btnEditDiaS to "Sábado",
            binding.btnEditDiaD to "Domingo"
        )

        for ((textView, dia) in botonesDias) {

            textView.setBackgroundColor(Color.parseColor("#E0E0E0"))
            textView.setTextColor(Color.parseColor("#757575"))

            textView.setOnClickListener {
                if (diasSeleccionados.contains(dia)) {
                    // Desmarcar
                    diasSeleccionados.remove(dia)
                    textView.setBackgroundColor(Color.parseColor("#E0E0E0"))
                    textView.setTextColor(Color.parseColor("#757575"))
                } else {
                    // Marcar
                    diasSeleccionados.add(dia)
                    textView.setBackgroundColor(Color.parseColor("#00695C"))
                    textView.setTextColor(Color.WHITE)
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