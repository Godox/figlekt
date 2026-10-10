package fr.godox.figlekt.fonts

import java.io.File

/**
 * The source of a font to compile.
 */
internal class FigletSpec private constructor(
    /**
     * The identifier of the spec, [KIND_CATALOG], [KIND_FILE] or [KIND_URL] followed by the location.
     */
    val id: String,
    /**
     * The name of the font file, used to name the generated class and extension.
     */
    val filename: String,
) {

    companion object {
        const val KIND_CATALOG = "catalog"
        const val KIND_FILE = "file"
        const val KIND_URL = "url"

        fun catalog(filename: String) = FigletSpec("$KIND_CATALOG:$filename", filename)

        fun file(file: File) = FigletSpec("$KIND_FILE:${file.absolutePath}", file.name)

        fun url(url: String) = FigletSpec("$KIND_URL:$url", url.substringAfterLast('/').substringBefore('?'))
    }
}

/**
 * A font to compile, selected with `+` in the `figlets` block.
 */
class FigletSelector internal constructor(
    internal val spec: FigletSpec,
    private val specs: MutableSet<String>,
) {

    /**
     * The name of the font file.
     */
    val filename: String
        get() = spec.filename

    /**
     * Selects the font.
     */
    operator fun unaryPlus() {
        specs.add(spec.id)
    }

    /**
     * Unselects the font.
     */
    operator fun unaryMinus() {
        specs.remove(spec.id)
    }

    override fun toString(): String = spec.filename
}

/**
 * The fonts to compile, configured inside the `figlektFonts` block.
 *
 * ```kotlin
 * figlektFonts {
 *     figlets {
 *         +basic
 *         +doom
 *         +file("fonts/MyFont.flf")
 *         +url("https://www.figlet.org/fonts/big.flf")
 *     }
 * }
 * ```
 *
 * One accessor is available per font of the FIGlet font database,
 * and [file] and [url] compile fonts from anywhere else.
 */
abstract class FigletsBase internal constructor() {

    internal val specs: MutableSet<String> = LinkedHashSet()

    internal fun select(filename: String): FigletSelector = FigletSelector(FigletSpec.catalog(filename), specs)

    /**
     * Compiles the given font file, whatever its name.
     */
    fun file(file: Any): FigletSelector = FigletSelector(FigletSpec.file(File(file.toString())), specs)

    /**
     * Compiles the font downloaded from the given URL.
     */
    fun url(url: Any): FigletSelector = FigletSelector(FigletSpec.url(url.toString()), specs)

    /**
     * Compiles all the fonts of the configured directories, the default when nothing is selected.
     */
    fun all() {
        specs.clear()
        specs.add("${FigletSpec.KIND_CATALOG}:")
    }

    override fun toString(): String = specs.toString()
}