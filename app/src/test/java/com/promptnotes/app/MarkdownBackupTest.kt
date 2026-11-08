package com.promptnotes.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownBackupTest {
    @Test
    fun parsesLegacyPromptNotesExportAndFolders() {
        val source = "# Shopping list\n_Folder: Personal_\n\nMilk\nBread\n\n---\n\n# Work plan\n_Folder: Work_\n\nShip release"
        val notes = MarkdownBackup.parse(source)

        assertEquals(2, notes.size)
        assertEquals("Shopping list", notes[0].title)
        assertEquals("Personal", notes[0].folder)
        assertEquals("Milk\nBread", notes[0].content)
        assertEquals("Work", notes[1].folder)
        assertEquals("Ship release", notes[1].content)
    }

    @Test
    fun ignoresInvalidMarkdownBlocks() {
        val notes = MarkdownBackup.parse("Not a Prompt Notes backup")
        assertTrue(notes.isEmpty())
    }
}
