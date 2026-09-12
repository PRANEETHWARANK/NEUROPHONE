package com.neurophone.app.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import com.neurophone.app.domain.model.ContextState
import com.neurophone.app.domain.model.LearnedGesture
import com.neurophone.app.domain.model.RoutinePattern
import com.neurophone.app.domain.model.UserFeedback

class NeuroDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "neurophone.db"
        const val DATABASE_VERSION = 2

        const val TABLE_GESTURES = "learned_gestures"
        const val TABLE_FEEDBACK = "user_feedback"
        const val TABLE_ROUTINES = "routine_patterns"
        const val TABLE_INTERACTIONS = "interaction_events"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_GESTURES (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                bound_action TEXT NOT NULL,
                sample_count INTEGER NOT NULL,
                confidence REAL NOT NULL,
                usage_count INTEGER NOT NULL,
                created_at INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_FEEDBACK (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                predicted_action TEXT NOT NULL,
                actual_action TEXT NOT NULL,
                was_correct INTEGER NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_ROUTINES (
                id TEXT PRIMARY KEY,
                sequence_desc TEXT NOT NULL,
                suggested_shortcut TEXT NOT NULL,
                occurrence_count INTEGER NOT NULL,
                confidence REAL NOT NULL,
                is_accepted INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_INTERACTIONS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                action_type TEXT NOT NULL,
                target_app TEXT NOT NULL,
                touch_x REAL NOT NULL,
                touch_y REAL NOT NULL,
                tap_duration INTEGER NOT NULL,
                swipe_velocity REAL NOT NULL,
                context_state TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent())

        insertInitialSeedData(db)
    }

    private fun insertInitialSeedData(db: SQLiteDatabase) {
        db.execSQL("""
            INSERT INTO $TABLE_GESTURES (id, name, bound_action, sample_count, confidence, usage_count, created_at)
            VALUES 
            ('g_01', 'Air Wave Up', 'Open Notes', 25, 0.94, 18, ${System.currentTimeMillis() - 86400000}),
            ('g_02', 'Two-Finger Pinch', 'Study Mode', 20, 0.91, 14, ${System.currentTimeMillis() - 72000000}),
            ('g_03', 'Circular Clockwise', 'Open Music', 30, 0.96, 27, ${System.currentTimeMillis() - 36000000})
        """.trimIndent())

        db.execSQL("""
            INSERT INTO $TABLE_ROUTINES (id, sequence_desc, suggested_shortcut, occurrence_count, confidence, is_accepted)
            VALUES
            ('r_01', 'Browser → College Portal → Notes', 'Quick Study Setup', 14, 0.92, 1),
            ('r_02', 'Camera → Gallery → Edit', 'Photo Workflow', 9, 0.85, 0)
        """.trimIndent())

        db.execSQL("""
            INSERT INTO $TABLE_FEEDBACK (predicted_action, actual_action, was_correct, timestamp)
            VALUES
            ('Open Music', 'Open Notes', 0, ${System.currentTimeMillis() - 14400000}),
            ('Open Notes', 'Open Notes', 1, ${System.currentTimeMillis() - 10800000}),
            ('Study Mode', 'Study Mode', 1, ${System.currentTimeMillis() - 7200000})
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GESTURES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FEEDBACK")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ROUTINES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_INTERACTIONS")
        onCreate(db)
    }

    fun getAllGestures(): List<LearnedGesture> {
        val list = mutableListOf<LearnedGesture>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_GESTURES ORDER BY usage_count DESC", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    LearnedGesture(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        name = it.getString(it.getColumnIndexOrThrow("name")),
                        boundAction = it.getString(it.getColumnIndexOrThrow("bound_action")),
                        sampleCount = it.getInt(it.getColumnIndexOrThrow("sample_count")),
                        confidence = it.getFloat(it.getColumnIndexOrThrow("confidence")),
                        usageCount = it.getInt(it.getColumnIndexOrThrow("usage_count")),
                        createdAt = it.getLong(it.getColumnIndexOrThrow("created_at"))
                    )
                )
            }
        }
        return list
    }

    fun saveGesture(gesture: LearnedGesture) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", gesture.id)
            put("name", gesture.name)
            put("bound_action", gesture.boundAction)
            put("sample_count", gesture.sampleCount)
            put("confidence", gesture.confidence)
            put("usage_count", gesture.usageCount)
            put("created_at", gesture.createdAt)
        }
        db.insertWithOnConflict(TABLE_GESTURES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun incrementGestureUsage(gestureId: String) {
        writableDatabase.execSQL("UPDATE $TABLE_GESTURES SET usage_count = usage_count + 1 WHERE id = ?", arrayOf(gestureId))
    }

    fun deleteGesture(gestureId: String) {
        writableDatabase.delete(TABLE_GESTURES, "id = ?", arrayOf(gestureId))
    }

    fun logFeedback(feedback: UserFeedback) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("predicted_action", feedback.predictedAction)
            put("actual_action", feedback.actualAction)
            put("was_correct", if (feedback.wasCorrect) 1 else 0)
            put("timestamp", feedback.timestamp)
        }
        db.insert(TABLE_FEEDBACK, null, values)
    }

    fun getAllFeedback(): List<UserFeedback> {
        val list = mutableListOf<UserFeedback>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM $TABLE_FEEDBACK ORDER BY timestamp DESC", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    UserFeedback(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        predictedAction = it.getString(it.getColumnIndexOrThrow("predicted_action")),
                        actualAction = it.getString(it.getColumnIndexOrThrow("actual_action")),
                        wasCorrect = it.getInt(it.getColumnIndexOrThrow("was_correct")) == 1,
                        timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp"))
                    )
                )
            }
        }
        return list
    }

    fun getAllRoutines(): List<RoutinePattern> {
        val list = mutableListOf<RoutinePattern>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM $TABLE_ROUTINES ORDER BY occurrence_count DESC", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    RoutinePattern(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        sequenceDescription = it.getString(it.getColumnIndexOrThrow("sequence_desc")),
                        suggestedShortcut = it.getString(it.getColumnIndexOrThrow("suggested_shortcut")),
                        occurrenceCount = it.getInt(it.getColumnIndexOrThrow("occurrence_count")),
                        confidence = it.getFloat(it.getColumnIndexOrThrow("confidence")),
                        isAccepted = it.getInt(it.getColumnIndexOrThrow("is_accepted")) == 1
                    )
                )
            }
        }
        return list
    }

    fun acceptRoutine(id: String) {
        writableDatabase.execSQL("UPDATE $TABLE_ROUTINES SET is_accepted = 1 WHERE id = ?", arrayOf(id))
    }

    fun recordInteraction(app: String, context: String, durationMs: Long = 180L) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("action_type", "APP_OPEN")
            put("target_app", app)
            put("touch_x", 540.0f)
            put("touch_y", 1200.0f)
            put("tap_duration", durationMs)
            put("swipe_velocity", 0.0f)
            put("context_state", context)
            put("timestamp", System.currentTimeMillis())
        }
        db.insert(TABLE_INTERACTIONS, null, values)
    }

    fun getRecentApps(limit: Int = 5): List<String> {
        val list = mutableListOf<String>()
        val cursor = readableDatabase.rawQuery(
            "SELECT target_app FROM $TABLE_INTERACTIONS ORDER BY timestamp DESC LIMIT ?",
            arrayOf(limit.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(it.getString(0))
            }
        }
        return list
    }

    fun clearAllData() {
        val db = writableDatabase
        db.delete(TABLE_GESTURES, null, null)
        db.delete(TABLE_FEEDBACK, null, null)
        db.delete(TABLE_ROUTINES, null, null)
        db.delete(TABLE_INTERACTIONS, null, null)
    }
}