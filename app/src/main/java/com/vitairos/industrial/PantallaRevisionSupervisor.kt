
package com.vitairos.industrial

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.File

@Composable
fun PantallaRevisionSupervisor(
    alVolver: () -> Unit
) {

    val contexto = LocalContext.current

    val baseDatos = remember(contexto) {
        VitairosDatabase.obtenerBaseDatos(contexto)
    }

    val alcance = rememberCoroutineScope()

    val flujoReportes = remember(baseDatos) {
        baseDatos.reporteDao().obtenerReportes()
    }

    val reportes by flujoReportes.collectAsState(
        initial = emptyList()
    )

    var idSeleccionado by remember {
        mutableStateOf<Int?>(null)
    }

    ColumnaPantalla {

        Titulo("REVISIÓN DEL SUPERVISOR")

        Spacer(Modifier.height(15.dp))

        if (idSeleccionado == null) {

            TextoSecundario(
                "Seleccione un reporte para revisarlo"
            )

            Spacer(Modifier.height(20.dp))

            if (reportes.isEmpty()) {

                TextoSecundario(
                    "No existen reportes guardados"
                )

            } else {

                reportes.forEach { reporte ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF12392D)
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = "REPORTE N.º ${reporte.id}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(8.dp))

                            TextoDato(
                                "Documento",
                                reporte.identificadorDocumento
                            )

                            TextoDato(
                                "Tipo",
                                reporte.tipoReporte
                            )

                            TextoDato(
                                "Estado",
                                reporte.estado
                            )

                            Spacer(Modifier.height(12.dp))

                            BotonVerde("REVISAR REPORTE") {
                                idSeleccionado = reporte.id
                            }
                        }
                    }

                    Spacer(Modifier.height(15.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            BotonVolver(alVolver)

        } else {

            val reporte = reportes.find {
                it.id == idSeleccionado
            }

            if (reporte == null) {

                TextoError("No se encontró el reporte")

            } else {

                var observacionSupervisor by remember(
                    reporte.id
                ) {
                    mutableStateOf(
                        reporte.observacionSupervisor
                    )
                }

                var mensaje by remember(reporte.id) {
                    mutableStateOf("")
                }

                var guardando by remember(reporte.id) {
                    mutableStateOf(false)
                }

                TextoSecundario(
                    "REPORTE N.º ${reporte.id}"
                )

                Spacer(Modifier.height(20.dp))

                TextoDato(
                    "Documento",
                    reporte.identificadorDocumento
                )

                TextoDato(
                    "Tipo",
                    reporte.tipoReporte
                )

                TextoDato(
                    "Versión",
                    reporte.version
                )

                TextoDato(
                    "Proceso",
                    reporte.proceso
                )

                if (reporte.area.isNotBlank()) {
                    TextoDato("Área / Faena", reporte.area)
                }

                if (reporte.equipo.isNotBlank()) {
                    TextoDato("Equipo / TAG", reporte.equipo)
                }

                if (reporte.turno.isNotBlank()) {
                    TextoDato("Turno", reporte.turno)
                }

                if (reporte.severidad.isNotBlank()) {
                    TextoDato("Severidad", reporte.severidad)
                }

                TextoDato(
                    "Fecha",
                    reporte.fechaHora
                )

                TextoDato(
                    "Operador",
                    reporte.usuario
                )

                TextoDato(
                    "Observaciones del operador",
                    reporte.observaciones
                )

                TextoDato(
                    "Estado",
                    reporte.estado
                )

                Spacer(Modifier.height(15.dp))

                Titulo("ARCHIVO ADJUNTO")

                Spacer(Modifier.height(10.dp))

                TextoDato(
                    "Archivo",
                    File(reporte.archivoLocal).name
                )

                Spacer(Modifier.height(20.dp))

                if (reporte.estado in listOf("PENDIENTE", "EN_COLA", "SINCRONIZADO")) {

                    CampoTexto(
                        valor = observacionSupervisor,
                        alCambiar = {
                            observacionSupervisor = it
                        },
                        etiqueta = "Observación del supervisor",
                        unaLinea = false
                    )

                    Spacer(Modifier.height(20.dp))

                    BotonVerde("APROBAR REPORTE") {

                        if (!guardando) {

                            guardando = true
                            mensaje = "Guardando aprobación..."

                            alcance.launch {

                                try {

                                    withContext(Dispatchers.IO) {

                                        baseDatos.reporteDao()
                                            .actualizarReporte(
                                                reporte.copy(
                                                    estado = "SINCRONIZADO",
                                                    observacionSupervisor =
                                                        observacionSupervisor.trim()
                                                )
                                            )
                                    }

                                    mensaje = "Reporte aprobado correctamente"

                                } catch (e: Exception) {

                                    mensaje = "Error: " +
                                            (e.localizedMessage
                                                ?: "No se pudo aprobar")

                                } finally {

                                    guardando = false
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(15.dp))

                    BotonVerde("RECHAZAR REPORTE") {

                        if (observacionSupervisor.isBlank()) {

                            mensaje =
                                "Debe escribir el motivo del rechazo"

                        } else if (!guardando) {

                            guardando = true
                            mensaje = "Guardando rechazo..."

                            alcance.launch {

                                try {

                                    withContext(Dispatchers.IO) {

                                        baseDatos.reporteDao()
                                            .actualizarReporte(
                                                reporte.copy(
                                                    estado = "REQUIERE_CORRECCIÓN",
                                                    observacionSupervisor =
                                                        observacionSupervisor.trim()
                                                )
                                            )
                                    }

                                    mensaje = "Reporte rechazado. Motivo guardado."

                                } catch (e: Exception) {

                                    mensaje = "Error: " +
                                            (e.localizedMessage
                                                ?: "No se pudo rechazar")

                                } finally {

                                    guardando = false
                                }
                            }
                        }
                    }

                } else {

                    TextoSecundario(
                        "Este reporte ya fue revisado"
                    )

                    Spacer(Modifier.height(15.dp))

                    if (
                        reporte.observacionSupervisor.isNotBlank()
                    ) {

                        TextoDato(
                            "Observación del supervisor",
                            reporte.observacionSupervisor
                        )
                    }
                }

                if (mensaje.isNotEmpty()) {

                    Spacer(Modifier.height(20.dp))

                    TextoSecundario(mensaje)
                }
            }

            Spacer(Modifier.height(25.dp))

            BotonVolver(
                alPulsar = {
                    idSeleccionado = null
                },
                texto = "VOLVER A LOS REPORTES"
            )
        }
    }
}
