package com.vitairos.industrial

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@Composable
fun SelectorUbicacion(
    ubicacion: String,
    alObtenerUbicacion: (String) -> Unit
) {
    val contexto = LocalContext.current
    var obteniendo by remember { mutableStateOf(false) }
    var mensajePermiso by remember { mutableStateOf("") }

    val solicitarPermiso = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val concedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (concedido) {
            obteniendo = true
            mensajePermiso = ""
            obtenerCoordenadas(contexto) { coords ->
                alObtenerUbicacion(coords)
                obteniendo = false
            }
        } else {
            mensajePermiso = "Permiso de ubicación denegado. Active el permiso en Ajustes."
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {

        Text(
            text = "UBICACIÓN GPS",
            color = Color(0xFFAAB6B7),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        AnimatedVisibility(visible = ubicacion.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1B342D)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📍", fontSize = 16.sp)
                    Text(
                        text = ubicacion,
                        color = Color(0xFFFBF7F1),
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Button(
            onClick = {
                val tienePermiso = ContextCompat.checkSelfPermission(
                    contexto, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            contexto, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                if (tienePermiso) {
                    obteniendo = true
                    mensajePermiso = ""
                    obtenerCoordenadas(contexto) { coords ->
                        alObtenerUbicacion(coords)
                        obteniendo = false
                    }
                } else {
                    solicitarPermiso.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            enabled = !obteniendo,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF102B23),
                contentColor = Color(0xFFD9B46C)
            )
        ) {
            if (obteniendo) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color(0xFFD9B46C),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Obteniendo GPS...")
            } else {
                Text(
                    text = if (ubicacion.isBlank()) "OBTENER UBICACIÓN GPS" else "ACTUALIZAR UBICACIÓN",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        AnimatedVisibility(visible = mensajePermiso.isNotBlank()) {
            Text(
                text = mensajePermiso,
                color = Color(0xFFDD7A52),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@SuppressLint("MissingPermission")
private fun obtenerCoordenadas(
    contexto: android.content.Context,
    alCompletar: (String) -> Unit
) {
    try {
        val cliente = LocationServices.getFusedLocationProviderClient(contexto)

        cliente.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->
            if (location != null) {
                val lat = String.format("%.6f", location.latitude)
                val lon = String.format("%.6f", location.longitude)
                alCompletar("Lat: $lat, Lon: $lon")
            } else {
                alCompletar("Ubicación no disponible")
            }
        }.addOnFailureListener {
            alCompletar("Error al obtener GPS")
        }

    } catch (e: SecurityException) {
        alCompletar("Permiso GPS requerido")
    }
}
