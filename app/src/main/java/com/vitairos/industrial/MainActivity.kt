
package com.vitairos.industrial

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import com.vitairos.industrial.ui.theme.VITAIROSIndustrialTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val TIPOS_REPORTE = listOf(
    "Reporte de turno",
    "Orden de mantención",
    "Inspección de equipo",
    "Incidente o hallazgo",
    "Checklist pre-uso"
)

private val TIPOS_CON_EQUIPO = setOf(
    "Orden de mantención",
    "Inspección de equipo",
    "Checklist pre-uso"
)

private val PROCESOS = listOf(
    "Chancado primario",
    "Molienda SAG",
    "Flotación",
    "Transporte de mineral",
    "Mantención planta",
    "Subestación eléctrica"
)

private val AREAS = listOf(
    "Planta concentradora",
    "Mina rajo",
    "Taller de mantención",
    "Subestación eléctrica"
)

private val TURNOS = listOf("Turno A – Día", "Turno B – Noche", "Turno C – Relevo")

private val SEVERIDADES = listOf("Baja", "Media", "Alta")

// Paleta VITAIROS Industrial AI
private val VerdeNoche = Color(0xFF081D16)
private val VerdeProfundo = Color(0xFF102B23)
private val VerdeMineral = Color(0xFF1B342D)
private val Hueso = Color(0xFFFBF7F1)
private val Niebla = Color(0xFFE8EFE9)
private val Cobre = Color(0xFFDD7A52)
private val Oro = Color(0xFFD9B46C)
private val Acero = Color(0xFFAAB6B7)

// Compatibilidad con nombre anterior
private val VerdeOscuro = VerdeNoche
private val VerdeBoton = Color(0xFF00A859)
private val Blanco = Hueso

fun colorDeEstado(estado: String): Color = when (estado) {
    "SINCRONIZADO", "APROBADO" -> Color(0xFF69F0AE)
    "EN_COLA", "PENDIENTE"     -> Color(0xFFD9B46C)
    "ENVIANDO"                 -> Color(0xFF64B5F6)
    "BORRADOR"                 -> Color(0xFFAAB6B7)
    "ERROR", "RECHAZADO",
    "REQUIERE_CORRECCIÓN"      -> Color(0xFFDD7A52)
    else                       -> Color(0xFFAAB6B7)
}

// ACTIVIDAD PRINCIPAL
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VITAIROSIndustrialTheme {
                AplicacionVitairos()
            }
        }
    }
}

// NAVEGACIÓN DE LA APLICACIÓN
@Composable
fun AplicacionVitairos() {

    var pantalla by rememberSaveable {
        mutableStateOf("carga")
    }

    var reporteSeleccionado by rememberSaveable {
        mutableIntStateOf(0)
    }

    var pantallaAnterior by rememberSaveable {
        mutableStateOf("reportes_guardados")
    }

    when (pantalla) {

        "carga" -> PantallaCarga(
            alTerminar = {
                pantalla = "bienvenida"
            }
        )

        "bienvenida" -> PantallaBienvenida(
            entrarOperador = {
                pantalla = "login_operador"
            },
            entrarSupervisor = {
                pantalla = "login_supervisor"
            }
        )

        "login_operador" -> PantallaAcceso(
            tipoUsuario = "OPERADOR",
            alVolver = {
                pantalla = "bienvenida"
            },
            alEntrar = {
                pantalla = "panel_operador"
            }
        )

        "login_supervisor" -> PantallaAcceso(
            tipoUsuario = "SUPERVISOR",
            alVolver = {
                pantalla = "bienvenida"
            },
            alEntrar = {
                pantalla = "panel_supervisor"
            }
        )

        "panel_operador" -> PanelOperador(
            nuevoReporte = {
                pantalla = "nuevo_reporte"
            },
            verReportes = {
                pantalla = "reportes_guardados"
            },
            verHistorial = {
                pantalla = "historial_reportes"
            },
            verSincronizacion = {
                pantalla = "sincronizacion"
            },
            alSalir = {
                pantalla = "bienvenida"
            }
        )

        "nuevo_reporte" -> PantallaNuevoReporte(
            alVolver = {
                pantalla = "panel_operador"
            }
        )

        "reportes_guardados" -> PantallaReportesGuardados(
            alAbrirReporte = { id ->
                reporteSeleccionado = id
                pantallaAnterior = "reportes_guardados"
                pantalla = "detalle_reporte"
            },
            alVolver = {
                pantalla = "panel_operador"
            }
        )

        "sincronizacion" -> PantallaSincronizacion(
            alVolver = {
                pantalla = "panel_operador"
            }
        )

        "historial_reportes" -> PantallaHistorialReportes(
            alVolver = {
                pantalla = "panel_operador"
            },
            alAbrirReporte = { id ->
                reporteSeleccionado = id
                pantallaAnterior = "historial_reportes"
                pantalla = "detalle_reporte"
            }
        )

        "detalle_reporte" -> PantallaDetalleReporte(
            idReporte = reporteSeleccionado,
            alVolver = { pantalla = pantallaAnterior },
            alEditar = { pantalla = "editar_reporte" }
        )

        "editar_reporte" -> PantallaEditarReporte(
            idReporte = reporteSeleccionado,
            alVolver = { pantalla = "detalle_reporte" }
        )

        "panel_supervisor" -> PanelSupervisor(
            consultarReportes = {
                pantalla = "revision_supervisor"
            },
            alSalir = {
                pantalla = "bienvenida"
            }
        )

        "revision_supervisor" -> PantallaRevisionSupervisor(
            alVolver = {
                pantalla = "panel_supervisor"
            }
        )
    }
}

// LOGO
@Composable
fun LogoVitairos() {

    Image(
        painter = painterResource(
            id = R.drawable.logo_vitairos
        ),
        contentDescription = "Logo VITAIROS",
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp),
        contentScale = ContentScale.Fit
    )
}

// BIENVENIDA
@Composable
fun PantallaBienvenida(
    entrarOperador: () -> Unit,
    entrarSupervisor: () -> Unit
) {

    ColumnaPantalla {

        Spacer(Modifier.height(45.dp))
        LogoVitairos()
        Spacer(Modifier.height(25.dp))

        Titulo("VITAIROS INDUSTRIAL")
        Spacer(Modifier.height(12.dp))
        TextoSecundario("Gestión de reportes industriales")
        Spacer(Modifier.height(40.dp))

        Titulo("SELECCIONE SU ACCESO")
        Spacer(Modifier.height(25.dp))

        BotonVerde("INGRESAR COMO OPERADOR") {
            entrarOperador()
        }

        Spacer(Modifier.height(18.dp))

        BotonVerde("INGRESAR COMO SUPERVISOR") {
            entrarSupervisor()
        }
    }
}

// INICIO DE SESIÓN
@Composable
fun PantallaAcceso(
    tipoUsuario: String,
    alVolver: () -> Unit,
    alEntrar: () -> Unit
) {

    var usuario by rememberSaveable(tipoUsuario) {
        mutableStateOf("")
    }

    var contrasena by rememberSaveable(tipoUsuario) {
        mutableStateOf("")
    }

    var error by rememberSaveable(tipoUsuario) {
        mutableStateOf("")
    }

    ColumnaPantalla {

        Spacer(Modifier.height(30.dp))
        LogoVitairos()
        Spacer(Modifier.height(20.dp))
        Titulo("ACCESO $tipoUsuario")
        Spacer(Modifier.height(30.dp))

        CampoTexto(
            valor = usuario,
            alCambiar = {
                usuario = it
                error = ""
            },
            etiqueta = "Usuario"
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = contrasena,
            onValueChange = {
                contrasena = it
                error = ""
            },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = coloresCampos()
        )

        if (error.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            TextoError(error)
        }

        Spacer(Modifier.height(30.dp))

        BotonVerde("INICIAR SESIÓN") {

            val correcto =
                if (tipoUsuario == "OPERADOR") {
                    "operador"
                } else {
                    "supervisor"
                }

            if (
                usuario.trim() == correcto &&
                contrasena == "1234"
            ) {
                alEntrar()
            } else {
                error = "Usuario o contraseña incorrectos"
            }
        }

        Spacer(Modifier.height(20.dp))
        BotonVolver(alVolver)
    }
}

// PANEL DEL OPERADOR
@Composable
fun PanelOperador(
    nuevoReporte: () -> Unit,
    verReportes: () -> Unit,
    verHistorial: () -> Unit,
    verSincronizacion: () -> Unit,
    alSalir: () -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel = remember { ReportesViewModel(app) }
    val reportes by viewModel.reportes.collectAsState()

    val cntEnCola = reportes.count { it.estado == "EN_COLA" || it.estado == "PENDIENTE" }
    val cntError = reportes.count { it.estado == "ERROR" || it.estado == "REQUIERE_CORRECCIÓN" }
    val cntBorrador = reportes.count { it.estado == "BORRADOR" }
    val cntSync = reportes.count { it.estado == "SINCRONIZADO" || it.estado == "APROBADO" }
    val atencion = reportes.filter { it.estado == "ERROR" || it.estado == "REQUIERE_CORRECCIÓN" }
    val recientes = reportes.take(3)

    ColumnaPantalla {

        LogoVitairos()
        Spacer(Modifier.height(8.dp))
        Titulo("PANEL DEL OPERADOR")
        Spacer(Modifier.height(4.dp))
        TextoSecundario("Gestión de reportes en terreno")
        Spacer(Modifier.height(20.dp))

        // Alerta de reportes que necesitan atención
        if (atencion.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1010)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "⚠ ${atencion.size} reporte(s) necesitan atención",
                        color = Color(0xFFFFB4AB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    atencion.take(2).forEach { r ->
                        Text(
                            "${r.identificadorDocumento} · ${r.estado}",
                            color = Cobre,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // Tiles de estado 2×2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TileEstado("En cola", cntEnCola, Oro, Modifier.weight(1f)) { verSincronizacion() }
            TileEstado("Error / Correcc.", cntError, Cobre, Modifier.weight(1f)) { verReportes() }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TileEstado("Borrador", cntBorrador, Acero, Modifier.weight(1f)) { verReportes() }
            TileEstado("Sincronizado", cntSync, Color(0xFF69F0AE), Modifier.weight(1f)) { verHistorial() }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = nuevoReporte,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cobre)
        ) {
            Text("＋  NUEVO REPORTE", color = VerdeNoche, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(12.dp))
        BotonVerde("REPORTES GUARDADOS") { verReportes() }
        Spacer(Modifier.height(10.dp))
        BotonVerde("SINCRONIZACIÓN") { verSincronizacion() }
        Spacer(Modifier.height(10.dp))
        BotonVerde("HISTORIAL") { verHistorial() }

        // Actividad reciente
        if (recientes.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            Text(
                "ACTIVIDAD RECIENTE",
                color = Acero,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))
            recientes.forEach { reporte ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VerdeProfundo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                reporte.identificadorDocumento,
                                color = Hueso,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(reporte.tipoReporte, color = Acero, fontSize = 12.sp)
                        }
                        Text(
                            "● ${reporte.estado}",
                            color = colorDeEstado(reporte.estado),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(20.dp))
        BotonVolver(alPulsar = alSalir, texto = "CERRAR SESIÓN")
    }
}

// TILE DE ESTADO para el dashboard
@Composable
private fun TileEstado(
    etiqueta: String,
    cantidad: Int,
    color: Color,
    modifier: Modifier = Modifier,
    alPulsar: () -> Unit
) {
    Button(
        onClick = alPulsar,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = VerdeProfundo)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = cantidad.toString(),
                color = if (cantidad > 0) color else Acero,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(text = etiqueta, color = Acero, fontSize = 10.sp)
        }
    }
}

// PANEL DEL SUPERVISOR
@Composable
fun PanelSupervisor(
    consultarReportes: () -> Unit,
    alSalir: () -> Unit
) {

    ColumnaPantalla {

        LogoVitairos()
        Spacer(Modifier.height(15.dp))
        Titulo("PANEL DEL SUPERVISOR")
        Spacer(Modifier.height(12.dp))
        TextoSecundario("Revisión y seguimiento de reportes")
        Spacer(Modifier.height(35.dp))

        BotonVerde("CONSULTAR REPORTES") {
            consultarReportes()
        }

        Spacer(Modifier.height(15.dp))

        BotonVerde("ESTADO DE REPORTES") {
            consultarReportes()
        }

        Spacer(Modifier.height(30.dp))

        BotonVolver(
            alPulsar = alSalir,
            texto = "CERRAR SESIÓN"
        )
    }
}

// NUEVO REPORTE
@Composable
fun PantallaNuevoReporte(
    alVolver: () -> Unit
) {

    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()

    val baseDatos = remember(contexto) {
        VitairosDatabase.obtenerBaseDatos(contexto)
    }

    var identificador by rememberSaveable {
        val f = SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())
        mutableStateOf("VIA-$f-${(1000..9999).random()}")
    }
    var tipoReporte by rememberSaveable { mutableStateOf("") }
    var proceso by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf("") }
    var equipo by rememberSaveable { mutableStateOf("") }
    var turno by rememberSaveable { mutableStateOf("") }
    var severidad by rememberSaveable { mutableStateOf("") }
    var ubicacion by rememberSaveable { mutableStateOf("") }
    var observaciones by rememberSaveable { mutableStateOf("") }
    var archivoUri by rememberSaveable { mutableStateOf("") }
    var nombreArchivo by rememberSaveable { mutableStateOf("") }
    var fotografia by remember { mutableStateOf<Bitmap?>(null) }
    var mensaje by rememberSaveable { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }

    var errorEquipo by remember { mutableStateOf<String?>(null) }

    val necesitaEquipo = tipoReporte in TIPOS_CON_EQUIPO
    val esIncidente = tipoReporte == "Incidente o hallazgo"

    val fechaHora = rememberSaveable {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
    }

    val abrirCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { foto: Bitmap? ->
        if (foto != null) {
            fotografia = foto
            archivoUri = ""
            nombreArchivo = "Fotografía capturada"
            mensaje = ""
        } else {
            mensaje = "No se capturó ninguna fotografía"
        }
    }

    val solicitarPermisoCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            abrirCamara.launch(null)
        } else {
            mensaje = "Permiso de cámara denegado. Actívelo en Ajustes del celular."
        }
    }

    val seleccionarImagen = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            archivoUri = uri.toString()
            fotografia = null
            nombreArchivo = "Imagen seleccionada"
            mensaje = ""
        }
    }

    val seleccionarPdf = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            archivoUri = uri.toString()
            fotografia = null
            nombreArchivo = "PDF seleccionado"
            mensaje = ""
        }
    }

    ColumnaPantalla {

        Titulo("NUEVO REPORTE")
        Spacer(Modifier.height(10.dp))
        TextoSecundario("Registro de reporte operacional")
        Spacer(Modifier.height(25.dp))

        CampoTexto(
            identificador,
            { identificador = it },
            "ID del reporte (ej: VIA-261019-0001)"
        )

        Spacer(Modifier.height(14.dp))

        DropdownSelector(
            valor = tipoReporte,
            opciones = TIPOS_REPORTE,
            etiqueta = "Tipo de reporte",
            alSeleccionar = {
                tipoReporte = it
                if (it !in TIPOS_CON_EQUIPO) equipo = ""
            }
        )

        Spacer(Modifier.height(14.dp))

        DropdownSelector(
            valor = area,
            opciones = AREAS,
            etiqueta = "Área / Faena",
            alSeleccionar = { area = it }
        )

        Spacer(Modifier.height(14.dp))

        DropdownSelector(
            valor = proceso,
            opciones = PROCESOS,
            etiqueta = "Proceso asociado",
            alSeleccionar = { proceso = it }
        )

        Spacer(Modifier.height(14.dp))

        DropdownSelector(
            valor = turno,
            opciones = TURNOS,
            etiqueta = "Turno",
            alSeleccionar = { turno = it }
        )

        AnimatedVisibility(visible = necesitaEquipo) {
            Column {
                Spacer(Modifier.height(14.dp))
                CampoTexto(
                    equipo,
                    {
                        equipo = it
                        errorEquipo = if (it.trim().length < 3 && it.isNotEmpty())
                            "Mínimo 3 caracteres" else null
                    },
                    "Equipo / TAG (ej: CV-104)",
                    error = errorEquipo
                )
            }
        }

        AnimatedVisibility(visible = esIncidente) {
            Column {
                Spacer(Modifier.height(14.dp))
                DropdownSelector(
                    valor = severidad,
                    opciones = SEVERIDADES,
                    etiqueta = "Severidad del incidente",
                    alSeleccionar = { severidad = it }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        SelectorUbicacion(
            ubicacion = ubicacion,
            alObtenerUbicacion = { ubicacion = it }
        )

        Spacer(Modifier.height(14.dp))

        Text("Fecha y hora: $fechaHora", color = Blanco)

        Spacer(Modifier.height(10.dp))

        Text("Usuario: OPERADOR", color = Blanco)

        Spacer(Modifier.height(20.dp))

        CampoTexto(
            observaciones,
            { observaciones = it },
            "Observaciones (máx. 500 caracteres)",
            false,
            maxCaracteres = 500
        )

        Spacer(Modifier.height(25.dp))
        Titulo("DOCUMENTO O EVIDENCIA")
        Spacer(Modifier.height(15.dp))

        BotonVerde("TOMAR FOTOGRAFÍA") {
            val tienePermiso = ContextCompat.checkSelfPermission(
                contexto, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (tienePermiso) {
                abrirCamara.launch(null)
            } else {
                solicitarPermisoCamara.launch(Manifest.permission.CAMERA)
            }
        }
        Spacer(Modifier.height(12.dp))
        BotonVerde("SELECCIONAR IMAGEN") { seleccionarImagen.launch("image/*") }
        Spacer(Modifier.height(12.dp))
        BotonVerde("SELECCIONAR PDF") { seleccionarPdf.launch("application/pdf") }

        Spacer(Modifier.height(20.dp))

        if (fotografia != null) {

            Titulo("VISTA PREVIA")
            Spacer(Modifier.height(12.dp))

            Image(
                bitmap = fotografia!!.asImageBitmap(),
                contentDescription = "Fotografía capturada",
                modifier = Modifier.fillMaxWidth().height(230.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(12.dp))

            BotonVerde("REPETIR FOTOGRAFÍA") { abrirCamara.launch(null) }

        } else if (archivoUri.isNotEmpty()) {

            TextoSecundario(nombreArchivo)

            if (nombreArchivo == "Imagen seleccionada") {
                val imagen = cargarImagenSeleccionada(archivoUri)
                if (imagen != null) {
                    Spacer(Modifier.height(12.dp))
                    Image(
                        bitmap = imagen.asImageBitmap(),
                        contentDescription = "Imagen seleccionada",
                        modifier = Modifier.fillMaxWidth().height(230.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            if (nombreArchivo == "PDF seleccionado") {
                Spacer(Modifier.height(10.dp))
                TextoSecundario("PDF seleccionado para guardar")
            }

        } else {
            TextoSecundario("No se ha seleccionado ningún archivo")
        }

        Spacer(Modifier.height(25.dp))

        Button(
            onClick = {

                val faltaCampo = identificador.isBlank() ||
                        tipoReporte.isBlank() ||
                        area.isBlank() ||
                        proceso.isBlank()

                val faltaEquipo = necesitaEquipo && equipo.isBlank()
                val faltaSeveridad = esIncidente && severidad.isBlank()

                when {
                    faltaCampo ->
                        mensaje = "Complete identificador, tipo, área y proceso"

                    faltaEquipo ->
                        mensaje = "Ingrese el equipo o TAG"

                    faltaSeveridad ->
                        mensaje = "Seleccione la severidad del incidente"

                    fotografia == null && archivoUri.isBlank() ->
                        mensaje = "Debe adjuntar una fotografía, imagen o PDF"

                    !guardando -> {
                        guardando = true
                        mensaje = "Guardando reporte..."

                        val idDocumento = identificador.trim()
                        val tipo = tipoReporte.trim()
                        val areaTrim = area.trim()
                        val proc = proceso.trim()
                        val turnTrim = turno.trim()
                        val eq = equipo.trim()
                        val sev = severidad.trim()
                        val ubic = ubicacion.trim()
                        val obs = observaciones.trim()
                        val uriActual = archivoUri
                        val fotoActual = fotografia
                        val nombreActual = nombreArchivo

                        alcance.launch {

                            var archivoCopiado: File? = null

                            try {
                                withContext(Dispatchers.IO) {

                                    val archivo = guardarArchivoLocal(
                                        contexto, fotoActual, uriActual, nombreActual
                                    )

                                    archivoCopiado = archivo

                                    val reporte = Reporte(
                                        identificadorDocumento = idDocumento,
                                        tipoReporte = tipo,
                                        version = "v1",
                                        proceso = proc,
                                        fechaHora = fechaHora,
                                        usuario = "OPERADOR",
                                        observaciones = obs,
                                        archivoLocal = archivo.absolutePath,
                                        estado = "EN_COLA",
                                        observacionSupervisor = "",
                                        area = areaTrim,
                                        equipo = eq,
                                        turno = turnTrim,
                                        severidad = sev,
                                        ubicacion = ubic
                                    )

                                    baseDatos.reporteDao().guardarReporte(reporte)
                                }

                                mensaje = "Reporte guardado correctamente sin Internet"

                                val newF = SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())
                                identificador = "VIA-$newF-${(1000..9999).random()}"
                                tipoReporte = ""
                                area = ""
                                proceso = ""
                                turno = ""
                                equipo = ""
                                severidad = ""
                                ubicacion = ""
                                observaciones = ""
                                archivoUri = ""
                                nombreArchivo = ""
                                fotografia = null

                            } catch (e: Exception) {
                                withContext(Dispatchers.IO) { archivoCopiado?.delete() }
                                mensaje = "No se pudo guardar: " +
                                        (e.localizedMessage ?: "Error desconocido")
                            } finally {
                                guardando = false
                            }
                        }
                    }
                }
            },
            enabled = !guardando,
            modifier = Modifier.fillMaxWidth().height(55.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VerdeBoton)
        ) {
            Text(
                text = if (guardando) "GUARDANDO..." else "GUARDAR REPORTE",
                color = Blanco,
                fontWeight = FontWeight.Bold
            )
        }

        if (mensaje.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            TextoSecundario(mensaje)
        }

        Spacer(Modifier.height(25.dp))
        BotonVolver(alVolver)
    }
}

// REPORTES GUARDADOS
@Composable
fun PantallaReportesGuardados(
    alAbrirReporte: (Int) -> Unit,
    alVolver: () -> Unit
) {

    val app = LocalContext.current.applicationContext as Application
    val viewModel = remember { ReportesViewModel(app) }
    val reportes by viewModel.reportes.collectAsState()

    var reporteAEliminar by remember { mutableStateOf<Reporte?>(null) }

    // Diálogo de confirmación para eliminar
    reporteAEliminar?.let { reporte ->
        AlertDialog(
            onDismissRequest = { reporteAEliminar = null },
            title = { Text("Eliminar reporte", color = Color(0xFFFBF7F1)) },
            text = {
                Text(
                    "¿Eliminar el reporte N.º ${reporte.id} (${reporte.tipoReporte})? Esta acción no se puede deshacer.",
                    color = Color(0xFFAAB6B7)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminar(reporte)
                    reporteAEliminar = null
                }) {
                    Text("ELIMINAR", color = Color(0xFFDD7A52), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { reporteAEliminar = null }) {
                    Text("CANCELAR", color = Color(0xFFAAB6B7))
                }
            },
            containerColor = Color(0xFF102B23)
        )
    }

    ColumnaPantalla {

        Titulo("REPORTES GUARDADOS")
        Spacer(Modifier.height(12.dp))
        TextoSecundario("Reportes almacenados en este celular")
        Spacer(Modifier.height(25.dp))

        if (reportes.isEmpty()) {

            TextoSecundario("Todavía no hay reportes guardados")

        } else {

            Text(
                text = "Total de reportes: ${reportes.size}",
                color = Blanco,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(20.dp))

            reportes.forEach { reporte ->

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF12392D)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            "REPORTE N.º ${reporte.id}",
                            color = Blanco,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(Modifier.height(10.dp))

                        TextoDato(
                            "Documento",
                            reporte.identificadorDocumento
                        )

                        TextoDato("Tipo", reporte.tipoReporte)
                        TextoDato("Proceso", reporte.proceso)
                        TextoDato("Fecha", reporte.fechaHora)
                        TextoDato("Estado", reporte.estado)

                        if (
                            reporte.observacionSupervisor.isNotBlank()
                        ) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Observación: ${reporte.observacionSupervisor}",
                                color = Color(0xFFFFB4AB),
                                fontSize = 13.sp
                            )
                        }

                        if (reporte.ubicacion.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "📍 ${reporte.ubicacion}",
                                color = Color(0xFFAAB6B7),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(Modifier.height(15.dp))

                        val colorEstado = colorDeEstado(reporte.estado)
                        Text(
                            text = "● ${reporte.estado}",
                            color = colorEstado,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { alAbrirReporte(reporte.id) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00A859)
                                )
                            ) {
                                Text("VER", color = Blanco, fontWeight = FontWeight.Bold)
                            }

                            if (reporte.estado == "ERROR") {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.actualizar(reporte.copy(estado = "EN_COLA"))
                                    },
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFFD9B46C)
                                    )
                                ) {
                                    Text("REINTENTAR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            if (reporte.estado in listOf(
                                    "BORRADOR", "EN_COLA", "PENDIENTE", "REQUIERE_CORRECCIÓN", "ERROR"
                                )
                            ) {
                                OutlinedButton(
                                    onClick = { reporteAEliminar = reporte },
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFFDD7A52)
                                    )
                                ) {
                                    Text("ELIMINAR", fontWeight = FontWeight.Bold)
                                }
                            }
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

// VER Y CORREGIR REPORTE
@Composable
fun PantallaEditarReporte(
    idReporte: Int,
    alVolver: () -> Unit
) {

    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()

    val baseDatos = remember(contexto) {
        VitairosDatabase.obtenerBaseDatos(contexto)
    }

    var reporteOriginal by remember(idReporte) {
        mutableStateOf<Reporte?>(null)
    }

    var cargando by remember(idReporte) {
        mutableStateOf(true)
    }

    var identificador by remember(idReporte) { mutableStateOf("") }
    var tipoReporte by remember(idReporte) { mutableStateOf("") }
    var proceso by remember(idReporte) { mutableStateOf("") }
    var area by remember(idReporte) { mutableStateOf("") }
    var equipo by remember(idReporte) { mutableStateOf("") }
    var turno by remember(idReporte) { mutableStateOf("") }
    var severidad by remember(idReporte) { mutableStateOf("") }
    var observaciones by remember(idReporte) { mutableStateOf("") }
    var mensaje by remember(idReporte) { mutableStateOf("") }

    var guardando by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(idReporte) {

        cargando = true

        val encontrado = withContext(Dispatchers.IO) {
            baseDatos.reporteDao().buscarReporte(idReporte)
        }

        reporteOriginal = encontrado

        if (encontrado != null) {
            identificador = encontrado.identificadorDocumento
            tipoReporte = encontrado.tipoReporte
            proceso = encontrado.proceso
            area = encontrado.area
            equipo = encontrado.equipo
            turno = encontrado.turno
            severidad = encontrado.severidad
            observaciones = encontrado.observaciones
        }

        cargando = false
    }

    ColumnaPantalla {

        Titulo("VER Y CORREGIR REPORTE")
        Spacer(Modifier.height(15.dp))

        if (cargando) {

            CircularProgressIndicator(
                color = VerdeBoton
            )

        } else if (reporteOriginal == null) {

            TextoError("No se encontró el reporte")

        } else {

            val reporte = reporteOriginal!!

            TextoSecundario("REPORTE N.º ${reporte.id}")

            Spacer(Modifier.height(20.dp))

            TextoDato("Fecha", reporte.fechaHora)
            TextoDato("Usuario", reporte.usuario)
            TextoDato("Estado", reporte.estado)

            if (reporte.estado == "RECHAZADO" || reporte.estado == "REQUIERE_CORRECCIÓN") {

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "MOTIVO DEL RECHAZO",
                    color = Color(0xFFFFB4AB),
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(10.dp))

                TextoSecundario(
                    reporte.observacionSupervisor.ifBlank {
                        "El supervisor no registró un motivo."
                    }
                )

                Spacer(Modifier.height(15.dp))

                TextoSecundario(
                    "Corrija la información y envíe nuevamente el reporte."
                )
            }

            val necesitaEquipoEditar = tipoReporte in TIPOS_CON_EQUIPO
            val esIncidenteEditar = tipoReporte == "Incidente o hallazgo"

            Spacer(Modifier.height(20.dp))

            CampoTexto(
                identificador,
                { identificador = it },
                "Identificador del documento"
            )

            Spacer(Modifier.height(14.dp))

            DropdownSelector(
                valor = tipoReporte,
                opciones = TIPOS_REPORTE,
                etiqueta = "Tipo de reporte",
                alSeleccionar = {
                    tipoReporte = it
                    if (it !in TIPOS_CON_EQUIPO) equipo = ""
                }
            )

            Spacer(Modifier.height(14.dp))

            DropdownSelector(
                valor = area,
                opciones = AREAS,
                etiqueta = "Área / Faena",
                alSeleccionar = { area = it }
            )

            Spacer(Modifier.height(14.dp))

            DropdownSelector(
                valor = proceso,
                opciones = PROCESOS,
                etiqueta = "Proceso asociado",
                alSeleccionar = { proceso = it }
            )

            Spacer(Modifier.height(14.dp))

            DropdownSelector(
                valor = turno,
                opciones = TURNOS,
                etiqueta = "Turno",
                alSeleccionar = { turno = it }
            )

            AnimatedVisibility(visible = necesitaEquipoEditar) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    CampoTexto(
                        equipo,
                        { equipo = it },
                        "Equipo / TAG (ej: CV-104)"
                    )
                }
            }

            AnimatedVisibility(visible = esIncidenteEditar) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    DropdownSelector(
                        valor = severidad,
                        opciones = SEVERIDADES,
                        etiqueta = "Severidad del incidente",
                        alSeleccionar = { severidad = it }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            CampoTexto(
                observaciones,
                { observaciones = it },
                "Observaciones",
                false
            )

            Spacer(Modifier.height(25.dp))

            Titulo("ARCHIVO ADJUNTO")
            Spacer(Modifier.height(10.dp))

            TextoSecundario(
                File(reporte.archivoLocal).name
            )

            Spacer(Modifier.height(12.dp))

            val esPdf = reporte.archivoLocal
                .endsWith(".pdf", ignoreCase = true)

            if (esPdf) {

                TextoSecundario("PDF guardado en el celular")

            } else {

                val imagen = cargarImagenLocal(
                    reporte.archivoLocal
                )

                if (imagen != null) {

                    Image(
                        bitmap = imagen.asImageBitmap(),
                        contentDescription = "Imagen del reporte",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentScale = ContentScale.Fit
                    )

                } else {

                    TextoSecundario("No se pudo mostrar la imagen")
                }
            }

            Spacer(Modifier.height(25.dp))

            if (
                reporte.estado in listOf(
                    "BORRADOR", "EN_COLA", "PENDIENTE",
                    "RECHAZADO", "REQUIERE_CORRECCIÓN"
                )
            ) {

                val textoBoton = when (reporte.estado) {
                    "RECHAZADO", "REQUIERE_CORRECCIÓN" -> "CORREGIR Y REENVIAR"
                    else -> "GUARDAR CAMBIOS"
                }

                Button(
                    onClick = {

                        if (
                            identificador.isBlank() ||
                            tipoReporte.isBlank() ||
                            proceso.isBlank() ||
                            area.isBlank()
                        ) {

                            mensaje = "Complete identificador, tipo, área y proceso"

                        } else if (!guardando) {

                            guardando = true
                            mensaje = "Guardando cambios..."

                            val nuevoEstado = when (reporte.estado) {
                                "RECHAZADO", "REQUIERE_CORRECCIÓN" -> "EN_COLA"
                                "BORRADOR" -> "EN_COLA"
                                else -> reporte.estado
                            }

                            val actualizado = reporte.copy(
                                identificadorDocumento = identificador.trim(),
                                tipoReporte = tipoReporte.trim(),
                                proceso = proceso.trim(),
                                area = area.trim(),
                                equipo = equipo.trim(),
                                turno = turno.trim(),
                                severidad = severidad.trim(),
                                observaciones = observaciones.trim(),
                                estado = nuevoEstado,
                                observacionSupervisor = ""
                            )

                            alcance.launch {

                                try {

                                    withContext(Dispatchers.IO) {
                                        baseDatos.reporteDao()
                                            .actualizarReporte(actualizado)
                                    }

                                    reporteOriginal = actualizado

                                    mensaje =
                                        "Reporte actualizado y enviado nuevamente a revisión"

                                } catch (e: Exception) {

                                    mensaje = "Error: " +
                                            (e.localizedMessage
                                                ?: "No se pudo actualizar")

                                } finally {

                                    guardando = false
                                }
                            }
                        }
                    },
                    enabled = !guardando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VerdeBoton
                    )
                ) {

                    Text(
                        text = if (guardando) {
                            "GUARDANDO..."
                        } else {
                            textoBoton
                        },
                        color = Blanco,
                        fontWeight = FontWeight.Bold
                    )
                }

            } else {

                TextoSecundario(
                    "Este reporte ya fue sincronizado y no se puede modificar."
                )
            }

            if (mensaje.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                TextoSecundario(mensaje)
            }
        }

        Spacer(Modifier.height(25.dp))
        BotonVolver(alVolver)
    }
}

// DETALLE DE REPORTE (solo lectura)
@Composable
fun PantallaDetalleReporte(
    idReporte: Int,
    alVolver: () -> Unit,
    alEditar: () -> Unit
) {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as Application
    val viewModel = remember { ReportesViewModel(app) }
    var reporte by remember { mutableStateOf<Reporte?>(null) }

    LaunchedEffect(idReporte) {
        reporte = viewModel.buscar(idReporte)
    }

    val r = reporte

    if (r == null) {
        ColumnaPantalla {
            Titulo("DETALLE DEL REPORTE")
            Spacer(Modifier.height(30.dp))
            CircularProgressIndicator(color = Cobre, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        return
    }

    val esEditable = r.estado in listOf(
        "BORRADOR", "EN_COLA", "PENDIENTE", "RECHAZADO", "REQUIERE_CORRECCIÓN"
    )

    ColumnaPantalla {

        Titulo("DETALLE DEL REPORTE")
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(r.identificadorDocumento, color = Oro, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorDeEstado(r.estado).copy(alpha = 0.18f)
                )
            ) {
                Text(
                    r.estado,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = colorDeEstado(r.estado),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = VerdeProfundo)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TextoDato("Tipo de reporte", r.tipoReporte)
                TextoDato("Proceso", r.proceso)
                TextoDato("Área / Faena", r.area)
                if (r.turno.isNotBlank()) TextoDato("Turno", r.turno)
                if (r.equipo.isNotBlank()) TextoDato("Equipo / TAG", r.equipo)
                if (r.severidad.isNotBlank()) TextoDato("Severidad", r.severidad)
                TextoDato("Fecha y hora", r.fechaHora)
                TextoDato("Usuario", r.usuario)
                TextoDato("Versión", r.version)
                if (r.ubicacion.isNotBlank()) TextoDato("Ubicación GPS", r.ubicacion)
                if (r.observaciones.isNotBlank()) TextoDato("Observaciones", r.observaciones)
            }
        }

        if (r.observacionSupervisor.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1010))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "MOTIVO / OBSERVACIÓN SUPERVISOR",
                        color = Color(0xFFFFB4AB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(r.observacionSupervisor, color = Hueso, fontSize = 14.sp)
                }
            }
        }

        if (r.archivoLocal.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VerdeMineral)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("📎", fontSize = 18.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Adjunto", color = Acero, fontSize = 12.sp)
                        Text(
                            r.archivoLocal.substringAfterLast("/"),
                            color = Hueso,
                            fontSize = 13.sp,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        if (esEditable) {
            BotonVerde("EDITAR REPORTE") { alEditar() }
            Spacer(Modifier.height(12.dp))
        }

        BotonVolver(alVolver)
    }
}

// CARGAR IMAGEN GUARDADA
@Composable
fun cargarImagenLocal(
    ruta: String
): Bitmap? {

    return remember(ruta) {

        try {

            val opciones = BitmapFactory.Options().apply {
                inSampleSize = 2
            }

            BitmapFactory.decodeFile(ruta, opciones)

        } catch (e: Exception) {
            null
        }
    }
}

// MOSTRAR DATOS
@Composable
fun TextoDato(
    nombre: String,
    valor: String
) {

    Text(
        text = "$nombre: $valor",
        color = Blanco,
        fontSize = 14.sp
    )

    Spacer(Modifier.height(6.dp))
}

// GUARDAR ARCHIVO LOCAL
private fun guardarArchivoLocal(
    contexto: Context,
    fotografia: Bitmap?,
    archivoUri: String,
    nombreArchivo: String
): File {

    val carpeta = File(
        contexto.filesDir,
        "reportes_adjuntos"
    )

    if (!carpeta.exists() && !carpeta.mkdirs()) {
        throw IllegalStateException(
            "No se pudo crear la carpeta de adjuntos"
        )
    }

    val extension = when {

        fotografia != null -> "jpg"

        nombreArchivo == "PDF seleccionado" -> "pdf"

        else -> "imagen"
    }

    val archivo = File(
        carpeta,
        "${UUID.randomUUID()}.$extension"
    )

    try {

        if (fotografia != null) {

            archivo.outputStream().use { salida ->

                val correcto = fotografia.compress(
                    Bitmap.CompressFormat.JPEG,
                    90,
                    salida
                )

                if (!correcto) {
                    throw IllegalStateException(
                        "No se pudo guardar la fotografía"
                    )
                }
            }

        } else {

            val uri = Uri.parse(archivoUri)

            val entrada = contexto.contentResolver
                .openInputStream(uri)
                ?: throw IllegalStateException(
                    "No se pudo abrir el archivo seleccionado"
                )

            entrada.use { origen ->

                archivo.outputStream().use { destino ->
                    origen.copyTo(destino)
                }
            }
        }

        return archivo

    } catch (e: Exception) {

        archivo.delete()
        throw e
    }
}

// VISTA PREVIA DE IMAGEN SELECCIONADA
@Composable
fun cargarImagenSeleccionada(
    archivoUri: String
): Bitmap? {

    val contexto = LocalContext.current

    return remember(archivoUri) {

        try {

            val uri = Uri.parse(archivoUri)

            contexto.contentResolver
                .openInputStream(uri)
                ?.use { entrada ->

                    val opciones = BitmapFactory.Options().apply {
                        inSampleSize = 4
                    }

                    BitmapFactory.decodeStream(
                        entrada,
                        null,
                        opciones
                    )
                }

        } catch (e: Exception) {
            null
        }
    }
}

// ESTRUCTURA DE PANTALLAS
@Composable
fun ColumnaPantalla(
    contenido: @Composable ColumnScope.() -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VerdeOscuro)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = contenido
    )
}

// DROPDOWN SELECTOR
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    valor: String,
    opciones: List<String>,
    etiqueta: String,
    alSeleccionar: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = !expandido }
    ) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            colors = coloresCampos()
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion) },
                    onClick = {
                        alSeleccionar(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}

// CAMPOS DE TEXTO
@Composable
fun CampoTexto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    unaLinea: Boolean = true,
    error: String? = null,
    maxCaracteres: Int = Int.MAX_VALUE
) {

    OutlinedTextField(
        value = valor,
        onValueChange = { if (it.length <= maxCaracteres) alCambiar(it) },
        label = { Text(etiqueta) },
        singleLine = unaLinea,
        minLines = if (unaLinea) 1 else 3,
        modifier = Modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = when {
            error != null -> {{ Text(error, color = Color(0xFFDD7A52)) }}
            maxCaracteres < Int.MAX_VALUE -> {{
                Text(
                    "${valor.length} / $maxCaracteres",
                    color = if (valor.length >= maxCaracteres) Color(0xFFDD7A52) else Color(0xFFAAB6B7)
                )
            }}
            else -> null
        },
        colors = coloresCampos()
    )
}

// COLORES DE CAMPOS
@Composable
fun coloresCampos() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = Blanco,
        unfocusedTextColor = Blanco,
        focusedLabelColor = Blanco,
        unfocusedLabelColor = Blanco,
        focusedBorderColor = VerdeBoton,
        unfocusedBorderColor = Color.LightGray,
        cursorColor = Blanco
    )

// TÍTULOS
@Composable
fun Titulo(texto: String) {

    Text(
        text = texto,
        color = Blanco,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

// TEXTO SECUNDARIO
@Composable
fun TextoSecundario(texto: String) {

    Text(
        text = texto,
        color = Color.LightGray,
        textAlign = TextAlign.Center
    )
}

// ERRORES
@Composable
fun TextoError(texto: String) {

    Text(
        text = texto,
        color = Color(0xFFFFB4AB),
        textAlign = TextAlign.Center
    )
}

// BOTÓN VERDE
@Composable
fun BotonVerde(
    texto: String,
    alPulsar: () -> Unit
) {

    Button(
        onClick = alPulsar,
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VerdeBoton
        )
    ) {

        Text(
            text = texto,
            color = Blanco,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

// BOTÓN VOLVER
@Composable
fun BotonVolver(
    alPulsar: () -> Unit,
    texto: String = "VOLVER"
) {

    TextButton(onClick = alPulsar) {

        Text(
            text = texto,
            color = Blanco
        )
    }
}
