
package com.vitairos.industrial

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reportes")
data class Reporte(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val identificadorDocumento: String,

    val tipoReporte: String,

    val version: String,

    val proceso: String,

    val fechaHora: String,

    val usuario: String,

    val observaciones: String,

    val archivoLocal: String,

    val estado: String = "PENDIENTE",

    val observacionSupervisor: String = "",

    val area: String = "",

    val equipo: String = "",

    val turno: String = "",

    val severidad: String = "",

    val ubicacion: String = ""
)
