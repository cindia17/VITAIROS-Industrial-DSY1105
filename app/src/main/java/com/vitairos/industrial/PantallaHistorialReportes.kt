
package com.vitairos.industrial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

@Composable
fun PantallaHistorialReportes(
    alVolver: () -> Unit,
    alAbrirReporte: (Int) -> Unit
) {

    val contexto = LocalContext.current

    val baseDatos = remember(contexto) {
        VitairosDatabase.obtenerBaseDatos(contexto)
    }

    val flujoReportes = remember(baseDatos) {
        baseDatos.reporteDao().obtenerReportes()
    }

    val reportes by flujoReportes.collectAsState(
        initial = emptyList()
    )

    var filtro by remember {
        mutableStateOf("TODOS")
    }

    val reportesFiltrados = reportes.filter { reporte ->
        when (filtro) {
            "TODOS" -> true
            "EN_COLA" -> reporte.estado == "EN_COLA" || reporte.estado == "PENDIENTE"
            "SINCRONIZADO" -> reporte.estado == "SINCRONIZADO" || reporte.estado == "APROBADO"
            "ERROR" -> reporte.estado == "ERROR" || reporte.estado == "RECHAZADO"
            else -> reporte.estado == filtro
        }
    }

    ColumnaPantalla {

        Titulo("HISTORIAL DE REPORTES")

        Spacer(Modifier.height(12.dp))

        TextoSecundario(
            "Consulta los reportes guardados en este dispositivo"
        )

        Spacer(Modifier.height(22.dp))

        Text(
            text = "Total de reportes: ${reportes.size}",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "En cola: ${reportes.count { it.estado == "EN_COLA" || it.estado == "PENDIENTE" }}",
            color = Color(0xFFD9B46C)
        )

        Text(
            text = "Sincronizados: ${reportes.count { it.estado == "SINCRONIZADO" || it.estado == "APROBADO" }}",
            color = Color(0xFF69F0AE)
        )

        Text(
            text = "Error / Requieren corrección: ${reportes.count { it.estado == "ERROR" || it.estado == "RECHAZADO" || it.estado == "REQUIERE_CORRECCIÓN" }}",
            color = Color(0xFFDD7A52)
        )

        Spacer(Modifier.height(25.dp))

        Titulo("FILTRAR POR ESTADO")

        Spacer(Modifier.height(15.dp))

        val filtros = listOf(
            "TODOS",
            "EN_COLA",
            "ENVIANDO",
            "SINCRONIZADO",
            "ERROR",
            "REQUIERE_CORRECCIÓN"
        )

        filtros.forEach { opcion ->

            OutlinedButton(
                onClick = {
                    filtro = opcion
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (filtro == opcion) {
                        Color(0xFF00A859)
                    } else {
                        Color.Transparent
                    },
                    contentColor = Color.White
                )
            ) {

                Text(
                    text = opcion,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Resultados: ${reportesFiltrados.size}",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(15.dp))

        if (reportesFiltrados.isEmpty()) {

            TextoSecundario(
                "No existen reportes con este estado"
            )

        } else {

            reportesFiltrados.forEach { reporte ->

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
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

                        Spacer(Modifier.height(12.dp))

                        TextoDato(
                            "Documento",
                            reporte.identificadorDocumento
                        )

                        TextoDato(
                            "Tipo",
                            reporte.tipoReporte
                        )

                        TextoDato(
                            "Proceso",
                            reporte.proceso
                        )

                        if (reporte.area.isNotBlank()) {
                            TextoDato("Área", reporte.area)
                        }

                        if (reporte.turno.isNotBlank()) {
                            TextoDato("Turno", reporte.turno)
                        }

                        TextoDato(
                            "Fecha",
                            reporte.fechaHora
                        )

                        val colorEstado = colorDeEstado(reporte.estado)

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Estado: ${reporte.estado}",
                            color = colorEstado,
                            fontWeight = FontWeight.Bold
                        )

                        if (
                            reporte.observacionSupervisor.isNotBlank()
                        ) {

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "Observación del supervisor:",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(5.dp))

                            Text(
                                text = reporte.observacionSupervisor,
                                color = Color.LightGray
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        BotonVerde("VER REPORTE") {
                            alAbrirReporte(reporte.id)
                        }
                    }
                }

                Spacer(Modifier.height(15.dp))
            }
        }

        Spacer(Modifier.height(25.dp))

        BotonVolver(alVolver)
    }
}
