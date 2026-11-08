package com.promptnotes.app

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [NoteEntity::class], version = 1, exportSchema = true)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
