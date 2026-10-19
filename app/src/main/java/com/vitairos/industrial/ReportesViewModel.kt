package com.vitairos.industrial

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReportesViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = VitairosDatabase.obtenerBaseDatos(app).reporteDao()

    val reportes: StateFlow<List<Reporte>> = dao
        .obtenerReportes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun guardar(reporte: Reporte, alFinalizar: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { dao.guardarReporte(reporte) }
                alFinalizar(true)
            } catch (e: Exception) {
                alFinalizar(false)
            }
        }
    }

    fun actualizar(reporte: Reporte, alFinalizar: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { dao.actualizarReporte(reporte) }
                alFinalizar(true)
            } catch (e: Exception) {
                alFinalizar(false)
            }
        }
    }

    fun eliminar(reporte: Reporte) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dao.eliminarReporte(reporte) }
        }
    }

    suspend fun buscar(id: Int): Reporte? = withContext(Dispatchers.IO) {
        dao.buscarReporte(id)
    }
}
