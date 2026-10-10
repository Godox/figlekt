@file:OptIn(ExperimentalWasmJsInterop::class)

import fr.godox.figlekt.core.Figlekt
import fr.godox.figlekt.core.FigletFont
import fr.godox.figlekt.generator.Figlet
import fr.godox.figlekt.generator.FigletCatalog
import fr.godox.figlekt.generator.FigletFontGenerator
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLLIElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.files.File
import org.w3c.files.FileReader
import org.w3c.xhr.XMLHttpRequest

private const val BASE_PACKAGE = "fr.godox.figlekt"
private const val DEFAULT_TEXT = "Figlekt"
private const val NO_PREVIEW = "Pick a font to see it."

/**
 * Browses the FIGlet font database, previews the selected font, generates its Kotlin class.
 */
fun main() {
    val fontsElement = document.getElementById("fonts") as HTMLElement
    val selectElement = document.getElementById("select") as HTMLSelectElement
    val searchInput = document.getElementById("search") as HTMLInputElement
    val previewElement = document.getElementById("preview") as HTMLElement
    val previewInput = document.getElementById("preview-text") as HTMLInputElement
    val dropZone = document.getElementById("drop") as HTMLElement
    val fileInput = document.getElementById("file") as HTMLInputElement
    val packageInput = document.getElementById("package") as HTMLInputElement
    val output = document.getElementById("output") as HTMLTextAreaElement
    val status = document.getElementById("status") as HTMLElement
    val count = document.getElementById("count") as HTMLElement
    val copyButton = document.getElementById("copy") as HTMLElement
    var font: FigletFont? = null

    fun packageName(): String? = packageInput.value.trim().takeIf { it.isNotEmpty() }

    fun preview() {
        val current = font
        if (current == null) {
            previewElement.textContent = NO_PREVIEW
            return
        }
        val text = previewInput.value.ifEmpty { DEFAULT_TEXT }
        try {
            previewElement.textContent = Figlekt.figletify(text, current)
        } catch (e: Throwable) {
            previewElement.textContent = "Preview failed: ${e.message}"
        }
    }

    fun generate(name: String, text: String) {
        try {
            font = FigletFont(name, name) { text }
            preview()
            val source = FigletFontGenerator.generate(
                name,
                text.encodeToByteArray(),
                packageName() ?: "",
                packageName() ?: BASE_PACKAGE,
            )
            output.value = source
            status.textContent = "Font.${FigletFontGenerator.functionName(name)} generated, ${source.length} characters."
        } catch (e: Throwable) {
            output.value = ""
            status.textContent = "Failed to generate the class: ${e.message}"
        }
    }

    fun select(filename: String, text: String) {
        generate(filename, text)
    }

    fun load(file: File) {
        val reader = FileReader()
        reader.onload = {
            val text = (reader.result as? JsString)?.toString() ?: ""
            if (text.isEmpty()) {
                status.textContent = "The file could not be read."
            } else {
                select(file.name, text)
            }
        }
        reader.readAsText(file)
    }

    fun download(figlet: Figlet) {
        status.textContent = "Downloading ${figlet.filename}..."
        val request = XMLHttpRequest()
        request.open("GET", "/figlet/${figlet.filename}")
        request.onload = {
            val text = request.responseText
            if (request.status.toInt() == 200 && text.isNotEmpty() && !text.trimStart().startsWith("<")) {
                select(figlet.filename, text)
            } else {
                status.textContent = "Unable to download ${figlet.filename}, drop the font file instead."
            }
        }
        request.onerror = {
            status.textContent = "Unable to download ${figlet.filename}, drop the font file instead."
        }
        request.send()
    }

    fun details(figlet: Figlet): String = buildString {
        append(figlet.filename)
        if (figlet.author.isNotEmpty()) {
            append(" - ")
            append(figlet.author)
        }
        if (figlet.date.isNotEmpty()) {
            append(" - ")
            append(figlet.date)
        }
    }

    fun render(filter: String) {
        val text = filter.trim().lowercase()
        val figlets = FigletCatalog.values.filter {
            text.isEmpty() || it.name.lowercase().contains(text) || it.filename.lowercase().contains(text) ||
                    it.author.lowercase().contains(text)
        }
        fontsElement.textContent = ""
        selectElement.textContent = ""
        for (figlet in figlets) {
            val item = document.createElement("li") as HTMLLIElement
            item.textContent = figlet.name
            item.title = details(figlet)
            item.addEventListener("click", { download(figlet) })
            fontsElement.appendChild(item)

            val option = document.createElement("option") as HTMLOptionElement
            option.value = figlet.filename
            option.textContent = figlet.name
            selectElement.appendChild(option)
        }
        count.textContent = "${figlets.size} / ${FigletCatalog.values.size} fonts"
    }

    searchInput.addEventListener("input", { render(searchInput.value) })
    selectElement.addEventListener("change", {
        val filename = selectElement.value
        FigletCatalog.values.firstOrNull { it.filename == filename }?.let { download(it) }
    })
    dropZone.addEventListener("dragover", {
        it.preventDefault()
        dropZone.classList.add("over")
    })
    dropZone.addEventListener("dragleave", { dropZone.classList.remove("over") })
    dropZone.addEventListener("drop", { event ->
        event.preventDefault()
        dropZone.classList.remove("over")
        val files = (event as? org.w3c.dom.DragEvent)?.dataTransfer?.files
        val file = files?.item(0)
        if (file != null) {
            load(file)
        }
    })
    dropZone.addEventListener("click", { fileInput.click() })
    fileInput.addEventListener("change", {
        val file = fileInput.files?.item(0)
        if (file != null) {
            load(file)
        }
    })
    packageInput.addEventListener("change", {
        font?.let { current ->
            generate(current.filename, current.data())
        }
    })
    previewInput.addEventListener("input", { preview() })
    copyButton.addEventListener("click", {
        val text = output.value
        if (text.isNullOrEmpty()) {
            status.textContent = "Nothing to copy yet."
            return@addEventListener
        }
        output.focus()
        output.select()
        status.textContent = if (document.execCommand("copy")) {
            "Copied to the clipboard."
        } else {
            "Press Ctrl+C to copy the selection."
        }
    })

    render("")
}