package com.vitairos.industrial

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Suite que agrupa todas las pruebas unitarias del proyecto.
 * Ejecutar esta clase es equivalente a correr todas las suites juntas.
 *
 * Comando: ./gradlew :app:testDebugUnitTest --no-configuration-cache
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Entidad Reporte — valores por defecto y comportamiento
    ReporteEntityTest::class,

    // Lógica de negocio — versión, estados, transiciones
    VersionIncrementTest::class,
    EstadoEditableTest::class,
    EstadoEliminableTest::class,
    TransicionEstadoTest::class,

    // Adjuntos e identificador
    AdjuntosTest::class,
    IdentificadorFormatoTest::class,

    // Validaciones del formulario y límites
    ValidacionCamposTest::class,
    LimitesAdjuntosTest::class,

    // Flujo completo operador → sincronización
    FlujoCreacionReporteTest::class
)
class VitairosTestSuite
