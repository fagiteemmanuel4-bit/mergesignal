package com.promptnotes.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(private val dao: NoteDao) : ViewModel() {
    val notes = dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(id: Long?, title: String, content: String, folder: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = id?.let { dao.findById(it) }
            if (existing == null) {
                dao.insert(NoteEntity(title = title.trim(), content = content, folder = folder.trim().ifBlank { "Personal" }))
            } else {
                dao.update(existing.copy(
                    title = title.trim(), content = content,
                    folder = folder.trim().ifBlank { "Personal" }, updatedAt = now
                ))
            }
        }
    }

    fun delete(note: NoteEntity) { viewModelScope.launch { dao.delete(note) } }

    fun togglePinned(note: NoteEntity) {
        viewModelScope.launch { dao.update(note.copy(pinned = !note.pinned, updatedAt = System.currentTimeMillis())) }
    }

    fun duplicate(note: NoteEntity) {
        viewModelScope.launch {
            dao.insert(note.copy(id = 0, title = if (note.title.isBlank()) "Untitled copy" else "${note.title} copy",
                pinned = false, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis(), reminderAt = null))
        }
    }

    fun setReminder(noteId: Long, at: Long) {
        viewModelScope.launch {
            dao.findById(noteId)?.let { dao.update(it.copy(reminderAt = at, updatedAt = System.currentTimeMillis())) }
        }
    }
}

class NotesViewModelFactory(private val dao: NoteDao) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = NotesViewModel(dao) as T
}
