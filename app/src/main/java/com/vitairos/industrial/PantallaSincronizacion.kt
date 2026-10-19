package com.vitairos.industrial

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PantallaSincronizacion(
    alVolver: () -> Unit
) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()

    val baseDatos = remember(contexto) {
        VitairosDatabase.obtenerBaseDatos(contexto)
    }

    val flujoReportes = remember(baseDatos) {
        baseDatos.reporteDao().obtenerReportes()
    }

    val reportes by flujoReportes.collectAsState(initial = emptyList())

    val enCola = reportes.count { it.estado == "EN_COLA" || it.estado == "PENDIENTE" }
    val enviando = reportes.count { it.estado == "ENVIANDO" }
    val sincronizados = reportes.count { it.estado == "SINCRONIZADO" || it.estado == "APROBADO" }
    val conError = reportes.count { it.estado == "ERROR" || it.estado == "REQUIERE_CORRECCIÓN" }

    var sincronizando by remember { mutableStateOf(false) }
    var progreso by remember { mutableFloatStateOf(0f) }
    var mensaje by remember { mutableStateOf("") }
    var intentoActual by remember { mutableStateOf("") }

    ColumnaPantalla {

        Titulo("COLA DE SINCRONIZACIÓN")

        Spacer(Modifier.height(15.dp))

        TextoSecundario("Control de envío de reportes al servidor")

        Spacer(Modifier.height(25.dp))

        // Tarjeta de resumen con colores de estado animados
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF102B23))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Text(
                    text = "RESUMEN",
                    color = Color(0xFFD9B46C),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.5.sp
                )

                Spacer(Modifier.height(14.dp))

                FilaEstado("En cola", enCola, Color(0xFFD9B46C))
                FilaEstado("Enviando", enviando, Color(0xFF64B5F6))
                FilaEstado("Sincronizados", sincronizados, Color(0xFF69F0AE))
                FilaEstado("Con error", conError, Color(0xFFDD7A52))
                FilaEstado("Total", reportes.size, Color(0xFFAAB6B7))
            }
        }

        Spacer(Modifier.height(25.dp))

        if (sincronizando) {

            Text(
                text = "Sincronizando...",
                color = Color(0xFF64B5F6),
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = Color(0xFFDD7A52),
                trackColor = Color(0xFF1B342D)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${(progreso * 100).toInt()}%",
                color = Color(0xFFFBF7F1)
            )

            if (intentoActual.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = intentoActual,
                    color = Color(0xFFD9B46C),
                    fontSize = 12.sp
                )
            }

        } else {

            Button(
                onClick = {
                    if (enCola > 0) {
                        sincronizando = true
                        progreso = 0f
                        mensaje = ""

                        alcance.launch {
                            val pendientes = withContext(Dispatchers.IO) {
                                baseDatos.reporteDao().obtenerPendientes()
                            }

                            var exitosos = 0
                            var fallidos = 0
                            val delaysReintento = listOf(5_000L, 15_000L, 30_000L)

                            pendientes.forEachIndexed { index, reporte ->

                                withContext(Dispatchers.IO) {
                                    baseDatos.reporteDao().actualizarReporte(
                                        reporte.copy(estado = "ENVIANDO")
                                    )
                                }

                                var sincronizado = false
                                for (intento in 1..3) {
                                    intentoActual = "Intento $intento de 3 — ${reporte.identificadorDocumento}"
                                    delay(1_200)

                                    // Falla simulada solo en intento 1 para demo (20% de reportes)
                                    val falla = intento == 1 && (index % 5 == 0) && pendientes.size > 1

                                    if (!falla) {
                                        withContext(Dispatchers.IO) {
                                            baseDatos.reporteDao().actualizarReporte(
                                                reporte.copy(estado = "SINCRONIZADO")
                                            )
                                        }
                                        sincronizado = true
                                        exitosos++
                                        break
                                    } else {
                                        intentoActual = "Error en intento $intento — reintentando en ${delaysReintento[intento - 1] / 1000}s…"
                                        delay(delaysReintento[intento - 1])
                                    }
                                }

                                if (!sincronizado) {
                                    withContext(Dispatchers.IO) {
                                        baseDatos.reporteDao().actualizarReporte(
                                            reporte.copy(estado = "ERROR")
                                        )
                                    }
                                    fallidos++
                                }

                                progreso = (index + 1f) / pendientes.size
                            }

                            intentoActual = ""
                            sincronizando = false
                            mensaje = when {
                                fallidos == 0 -> "✓ $exitosos reporte(s) sincronizado(s) correctamente"
                                exitosos == 0 -> "✗ $fallidos reporte(s) con error tras 3 reintentos"
                                else -> "✓ $exitosos sincronizados · ✗ $fallidos con error"
                            }
                        }
                    } else {
                        mensaje = "No hay reportes en cola para sincronizar"
                    }
                },
                enabled = !sincronizando,
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDD7A52)
                )
            ) {
                Text(
                    "SINCRONIZAR AHORA",
                    color = Color(0xFFFBF7F1),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (mensaje.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            val colorMsg = if (mensaje.startsWith("✓")) Color(0xFF69F0AE) else Color(0xFFD9B46C)
            Text(text = mensaje, color = colorMsg, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(30.dp))

        BotonVolver(alVolver)
    }
}

@Composable
private fun FilaEstado(etiqueta: String, cantidad: Int, color: Color) {
    val colorAnimado by animateColorAsState(
        targetValue = if (cantidad > 0) color else Color(0xFF3A5A50),
        animationSpec = tween(500),
        label = "color_$etiqueta"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = etiqueta, color = Color(0xFFAAB6B7), fontSize = 14.sp)
        Text(
            text = cantidad.toString(),
            color = colorAnimado,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}
