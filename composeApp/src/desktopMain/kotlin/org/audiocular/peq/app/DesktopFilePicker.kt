package org.audiocular.peq.app

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

object DesktopFilePicker {
    fun pickFolder(title: String = "Select Folder"): String? {
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
        dialog.isVisible = true
        val result = if (dialog.directory != null && dialog.file != null) {
            File(dialog.directory, dialog.file).absolutePath
        } else if (dialog.directory != null) {
            File(dialog.directory).absolutePath
        } else null
        System.setProperty("apple.awt.fileDialogForDirectories", "false")
        return result
    }

    fun pickFile(title: String = "Select File", extensions: List<String> = emptyList()): String? {
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
        if (extensions.isNotEmpty()) {
            dialog.setFilenameFilter { _, name ->
                extensions.any { name.lowercase().endsWith(it.lowercase()) }
            }
        }
        dialog.isVisible = true
        return if (dialog.directory != null && dialog.file != null) {
            File(dialog.directory, dialog.file).absolutePath
        } else null
    }
}
