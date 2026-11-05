package com.vitairos.industrial

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.runner.junit4.KotestTestRunner
import org.junit.runner.RunWith

// ─────────────────────────────────────────────────────────────
//  Funciones puras que espejean las validaciones del formulario
//  (PantallaNewReporte — botón Guardar ~línea 882)
// ─────────────────────────────────────────────────────────────

private data class CamposReporte(
    val identificador: String = "",
    val tipoReporte: String   = "",
    val area: String          = "",
    val proceso: String       = "",
    val equipo: String        = "",
    val severidad: String     = "",
    val totalAdjuntos: Int    = 0,
    val necesitaEquipo: Boolean  = false,
    val esIncidente: Boolean     = false
)

private fun validarCampos(c: CamposReporte): String? = when {
    c.identificador.isBlank() ||
    c.tipoReporte.isBlank()   ||
    c.area.isBlank()          ||
    c.proceso.isBlank()         -> "Complete identificador, tipo, área y proceso"

    c.necesitaEquipo && c.equipo.isBlank() -> "Ingrese el equipo o TAG"

    c.esIncidente && c.severidad.isBlank() -> "Seleccione la severidad del incidente"

    c.totalAdjuntos == 0        -> "Debe adjuntar al menos una fotografía, imagen o PDF"

    else                        -> null  // null = válido
}

private const val MAX_ADJUNTOS   = 5
private const val LIMITE_BYTES   = 10L * 1024 * 1024   // 10 MB

private fun contarAdjuntos(archivoLocal: String): Int =
    if (archivoLocal.isBlank()) 0
    else archivoLocal.split("|").count { it.isNotBlank() }

private fun superaLimiteTamaño(bytes: Long): Boolean = bytes > LIMITE_BYTES

// ─────────────────────────────────────────────────────────────
//  VALIDACIÓN — campos obligatorios del formulario
// ─────────────────────────────────────────────────────────────

@RunWith(KotestTestRunner::class)
class ValidacionCamposTest : ShouldSpec({

    context("campos mínimos obligatorios") {

        should("aceptar reporte con todos los campos requeridos") {
            val campos = CamposReporte(
                identificador = "VIA-261105-0001",
                tipoReporte   = "Inspección Rutinaria",
                area          = "Área Sur",
                proceso       = "Planta 2",
                totalAdjuntos = 1
            )
            validarCampos(campos) shouldBe null
        }

        should("rechazar si identificador está vacío") {
            val campos = CamposReporte(
                identificador = "",
                tipoReporte   = "Inspección",
                area          = "Sur",
                proceso       = "P1",
                totalAdjuntos = 1
            )
            validarCampos(campos) shouldContain "identificador"
        }

        should("rechazar si tipoReporte está vacío") {
            val campos = CamposReporte(
                identificador = "VIA-261105-0001",
                tipoReporte   = "",
                area          = "Sur",
                proceso       = "P1",
                totalAdjuntos = 1
            )
            validarCampos(campos) shouldContain "tipo"
        }

        should("rechazar si área está vacía") {
            val campos = CamposReporte(
                identificador = "VIA-261105-0001",
                tipoReporte   = "Inspección",
                area          = "",
                proceso       = "P1",
                totalAdjuntos = 1
            )
            validarCampos(campos) shouldContain "área"
        }

        should("rechazar si proceso está vacío") {
            val campos = CamposReporte(
                identificador = "VIA-261105-0001",
                tipoReporte   = "Inspección",
                area          = "Sur",
                proceso       = "",
                totalAdjuntos = 1
            )
            validarCampos(campos) shouldContain "proceso"
        }

        should("rechazar si campos son solo espacios en blanco") {
            val campos = CamposReporte(
                identificador = "   ",
                tipoReporte   = "   ",
                area          = "   ",
                proceso       = "   ",
                totalAdjuntos = 1
            )
            validarCampos(campos) shouldContain "identificador"
        }
    }

    context("campos condicionales") {

        should("rechazar si es tipo que necesita equipo y equipo está vacío") {
            val campos = CamposReporte(
                identificador    = "VIA-261105-0001",
                tipoReporte      = "Mantención Preventiva",
                area             = "Sur",
                proceso          = "P1",
                equipo           = "",
                necesitaEquipo   = true,
                totalAdjuntos    = 1
            )
            validarCampos(campos) shouldContain "equipo"
        }

        should("aceptar si necesita equipo y el campo está lleno") {
            val campos = CamposReporte(
                identificador    = "VIA-261105-0001",
                tipoReporte      = "Mantención Preventiva",
                area             = "Sur",
                proceso          = "P1",
                equipo           = "BOMBA-01",
                necesitaEquipo   = true,
                totalAdjuntos    = 1
            )
            validarCampos(campos) shouldBe null
        }

        should("rechazar si es incidente sin severidad") {
            val campos = CamposReporte(
                identificador = "VIA-261105-0001",
                tipoReporte   = "Incidente",
                area          = "Sur",
                proceso       = "P1",
                severidad     = "",
                esIncidente   = true,
                totalAdjuntos = 1
            )
            validarCampos(campos) shouldContain "severidad"
        }
    }

    context("adjuntos obligatorios") {

        should("rechazar si no hay ningún adjunto") {
            val campos = CamposReporte(
                identificador = "VIA-261105-0001",
                tipoReporte   = "Inspección",
                area          = "Sur",
                proceso       = "P1",
                totalAdjuntos = 0
            )
            validarCampos(campos) shouldContain "adjuntar"
        }

        should("aceptar si hay exactamente un adjunto") {
            val campos = CamposReporte(
                identificador = "VIA-261105-0001",
                tipoReporte   = "Inspección",
                area          = "Sur",
                proceso       = "P1",
                totalAdjuntos = 1
            )
            validarCampos(campos) shouldBe null
        }
    }
})

// ─────────────────────────────────────────────────────────────
//  VALIDACIÓN — límites de adjuntos
// ─────────────────────────────────────────────────────────────

@RunWith(KotestTestRunner::class)
class LimitesAdjuntosTest : ShouldSpec({

    context("conteo de adjuntos desde archivoLocal") {

        should("contar 0 con cadena vacía") {
            contarAdjuntos("") shouldBe 0
        }

        should("contar 1 con un archivo") {
            contarAdjuntos("/data/files/foto.jpg") shouldBe 1
        }

        should("contar 3 con tres archivos separados por pipe") {
            contarAdjuntos("a.jpg|b.pdf|c.png") shouldBe 3
        }

        should("contar exactamente $MAX_ADJUNTOS en el límite máximo") {
            val adjuntos = (1..MAX_ADJUNTOS).joinToString("|") { "archivo$it.jpg" }
            contarAdjuntos(adjuntos) shouldBe MAX_ADJUNTOS
        }
    }

    context("validación de tamaño de archivo — límite 10 MB") {

        should("aceptar archivo de exactamente 10 MB") {
            superaLimiteTamaño(LIMITE_BYTES) shouldBe false
        }

        should("rechazar archivo de 10 MB + 1 byte") {
            superaLimiteTamaño(LIMITE_BYTES + 1) shouldBe true
        }

        should("aceptar archivo de 1 MB") {
            superaLimiteTamaño(1L * 1024 * 1024) shouldBe false
        }

        should("rechazar archivo de 15 MB") {
            superaLimiteTamaño(15L * 1024 * 1024) shouldBe true
        }

        should("aceptar archivo de 0 bytes") {
            superaLimiteTamaño(0L) shouldBe false
        }
    }
})

// ─────────────────────────────────────────────────────────────
//  FLUJO COMPLETO — crear reporte válido hasta EN_COLA
// ─────────────────────────────────────────────────────────────

@RunWith(KotestTestRunner::class)
class FlujoCreacionReporteTest : ShouldSpec({

    should("reporte creado por operador tiene estado EN_COLA y versión v1") {
        val r = Reporte(
            identificadorDocumento = "VIA-261105-0001",
            tipoReporte            = "Inspección Rutinaria",
            version                = "v1",
            proceso                = "Planta Sur",
            fechaHora              = "2026-11-05 10:00",
            usuario                = "OPERADOR",
            observaciones          = "Revisión periódica",
            archivoLocal           = "/data/files/foto1.jpg",
            estado                 = "EN_COLA"
        )
        r.estado   shouldBe "EN_COLA"
        r.version  shouldBe "v1"
        r.usuario  shouldBe "OPERADOR"
    }

    should("reporte rechazado recupera estado EN_COLA al editar y sube versión a v2") {
        val original = Reporte(
            identificadorDocumento = "VIA-261105-0001",
            tipoReporte            = "Inspección",
            version                = "v1",
            proceso                = "P1",
            fechaHora              = "2026-11-05 10:00",
            usuario                = "OPERADOR",
            observaciones          = "obs",
            archivoLocal           = "foto.jpg",
            estado                 = "REQUIERE_CORRECCIÓN",
            observacionSupervisor  = "Falta firma"
        )

        // simula la lógica de PantallaEditarReporte al guardar
        val nuevoEstado  = if (original.estado in listOf("RECHAZADO","REQUIERE_CORRECCIÓN")) "EN_COLA" else original.estado
        val num          = original.version.removePrefix("v").toIntOrNull() ?: 1
        val nuevaVersion = "v${num + 1}"

        val corregido = original.copy(
            estado                = nuevoEstado,
            version               = nuevaVersion,
            observacionSupervisor = ""
        )

        corregido.estado                shouldBe "EN_COLA"
        corregido.version               shouldBe "v2"
        corregido.observacionSupervisor shouldBe ""
    }

    should("reporte SINCRONIZADO mantiene su estado al intentar editar (no es editable)") {
        val sync = Reporte(
            identificadorDocumento = "VIA-261104-0005",
            tipoReporte            = "Mantención",
            version                = "v1",
            proceso                = "P2",
            fechaHora              = "2026-11-04 08:00",
            usuario                = "OPERADOR",
            observaciones          = "ok",
            archivoLocal           = "doc.pdf",
            estado                 = "SINCRONIZADO"
        )
        val esEditable = sync.estado in listOf("BORRADOR","EN_COLA","PENDIENTE","RECHAZADO","REQUIERE_CORRECCIÓN")
        esEditable shouldBe false
    }

    should("identificar correctamente que un duplicado ya sincronizado no debe reenviarse") {
        // Simula el check de deduplicación: si ya existe el mismo ID en SINCRONIZADO
        val idDocumento = "VIA-261105-0001"
        val reportesExistentes = listOf(
            Reporte(id=1, identificadorDocumento="VIA-261105-0001", tipoReporte="X",
                version="v1", proceso="P", fechaHora="", usuario="", observaciones="",
                archivoLocal="", estado="SINCRONIZADO")
        )
        val duplicado = reportesExistentes.any {
            it.identificadorDocumento == idDocumento &&
            it.estado in listOf("SINCRONIZADO","APROBADO") &&
            it.id != 99  // id del reporte que se intenta enviar
        }
        duplicado shouldBe true
    }
})
