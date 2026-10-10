package fr.godox.figlekt.generator

/**
 * Turns a FIGlet font into Kotlin source, so that the font can be embedded in an artifact and used
 * from common code, synchronously.
 *
 * A font is generated as a lazily instantiated `Font` extension:
 *
 * ```kotlin
 * private var _doom: FigletFont? = null
 *
 * public val Font.doom: FigletFont
 *     get() = _doom ?: FigletFont("doom", "doom.flf") { "flf2a$ 6 5 15 13 3 24463 229…" }.also { _doom = it }
 * ```
 *
 * This is the single source of truth shared by the Gradle plugin (`fr.godox.figlekt.fonts`), the
 * command line generator and the Kotlin/Wasm web page, which all run this very code on their own
 * platform.
 *
 * The source is written by hand rather than with a code generator library because this code has to
 * run on every platform, KotlinPoet included: its `kotlinpoet-wasm-js` artifact, the only one
 * available for Kotlin/Wasm, contains none of its API, so the plugin and the web page would end up
 * with two generators that must be kept in sync.
 */
object FigletFontGenerator {

    private const val BASE_PACKAGE = "fr.godox.figlekt"
    private const val PLUGIN_PACKAGE = "fr.godox.figlekt.fonts"
    private const val CHUNK_SIZE = 4000

    private val RESERVED = setOf(
        "as", "break", "by", "catch", "class", "companion", "const", "constructor", "continue", "crossinline",
        "data", "delegate", "do", "dynamic", "else", "enum", "expect", "external", "false", "field", "file",
        "final", "finally", "for", "fun", "get", "if", "import", "in", "init", "inline", "inner", "internal",
        "is", "it", "lateinit", "noinline", "null", "object", "open", "operator", "out", "override", "package",
        "param", "private", "property", "protected", "public", "receiver", "reified", "return", "sealed",
        "set", "setparam", "super", "suspend", "tailrec", "this", "throw", "true", "try", "typealias",
        "typeof", "val", "value", "var", "vararg", "when", "where", "while",
    )

    /**
     * Returns the font name of the given file, for example `Standard.flf` becomes `Standard`.
     */
    fun fontName(filename: String): String = filename.substringBeforeLast('.')

    /**
     * Returns the name of the `Font` extension generated for the given file,
     * for example `Big_Money-ne.flf` becomes `bigMoneyNe`.
     */
    fun functionName(filename: String): String = camelCase(fontName(filename))

    /**
     * Returns the name of the `FigletsExtension` accessor of the given file,
     * for example `banner3-D.flf` becomes `banner3D`.
     */
    fun accessorName(filename: String): String {
        val name = functionName(filename)
        return if (name in RESERVED) name + "Font" else name
    }

    /**
     * Returns the Kotlin source exposing the given FIGlet fonts as lazily instantiated `Font` extensions.
     * @param fonts       the fonts, as a filename to raw content map.
     * @param packageName the package of the generated code, empty for the default package.
     * @param basePackage the package holding the `Font` object and the `FigletFont` class.
     */
    fun generate(fonts: Map<String, ByteArray>, packageName: String = "", basePackage: String = BASE_PACKAGE): String {
        val builder = StringBuilder()
        if (packageName.isNotEmpty()) {
            builder.append("package ").append(packageName).append("\n\n")
            if (packageName != basePackage) {
                builder.append("import ").append(basePackage).append(".FigletFont\n")
                builder.append("import ").append(basePackage).append(".Font\n\n")
            }
        }
        for ((filename, content) in fonts) {
            builder.append(fontSource(filename, content, basePackage))
            builder.append("\n")
        }
        return builder.toString()
    }

    /**
     * Returns the Kotlin source exposing the given FIGlet font as a lazily instantiated `Font` extension.
     * @param filename    the font file name, for example `Standard.flf`.
     * @param content     the raw content of the font file.
     * @param packageName the package of the generated code, empty for the default package.
     * @param basePackage the package holding the `Font` object and the `FigletFont` class.
     */
    fun generate(
        filename: String,
        content: ByteArray,
        packageName: String = "",
        basePackage: String = BASE_PACKAGE,
    ): String = generate(mapOf(filename to content), packageName, basePackage)

    /**
     * Returns the Kotlin source of a single font, without any package declaration.
     * @param filename    the font file name, for example `Standard.flf`.
     * @param content     the raw content of the font file.
     * @param basePackage the package holding the `Font` object and the `FigletFont` class.
     */
    fun fontSource(filename: String, content: ByteArray, basePackage: String = BASE_PACKAGE): String {
        val name = functionName(filename)
        val field = "_" + name.trimStart('_')
        val builder = StringBuilder()
        builder.append("private var ").append(field).append(": FigletFont? = null\n\n")
        builder.append("/** The FIGlet font `").append(filename).append("`. */\n")
        builder.append("public val Font.").append(name).append(": FigletFont\n")
        builder.append("    get() = ").append(field).append(" ?: FigletFont(\"")
            .append(escape(fontName(filename))).append("\", \"").append(escape(filename)).append("\") {\n")
        literal(builder, content.decodeToString())
        builder.append("    }.also { ").append(field).append(" = it }\n")
        return builder.toString()
    }

    /**
     * Returns the Kotlin source of the `FigletsExtension` of the Gradle plugin,
     * one accessor per font of the [FigletCatalog].
     *
     * ```kotlin
     * figlektFonts {
     *     figlets {
     *         +basic
     *         +doom
     *     }
     * }
     * ```
     */
    fun generateAccessors(): String {
        val builder = StringBuilder()
        builder.append("// Generated from the FIGlet font database, do not edit.\n")
        builder.append("package ").append(PLUGIN_PACKAGE).append("\n\n")
        builder.append("/**\n")
        builder.append(" * The fonts to compile, one accessor per font of the FIGlet font database.\n")
        builder.append(" *\n")
        builder.append(" * @see <a href=\"https://www.figlet.org/cgi-bin/fontdb.cgi\">font database</a>\n")
        builder.append(" */\n")
        builder.append("public class FigletsExtension : FigletsBase() {\n")
        val accessors = LinkedHashMap<String, String>()
        for (figlet in FigletCatalog.values) {
            val name = accessorName(figlet.filename)
            val previous = accessors.put(name, figlet.filename)
            if (previous != null) {
                throw IllegalStateException("Duplicate figlet accessor '$name' for '$previous' and '${figlet.filename}'")
            }
        }
        for ((name, filename) in accessors) {
            builder.append("\n    /** The `").append(filename).append("` FIGlet font. */\n")
            builder.append("    public val ").append(name).append(": FigletSelector = select(\"")
                .append(escape(filename)).append("\")\n")
        }
        builder.append("}\n")
        return builder.toString()
    }

    /**
     * Appends the font content as a Kotlin expression, escaped rather than raw so that the content of
     * the font file is never altered by a formatter, and chunked to stay well below the string
     * constant limits of the compiler.
     */
    private fun literal(builder: StringBuilder, value: String) {
        val chunks = splitChunks(value, CHUNK_SIZE)
        if (chunks.size == 1) {
            builder.append("        \"").append(escape(chunks[0])).append("\"\n")
            return
        }
        builder.append("        buildString(").append(value.length).append(") {\n")
        for (chunk in chunks) {
            builder.append("            append(\"").append(escape(chunk)).append("\")\n")
        }
        builder.append("        }\n")
    }

    private fun splitChunks(value: String, max: Int): List<String> {
        if (value.isEmpty()) {
            return listOf("")
        }
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < value.length) {
            var end = minOf(start + max, value.length)
            if (end < value.length
                && value[end - 1] in '\uD800'..'\uDBFF'
                && value[end] in '\uDC00'..'\uDFFF'
            ) {
                end--
            }
            chunks.add(value.substring(start, end))
            start = end
        }
        return chunks
    }

    /**
     * Escapes the value for a Kotlin string literal, the dollar signs included.
     */
    private fun escape(value: String): String = buildString {
        for (c in value) {
            when {
                c == '\\' -> append("\\\\")
                c == '"' -> append("\\\"")
                c == '$' -> append("\\\$")
                c == '\n' -> append("\\n")
                c == '\r' -> append("\\r")
                c == '\t' -> append("\\t")
                c.code < 0x20 || c.code == 0x7f -> {
                    append("\\u")
                    append(c.code.toString(16).padStart(4, '0'))
                }
                else -> append(c)
            }
        }
    }

    private fun camelCase(value: String): String {
        val builder = StringBuilder()
        for ((index, word) in words(value).withIndex()) {
            if (index == 0) {
                builder.append(word.lowercase())
            } else {
                builder.append(word.replaceFirstChar { it.uppercaseChar() })
            }
        }
        val text = builder.toString()
        if (text.isEmpty()) {
            return "font"
        }
        return if (text[0].isDigit()) "_$text" else text
    }

    private fun words(value: String): List<String> = value.split(Regex("[^A-Za-z0-9]+"))
        .filter { it.isNotEmpty() }
}