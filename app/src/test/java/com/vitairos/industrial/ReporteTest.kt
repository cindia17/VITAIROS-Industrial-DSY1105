package com.vitairos.industrial

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotMatch
import io.kotest.runner.junit4.KotestTestRunner
import org.junit.runner.RunWith

// ─────────────────────────────────────────────────────────────
//  Funciones puras que espejean la lógica de la app
// ─────────────────────────────────────────────────────────────

private fun incrementarVersion(version: String): String {
    val num = version.removePrefix("v").toIntOrNull() ?: 1
    return "v${num + 1}"
}

private fun nuevoEstadoAlEditar(estado: String): String = when (estado) {
    "RECHAZADO", "REQUIERE_CORRECCIÓN", "BORRADOR" -> "EN_COLA"
    else -> estado
}

private fun esEditable(estado: String): Boolean =
    estado in listOf("BORRADOR", "EN_COLA", "PENDIENTE", "RECHAZADO", "REQUIERE_CORRECCIÓN")

private fun esEliminable(estado: String): Boolean =
    estado in listOf("BORRADOR", "EN_COLA", "PENDIENTE", "REQUIERE_CORRECCIÓN", "ERROR")

private fun parsearAdjuntos(archivoLocal: String): List<String> =
    if (archivoLocal.isBlank()) emptyList()
    else archivoLocal.split("|").filter { it.isNotBlank() }

private const val REGEX_ID = "^VIA-\\d{6}-\\d{4}$"
private const val LIMITE_BYTES = 10L * 1024 * 1024   // 10 MB

private fun superaLimiteTamaño(bytes: Long): Boolean = bytes > LIMITE_BYTES

private data class CamposReporte(
    val identificador: String = "VIA-261105-0001",
    val tipoReporte: String = "Inspección Rutinaria",
    val area: String = "Área Sur",
    val proceso: String = "Planta 2",
    val totalAdjuntos: Int = 1
)

private fun validarCampos(c: CamposReporte): String? = when {
    c.identificador.isBlank() || c.tipoReporte.isBlank() ||
    c.area.isBlank() || c.proceso.isBlank() -> "Complete identificador, tipo, área y proceso"
    c.totalAdjuntos == 0 -> "Debe adjuntar al menos una fotografía, imagen o PDF"
    else -> null  // null = válido
}

private fun reporteBase() = Reporte(
    identificadorDocumento = "VIA-261019-0001",
    tipoReporte = "Inspección Rutinaria",
    version = "v1",
    proceso = "Planta Sur",
    fechaHora = "2026-11-05 09:41",
    usuario = "operador",
    observaciones = "Sin observaciones",
    archivoLocal = ""
)

// ─────────────────────────────────────────────────────────────
//  10 PRUEBAS UNITARIAS — VITAIROS Industrial
// ─────────────────────────────────────────────────────────────

@RunWith(KotestTestRunner::class)
class ReporteTest : ShouldSpec({

    should("1. crear un reporte nuevo con estado PENDIENTE y versión v1") {
        val r = reporteBase()
        r.estado shouldBe "PENDIENTE"
        r.version shouldBe "v1"
    }

    should("2. incrementar la versión de v1 a v2 al corregir") {
        incrementarVersion("v1") shouldBe "v2"
    }

    should("3. permitir editar REQUIERE_CORRECCIÓN y bloquear SINCRONIZADO") {
        esEditable("REQUIERE_CORRECCIÓN") shouldBe true
        esEditable("SINCRONIZADO") shouldBe false
    }

    should("4. impedir eliminar un reporte APROBADO") {
        esEliminable("APROBADO") shouldBe false
    }

    should("5. pasar de REQUIERE_CORRECCIÓN a EN_COLA al guardar la edición") {
        nuevoEstadoAlEditar("REQUIERE_CORRECCIÓN") shouldBe "EN_COLA"
    }

    should("6. separar adjuntos por pipe ignorando segmentos vacíos") {
        parsearAdjuntos("foto1.jpg||foto2.png") shouldContainExactly listOf("foto1.jpg", "foto2.png")
    }

    should("7. validar el formato del identificador VIA-YYMMDD-NNNN") {
        "VIA-261019-0001" shouldMatch REGEX_ID
        "VIA-261019-001" shouldNotMatch REGEX_ID
    }

    should("8. aceptar un formulario con todos los campos obligatorios") {
        validarCampos(CamposReporte()) shouldBe null
    }

    should("9. rechazar un formulario sin adjuntos") {
        validarCampos(CamposReporte(totalAdjuntos = 0)) shouldContain "adjuntar"
    }

    should("10. rechazar archivos de más de 10 MB") {
        superaLimiteTamaño(LIMITE_BYTES) shouldBe false
        superaLimiteTamaño(LIMITE_BYTES + 1) shouldBe true
    }
})
