package fr.godox.figlekt.core

import kotlin.concurrent.Volatile
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Figlekt generates FIGlet banners.
 * FIGlet is a computer program that generates text banners,
 * in a variety of typefaces, composed of letters made up of
 * conglomerations of smaller ASCII characters.
 *
 * Fonts are compiled into the artifacts as plain Kotlin source,
 * so everything stays synchronous and usable from common code:
 *
 * ```kotlin
 * Figlekt.figletify("Hello, World!", Font.standard)
 * ```
 *
 * The rendering itself lives in [FigletSmush], [FigletLayout] and [FigletFontFile].
 */
object Figlekt {

    @Volatile
    private var meta: Map<FigletFont, Meta> = emptyMap()

    /**
     * Returns the FIGlet of the text.
     * @param text             the original text.
     * @param horizontalLayout the horizontal layout.
     * @param verticalLayout   the vertical layout.
     * @return the FIGlet of the text.
     */
    @JvmStatic
    fun figletify(text: String, horizontalLayout: Layout?, verticalLayout: Layout? = null): String {
        return figletify(text, Font.standard, horizontalLayout, verticalLayout)
    }

    /**
     * Returns the FIGlet of the text with the specified font.
     * @param text             the original text.
     * @param font             the specified font, for example `Font.standard`.
     * @param horizontalLayout the horizontal layout.
     * @param verticalLayout   the vertical layout.
     * @return the FIGlet of the text.
     */
    @JvmStatic
    @JvmOverloads
    fun figletify(text: String, font: FigletFont = Font.standard, horizontalLayout: Layout? = null, verticalLayout: Layout? = null): String {
        return joinFiglet(generateFiglet(text, font, horizontalLayout, verticalLayout))
    }

    /**
     * Returns the lines of the FIGlet of the text with the specified font.
     * @param text             the original text.
     * @param font             the specified font, for example `Font.standard`.
     * @param horizontalLayout the horizontal layout.
     * @param verticalLayout   the vertical layout.
     * @return the lines of the FIGlet of the text.
     */
    @JvmStatic
    @JvmOverloads
    fun generateFiglet(text: String, font: FigletFont = Font.standard, horizontalLayout: Layout? = null, verticalLayout: Layout? = null): Array<String> {
        val m = getMeta(font)
        val option = setLayout(m.option, horizontalLayout, verticalLayout)
        val lines = javaSplit(text, LINE_BREAK)
        val figletLines = lines.map { generateFigletLine(it, m.figletMap, option) }
        return figletLines.drop(1).fold(figletLines[0]) { output, line ->
            smushVerticalFigletLines(output, line, option)
        }
    }

    /**
     * Returns the parsed font, read once and kept for the next calls.
     */
    private fun getMeta(font: FigletFont): Meta {
        meta[font]?.let { return it }
        val newMeta = buildMeta(font)
        meta = meta + (font to newMeta)
        return newMeta
    }
}