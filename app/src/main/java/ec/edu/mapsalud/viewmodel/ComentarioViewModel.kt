package ec.edu.mapsalud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.mapsalud.dto.ComentarioDtoRemote
import ec.edu.mapsalud.usercases.comentriosUC.GetCommentsByCenterUC
import ec.edu.mapsalud.usercases.comentriosUC.SaveCommentUC
import kotlinx.coroutines.launch

class ComentarioViewModel : ViewModel() {

    private var _commentsList = MutableLiveData<List<ComentarioDtoRemote>>()
    val commentsList: LiveData<List<ComentarioDtoRemote>> get() = _commentsList

    private var _commentSaved = MutableLiveData<ComentarioDtoRemote?>()
    val commentSaved: LiveData<ComentarioDtoRemote?> get() = _commentSaved

    fun cargarComentariosPorCentro(idCenter: String, getCommentsUC: GetCommentsByCenterUC) {
        viewModelScope.launch {
            val resultado = getCommentsUC.invoke(idCenter).getOrNull()
            _commentsList.value = resultado ?: emptyList()
        }
    }

    fun guardarComentario(comment: ComentarioDtoRemote, saveCommentUC: SaveCommentUC) {
        viewModelScope.launch {
            val resultado = saveCommentUC.invoke(comment).getOrNull()
            _commentSaved.value = resultado
        }
    }
}