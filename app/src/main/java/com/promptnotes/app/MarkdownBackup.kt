package com.promptnotes.app

object MarkdownBackup {
    fun parse(markdown: String): List<NoteEntity> {
        val raw = markdown.replace("\r\n", "\n").replace("\r", "\n").trim()
        if (raw.isBlank()) return emptyList()
        val blocks = raw.split(Regex("\n{2}---\n{2}(?=#\\s)"))
        return blocks.mapNotNull { block ->
            val heading = Regex("(?m)^#\\s+(.+?)(?:\n|$)").find(block) ?: return@mapNotNull null
            val lines = block.substring(heading.range.last + 1).trimStart('\n').split("\n").toMutableList()
            val folderMatch = lines.firstOrNull()?.trim()?.let { Regex("^_Folder:\\s*(.*?)_$").find(it) }
            val folder = folderMatch?.groupValues?.get(1)?.trim().orEmpty().ifBlank { "Personal" }
            if (folderMatch != null) lines.removeAt(0)
            while (lines.isNotEmpty() && lines.first().isBlank()) lines.removeAt(0)
            NoteEntity(
                title = heading.groupValues[1].trim().ifBlank { "Untitled note" },
                content = lines.joinToString("\n").trim(),
                folder = folder
            )
        }
    }

    fun format(notes: List<NoteEntity>): String = notes.joinToString("\n\n---\n\n") { note ->
        "# " + note.title.ifBlank { "Untitled note" } + "\n_Folder: " + note.folder + "_\n\n" + note.content
    }
}
