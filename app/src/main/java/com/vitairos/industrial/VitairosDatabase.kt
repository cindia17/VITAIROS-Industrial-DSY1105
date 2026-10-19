
package com.vitairos.industrial

import android.content.Context

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Reporte::class],
    version = 5,
    exportSchema = false
)
abstract class VitairosDatabase : RoomDatabase() {

    abstract fun reporteDao(): ReporteDao

    companion object {

        @Volatile
        private var INSTANCE: VitairosDatabase? = null

        // Actualiza la base de datos
        // desde la versión 1 a la versión 2.
        private val MIGRACION_1_2 = object : Migration(1, 2) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {

                db.execSQL(
                    """
                    ALTER TABLE reportes
                    ADD COLUMN observacionSupervisor
                    TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )
            }
        }

        private val MIGRACION_2_3 = object : Migration(2, 3) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {

                db.execSQL(
                    "ALTER TABLE reportes ADD COLUMN area TEXT NOT NULL DEFAULT ''"
                )

                db.execSQL(
                    "ALTER TABLE reportes ADD COLUMN equipo TEXT NOT NULL DEFAULT ''"
                )

                db.execSQL(
                    "ALTER TABLE reportes ADD COLUMN turno TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRACION_3_4 = object : Migration(3, 4) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {

                db.execSQL(
                    "ALTER TABLE reportes ADD COLUMN severidad TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRACION_4_5 = object : Migration(4, 5) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {

                db.execSQL(
                    "ALTER TABLE reportes ADD COLUMN ubicacion TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        fun obtenerBaseDatos(
            context: Context
        ): VitairosDatabase {

            return INSTANCE ?: synchronized(this) {

                val instancia = Room.databaseBuilder(
                    context.applicationContext,
                    VitairosDatabase::class.java,
                    "vitairos_database"
                )
                    .addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4, MIGRACION_4_5)
                    .build()

                INSTANCE = instancia

                instancia
            }
        }
    }
}
