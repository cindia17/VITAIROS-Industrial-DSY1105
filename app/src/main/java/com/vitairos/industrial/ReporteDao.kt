package com.vitairos.industrial




import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReporteDao {

    // Guardar un nuevo reporte
    @Insert
    suspend fun guardarReporte(reporte: Reporte)

    // Consultar todos los reportes guardados
    @Query("SELECT * FROM reportes ORDER BY id DESC")
    fun obtenerReportes(): Flow<List<Reporte>>

    // Buscar un reporte por su número
    @Query("SELECT * FROM reportes WHERE id = :id LIMIT 1")
    suspend fun buscarReporte(id: Int): Reporte?

    // Actualizar un reporte existente
    @Update
    suspend fun actualizarReporte(reporte: Reporte)

    // Consultar reportes en cola de sincronización
    @Query("SELECT * FROM reportes WHERE estado IN ('PENDIENTE','EN_COLA','BORRADOR')")
    suspend fun obtenerPendientes(): List<Reporte>

    // Verificar si ya existe un reporte sincronizado con el mismo identificador (deduplicación)
    @Query("SELECT COUNT(*) FROM reportes WHERE identificadorDocumento = :idDoc AND estado IN ('SINCRONIZADO','APROBADO') AND id != :idPropio")
    suspend fun contarDuplicadoSincronizado(idDoc: String, idPropio: Int): Int

    // Eliminar un reporte
    @Delete
    suspend fun eliminarReporte(reporte: Reporte)
}
