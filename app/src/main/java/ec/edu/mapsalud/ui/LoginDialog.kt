package ec.edu.mapsalud.ui

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import ec.edu.mapsalud.R

class LoadingDialog(
    context: Context
) {

    private val dialog = Dialog(context)

    private val txtLoading: TextView

    init {

        val view = LayoutInflater.from(context)
            .inflate(R.layout.loading_dialog, null)

        txtLoading = view.findViewById(R.id.txtLoading)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.setContentView(view)

        dialog.setCancelable(false)

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    fun show(message: String = "Cargando...") {

        txtLoading.text = message

        if (!dialog.isShowing) {

            dialog.show()

        }
    }

    fun hide() {

        if (dialog.isShowing) {

            dialog.dismiss()

        }
    }

    fun isShowing(): Boolean {

        return dialog.isShowing

    }
}