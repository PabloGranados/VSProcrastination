package com.example.vsprocrastination.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.vsprocrastination.data.dao.HabitDao
import com.example.vsprocrastination.data.dao.SubtaskDao
import com.example.vsprocrastination.data.dao.TaskDao
import com.example.vsprocrastination.data.model.Habit
import com.example.vsprocrastination.data.model.HabitLog
import com.example.vsprocrastination.data.model.Subtask
import com.example.vsprocrastination.data.model.Task
import com.example.vsprocrastination.data.model.TaskConverters

/**
 * Base de datos Room para la app.
 * Singleton pattern para evitar múltiples instancias.
 * 
 * @TypeConverters a nivel de Database para que Room pueda
 * convertir el enum Difficulty en todas las queries del DAO.
 */
@Database(
    entities = [Task::class, Subtask::class, Habit::class, HabitLog::class],
    version = 5,
    exportSchema = true
)
@TypeConverters(TaskConverters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun habitDao(): HabitDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * Migración v1→v2: Agrega columna 'priority' con default NORMAL.
         * Preserva todos los datos existentes del usuario.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN priority TEXT NOT NULL DEFAULT 'NORMAL'"
                )
            }
        }
        
        /**
         * Migración v2→v3: Agrega tabla 'subtasks' y columna 'isQuickTask'.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN isQuickTask INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS subtasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taskId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (taskId) REFERENCES tasks(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_subtasks_taskId ON subtasks(taskId)")
            }
        }
        
        /**
         * Migración v3→v4: Agrega campos de sincronización Firebase.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN firebaseId TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN lastModifiedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE subtasks ADD COLUMN firebaseId TEXT")
            }
        }
        
        /**
         * Migración v4→v5: Agrega tablas de Habit Tracker.
         * - habits: definición de hábitos del usuario
         * - habit_logs: registro diario de completación
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Crear tabla de hábitos
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS habits (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        emoji TEXT NOT NULL DEFAULT '✅',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        isArchived INTEGER NOT NULL DEFAULT 0,
                        firebaseId TEXT,
                        lastModifiedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                
                // Crear tabla de logs de hábitos
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS habit_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        habitId INTEGER NOT NULL,
                        dateEpochDay INTEGER NOT NULL,
                        completedAt INTEGER NOT NULL DEFAULT 0,
                        firebaseId TEXT,
                        FOREIGN KEY (habitId) REFERENCES habits(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Índices para queries eficientes
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_logs_habitId ON habit_logs(habitId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_logs_dateEpochDay ON habit_logs(dateEpochDay)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_habit_logs_habitId_dateEpochDay ON habit_logs(habitId, dateEpochDay)")
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vsprocrastination_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
