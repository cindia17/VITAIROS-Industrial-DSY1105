package com.vitairos.industrial

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
//  ENTIDAD Reporte — valores por defecto y comportamiento
// ─────────────────────────────────────────────────────────────

@RunWith(KotestTestRunner::class)
class ReporteEntityTest : ShouldSpec({

    context("valores por defecto") {

        should("tener estado PENDIENTE al crear sin especificarlo") {
            reporteBase().estado shouldBe "PENDIENTE"
        }

        should("tener observacionSupervisor vacía por defecto") {
            reporteBase().observacionSupervisor shouldBe ""
        }

        should("tener versión v1 al crear") {
            reporteBase().version shouldBe "v1"
        }

        should("tener id 0 antes de persistir en Room") {
            reporteBase().id shouldBe 0
        }
    }

    context("comportamiento de data class") {

        should("preservar campos no modificados al hacer copy") {
            val original = reporteBase()
            val copia = original.copy(estado = "EN_COLA")
            copia.identificadorDocumento shouldBe original.identificadorDocumento
            copia.tipoReporte shouldBe original.tipoReporte
            copia.estado shouldBe "EN_COLA"
        }

        should("ser igual a otro reporte con los mismos datos") {
            reporteBase() shouldBe reporteBase()
        }

        should("ser distinto si cambia el identificador") {
            reporteBase() shouldNotBe reporteBase().copy(identificadorDocumento = "VIA-261019-0002")
        }

        should("tener identificadorDocumento no vacío") {
            reporteBase().identificadorDocumento.isNotBlank() shouldBe true
        }
    }
})

// ─────────────────────────────────────────────────────────────
//  VERSIÓN — incremento al corregir un reporte rechazado
// ─────────────────────────────────────────────────────────────

@RunWith(KotestTestRunner::class)
class VersionIncrementTest : ShouldSpec({

    should("incrementar v1 a v2") {
        incrementarVersion("v1") shouldBe "v2"
    }

    should("incrementar v2 a v3") {
        incrementarVersion("v2") shouldBe "v3"
    }

    should("incrementar v9 a v10") {
        incrementarVersion("v9") shouldBe "v10"
    }

    should("producir v2 con formato inválido (toIntOrNull retorna null → usa 1)") {
        incrementarVersion("vX") shouldBe "v2"
    }

    should("producir v2 con cadena vacía") {
        incrementarVersion("") shouldBe "v2"
    }
})

// ─────────────────────────────────────────────────────────────
//  ESTADO — qué reportes se pueden editar
// ─────────────────────────────────────────────────────────────

@RunWith(KotestTestRunner::class)
class EstadoEditableTest : ShouldSpec({

    context("estados que permiten edición") {
        listOf("BORRADOR", "EN_COLA", "PENDIENTE", "RECHAZADO", "REQUIERE_CORRECCIÓN").forEach { estado ->
            should("permitir editar $estado") {
                esEditable(estado) shouldBe true
            }
        }
    }

    context("estados que bloquean edición") {
        listOf("SINCRONIZADO", "APROBADO", "ENVIANDO").forEach { estado ->
            should("bloquear edición de $estado") {
                esEditable(estado) shouldBe false
            }
        }
    }
})

// ─────────────────────────────────────────────────────────────
//  ESTADO — qué reportes se pueden eliminar
// ─────────────────────────────────────────────────────────────

@RunWith(KotestTestRunner::class)
class EstadoEliminableTest : ShouldSpec({

    context("estados que permiten eliminar") {
        listOf("BORRADOR", "EN_COLA", "PENDIENTE", "REQUIERE_CORRECCIÓN", "ERROR").forEach { estado ->
            should("permitir eliminar $estado") {
                esEliminable(estado) shouldBe true
            }
        }
    }

    context("estados que no se pueden eliminar") {
        listOf("SINCRONIZADO", "APROBADO").forEach { estado ->
            should("bloquear eliminación de $estado") {
                esEliminable(estado) shouldBe false
            }
        }
    }
})

// ─────────────────────────────────────────────────────────────
//  TRANSICIÓN — estado resultante al guardar edición
// ─────────────────────────────────────────────────────────────

@RunWith(KotestTestRunner::class)
class TransicionEstadoTest : ShouldSpec({

    context("estados que pasan a EN_COLA al editar") {
        listOf("REQUIERE_CORRECCIÓN", "RECHAZADO", "BORRADOR").forEach { estado ->
            should("cambiar $estado a EN_COLA") {
                nuevoEstadoAlEditar(estado) shouldBe "EN_COLA"
            }
        }
    }

    context("estados que se mantienen igual al editar") {
        listOf("EN_COLA", "PENDIENTE").forEach { estado ->
            should("mantener $estado sin cambios") {
                nuevoEstadoAlEditar(estado) shouldBe estado
            }
        }
    }
})

// ─────────────────────────────────────────────────────────────
//  ADJUNTOS — parseo del campo archivoLocal (separado por "|")
// ─────────────────────────────────────────────────────────────

@RunWith(KotestTestRunner::class)
class AdjuntosTest : ShouldSpec({

    should("devolver lista vacía si archivoLocal está en blanco") {
        parsearAdjuntos("").shouldBeEmpty()
    }

    should("devolver un elemento con un solo adjunto") {
        parsearAdjuntos("/data/user/0/com.vitairos/files/foto.jpg") shouldHaveSize 1
    }

    should("devolver dos elementos con dos adjuntos separados por pipe") {
        val result = parsearAdjuntos("foto1.jpg|foto2.png")
        result shouldHaveSize 2
        result[0] shouldBe "foto1.jpg"
        result[1] shouldBe "foto2.png"
    }

    should("aceptar cinco adjuntos (máximo según VITAIROS)") {
        val adjuntos = (1..5).joinToString("|") { "archivo$it.jpg" }
        parsearAdjuntos(adjuntos) shouldHaveSize 5
    }

    should("ignorar segmentos vacíos entre pipes") {
        parsearAdjuntos("foto1.jpg||foto2.jpg") shouldHaveSize 2
    }
})

// ─────────────────────────────────────────────────────────────
//  IDENTIFICADOR — formato VIA-YYMMDD-NNNN
// ─────────────────────────────────────────────────────────────

@RunWith(KotestTestRunner::class)
class IdentificadorFormatoTest : ShouldSpec({

    context("identificadores válidos") {

        should("aceptar VIA-261019-0001") {
            "VIA-261019-0001" shouldMatch REGEX_ID
        }

        should("aceptar número de secuencia hasta 9999") {
            "VIA-261019-9999" shouldMatch REGEX_ID
        }

        should("aceptar fecha de distintos meses") {
            "VIA-261105-0001" shouldMatch REGEX_ID
        }
    }

    context("identificadores inválidos") {

        should("rechazar identificador sin prefijo VIA") {
            "261019-0001" shouldNotMatch REGEX_ID
        }

        should("rechazar letras en la parte de fecha") {
            "VIA-AB1019-0001" shouldNotMatch REGEX_ID
        }

        should("rechazar secuencia de solo 3 dígitos") {
            "VIA-261019-001" shouldNotMatch REGEX_ID
        }

        should("rechazar cadena vacía") {
            "" shouldNotMatch REGEX_ID
        }

        should("rechazar identificador con espacios") {
            "VIA-261019 0001" shouldNotMatch REGEX_ID
        }
    }
})
