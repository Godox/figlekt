package fr.godox.figlekt.core

import kotlin.concurrent.Volatile
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.math.abs

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
 */
object Figlekt {
    private const val INVALID = 0
    private const val VALID = 1
    private const val END = 2
    private const val EMPTY = ""
    private const val WHITESPACE = " "

    private val CODES: List<Int> = run {
        val codes = ArrayList<Int>()
        // Adds ASCII codes.
        for (i in 32 until 127) {
            codes.add(i)
        }
        // Adds extra codes.
        codes.addAll(listOf(196, 214, 220, 223, 228, 246, 252))
        codes
    }

    private val LINE_BREAK = Regex("\\r?\n")
    private val SPACE = Regex(" ")

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
        val figletLines = Array(lines.size) { i ->
            generateFigletLine(lines[i], m.figletMap, option)
        }
        var output = figletLines[0]
        for (i in 1 until figletLines.size) {
            output = smushVerticalFigletLines(output, figletLines[i], option)
        }
        return output
    }

    /**
     * Joins the generated lines, mirroring the StringBuilder logic of the original implementation.
     */
    private fun joinFiglet(lines: Array<String>): String {
        if (lines.isEmpty()) {
            return EMPTY
        }
        val joined = lines.joinToString("\n")
        return if (joined.isEmpty()) "\n" else joined
    }

    private fun getMeta(font: FigletFont): Meta {
        meta[font]?.let { return it }
        val newMeta = buildMeta(font)
        meta = meta + (font to newMeta)
        return newMeta
    }

    private fun buildMeta(font: FigletFont): Meta {
        // Reads file content.
        val data = readFigletLines(font.data())
        // Builds option.
        val header = javaSplit(data[0], SPACE)
        val hardBlank = header[0].substring(5, 6)
        val height = header[1].toInt()
        val baseline = header[2].toInt()
        val oldLayout = header[4].toInt()
        val numCommentLines = header[5].toInt()
        val printDirection = if (header.size > 6) header[6].toInt() else 0
        val fullLayout = if (header.size > 7) header[7].toInt() else null
        val codeTagCount = if (header.size > 8) header[8].toInt() else null
        val option = Option(baseline, hardBlank, height, oldLayout, printDirection, fullLayout, codeTagCount,
                getSmushRule(oldLayout, fullLayout))
        // Builds FIGlet map, the comment lines are data[1..numCommentLines].
        val figletMap = HashMap<Int, Array<String>>(CODES.size)
        val num = numCommentLines + 1
        for (i in CODES.indices) {
            val code = CODES[i]
            if (i * height + num >= data.size) {
                break
            }
            @Suppress("UNCHECKED_CAST")
            val figlet = arrayOfNulls<String>(height) as Array<String>
            figletMap[code] = figlet
            for (j in 0 until height) {
                val row = i * height + j + num
                if (row >= data.size) {
                    figletMap.remove(code)
                    break
                }
                val charRow = data[row]

                // inspired by readfontchar method in figlet.c
                // https://github.com/cmatsuoka/figlet/blob/202a0a8110650a943f1125f536b3bb455cf72ee1/figlet.c#L1146-L1168
                // endmarks my differ for each characters in tlf files
                var charIndex = charRow.length - 1 // starts at the end

                while (charIndex >= 0 && charRow[charIndex].isWhitespace()) { // remove trailing space
                    charIndex--
                }

                val endChar = charRow[charIndex] // first endmark
                while (charIndex >= 0 && charRow[charIndex] == endChar) { // remove all endmarks
                    charIndex--
                }

                figlet[j] = charRow.substring(0, charIndex + 1)
            }
        }
        return Meta(option, figletMap)
    }

    private fun generateFigletLine(text: String, figletMap: Map<Int, Array<String>>, option: Option): Array<String> {
        val height = option.height
        var output = Array(height) { EMPTY }
        for (index in text.indices) {
            val figlet = figletMap[text[index].code] ?: continue
            var overlap = 0
            if (option.rule.horizontalLayout != Layout.FULL) {
                var length = Int.MAX_VALUE
                for (i in 0 until height) {
                    length = minOf(length, getHorizontalSmushLength(output[i], figlet[i], option))
                }
                overlap = if (length == Int.MAX_VALUE) 0 else length
            }
            output = smushHorizontal(output, figlet, overlap, option)
        }
        for (i in output.indices) {
            output[i] = output[i].replace(option.hardBlank, WHITESPACE)
        }
        return output
    }

    private fun setLayout(option: Option, horizontalLayout: Layout?, verticalLayout: Layout?): Option {
        if ((horizontalLayout == null || horizontalLayout == Layout.DEFAULT)
                && (verticalLayout == null || verticalLayout == Layout.DEFAULT)) {
            return option
        }
        val newOption = option.copy()
        setHorizontalLayout(newOption, horizontalLayout)
        setVerticalLayout(newOption, verticalLayout)
        return newOption
    }

    private fun setHorizontalLayout(option: Option, layout: Layout?) {
        if (layout == null || layout == Layout.DEFAULT) {
            return
        }
        val rule = option.rule
        when (layout) {
            Layout.FULL -> rule.setHorizontal(Layout.FULL, false, false, false, false, false, false)
            Layout.FITTED -> rule.setHorizontal(Layout.FITTED, false, false, false, false, false, false)
            Layout.SMUSH_U -> rule.setHorizontal(Layout.SMUSH_U, false, false, false, false, false, false)
            Layout.SMUSH_R -> rule.setHorizontal(Layout.SMUSH_R, true, true, true, true, true, true)
            else -> {}
        }
    }

    private fun setVerticalLayout(option: Option, layout: Layout?) {
        if (layout == null || layout == Layout.DEFAULT) {
            return
        }
        val rule = option.rule
        when (layout) {
            Layout.FULL -> rule.setVertical(Layout.FULL, false, false, false, false, false)
            Layout.FITTED -> rule.setVertical(Layout.FITTED, false, false, false, false, false)
            Layout.SMUSH_U -> rule.setVertical(Layout.SMUSH_U, false, false, false, false, false)
            Layout.SMUSH_R -> rule.setVertical(Layout.SMUSH_R, true, true, true, true, true)
            else -> {}
        }
    }

    private fun smushHorizontal(figlet1: Array<String>, figlet2: Array<String>, overlap: Int, option: Option): Array<String> {
        val height = option.height
        val rule = option.rule
        val hardBlank = option.hardBlank
        val output = Array(height) { EMPTY }
        for (i in 0 until height) {
            val text1 = figlet1[i]
            val text2 = figlet2[i]
            val len1 = text1.length
            val len2 = text2.length
            val overlapStart = len1 - overlap
            val piece = StringBuilder()
            piece.append(substr(text1, 0, maxOf(0, overlapStart)))
            // Determines overlap piece.
            val seg1 = substr(text1, maxOf(0, len1 - overlap), overlap)
            val seg2 = substr(text2, 0, minOf(overlap, len2))
            for (j in 0 until overlap) {
                val ch1 = if (j < len1) substr(seg1, j, 1) else WHITESPACE
                val ch2 = if (j < len2) substr(seg2, j, 1) else WHITESPACE
                if (ch1 != WHITESPACE && ch2 != WHITESPACE) {
                    when (rule.horizontalLayout) {
                        Layout.FITTED -> piece.append(smushUniversal(ch1, ch2, hardBlank))
                        Layout.SMUSH_U -> piece.append(smushUniversal(ch1, ch2, hardBlank))
                        else -> {
                            var nextCh = EMPTY
                            if (nextCh.isEmpty() && rule.horizontal1)
                                nextCh = smushHorizontalRule1(ch1, ch2, hardBlank)
                            if (nextCh.isEmpty() && rule.horizontal2)
                                nextCh = smushHorizontalRule2(ch1, ch2)
                            if (nextCh.isEmpty() && rule.horizontal3)
                                nextCh = smushHorizontalRule3(ch1, ch2)
                            if (nextCh.isEmpty() && rule.horizontal4)
                                nextCh = smushHorizontalRule4(ch1, ch2)
                            if (nextCh.isEmpty() && rule.horizontal5)
                                nextCh = smushHorizontalRule5(ch1, ch2)
                            if (nextCh.isEmpty() && rule.horizontal6)
                                nextCh = smushHorizontalRule6(ch1, ch2, hardBlank)
                            if (nextCh.isEmpty())
                                nextCh = smushUniversal(ch1, ch2, hardBlank)
                            piece.append(nextCh)
                        }
                    }
                } else {
                    piece.append(smushUniversal(ch1, ch2, hardBlank))
                }
            }
            if (overlap < len2) {
                piece.append(substr(text2, overlap, maxOf(0, len2 - overlap)))
            }
            output[i] = piece.toString()
        }
        return output
    }

    private fun getHorizontalSmushLength(text1: String, text2: String, option: Option): Int {
        val rule = option.rule
        val hardBlank = option.hardBlank
        if (rule.horizontalLayout == Layout.FULL) {
            return 0
        }
        val len1 = text1.length
        val len2 = text2.length
        var curDist = 1
        val maxDist = text1.length
        var breakAfter = false
        var validSmush = false
        if (len1 == 0) {
            return 0
        }
        while (curDist <= maxDist) {
            var skip = false
            val seg1 = substr(text1, len1 - curDist, len1)
            val seg2 = substr(text2, 0, minOf(curDist, len2))
            for (i in 0 until minOf(curDist, len2)) {
                val ch1 = substr(seg1, i, 1)
                val ch2 = substr(seg2, i, 1)
                if (ch1 != WHITESPACE && ch2 != WHITESPACE) {
                    if (rule.horizontalLayout == Layout.FITTED) {
                        curDist = curDist - 1
                        skip = true
                        break
                    } else if (rule.horizontalLayout == Layout.SMUSH_U) {
                        if (ch1 == hardBlank || ch2 == hardBlank) {
                            curDist = curDist - 1
                        }
                        skip = true
                        break
                    } else {
                        breakAfter = true
                        validSmush = false
                        if (rule.horizontal1)
                            validSmush = smushHorizontalRule1(ch1, ch2, hardBlank).isNotEmpty()
                        if (!validSmush && rule.horizontal2)
                            validSmush = smushHorizontalRule2(ch1, ch2).isNotEmpty()
                        if (!validSmush && rule.horizontal3)
                            validSmush = smushHorizontalRule3(ch1, ch2).isNotEmpty()
                        if (!validSmush && rule.horizontal4)
                            validSmush = smushHorizontalRule4(ch1, ch2).isNotEmpty()
                        if (!validSmush && rule.horizontal5)
                            validSmush = smushHorizontalRule5(ch1, ch2).isNotEmpty()
                        if (!validSmush && rule.horizontal6)
                            validSmush = smushHorizontalRule6(ch1, ch2, hardBlank).isNotEmpty()
                        if (!validSmush) {
                            curDist = curDist - 1
                            skip = true
                            break
                        }
                    }
                }
            }
            if (skip) {
                break
            }
            if (breakAfter) {
                break
            }
            curDist++
        }
        return minOf(maxDist, curDist)
    }

    private fun getSmushRule(oldLayout: Int, newLayout: Int?): Rule {
        val rules = HashMap<String, Int>()
        var layout = newLayout ?: oldLayout
        for (rule in RuleEnum.values()) {
            val key = rule.key
            val code = rule.code
            val value = rule.value
            if (layout >= code) {
                layout -= code
                if (!rules.containsKey(key)) {
                    rules[key] = value
                }
            } else if (RuleEnum.HORIZONTAL_LAYOUT_SMUSH.key != key
                    && RuleEnum.VERTICAL_LAYOUT_SMUSH.key != key) {
                rules[key] = 0
            }
        }
        val rule = Rule(
                Layout.get(rules[RuleEnum.HORIZONTAL_LAYOUT_SMUSH.key]),
                rules[RuleEnum.HORIZONTAL_1.key] == 1,
                rules[RuleEnum.HORIZONTAL_2.key] == 1,
                rules[RuleEnum.HORIZONTAL_3.key] == 1,
                rules[RuleEnum.HORIZONTAL_4.key] == 1,
                rules[RuleEnum.HORIZONTAL_5.key] == 1,
                rules[RuleEnum.HORIZONTAL_6.key] == 1,
                Layout.get(rules[RuleEnum.VERTICAL_LAYOUT_SMUSH.key]),
                rules[RuleEnum.VERTICAL_1.key] == 1,
                rules[RuleEnum.VERTICAL_2.key] == 1,
                rules[RuleEnum.VERTICAL_3.key] == 1,
                rules[RuleEnum.VERTICAL_4.key] == 1,
                rules[RuleEnum.VERTICAL_5.key] == 1
        )
        if (rule.horizontalLayout == null) {
            if (oldLayout == 0) {
                rule.horizontalLayout = Layout.FITTED
            } else if (oldLayout == -1) {
                rule.horizontalLayout = Layout.FULL
            } else if (rule.horizontal1
                    || rule.horizontal2
                    || rule.horizontal3
                    || rule.horizontal4
                    || rule.horizontal5
                    || rule.horizontal6) {
                rule.horizontalLayout = Layout.SMUSH_R
            } else {
                rule.horizontalLayout = Layout.SMUSH_U
            }
        } else if ((rule.horizontalLayout == Layout.SMUSH_U
                        && rule.horizontal1)
                || rule.horizontal2
                || rule.horizontal3
                || rule.horizontal4
                || rule.horizontal5
                || rule.horizontal6) {
            rule.horizontalLayout = Layout.SMUSH_R
        }
        if (rule.verticalLayout == null) {
            if (rule.vertical1
                    || rule.vertical2
                    || rule.vertical3
                    || rule.vertical4
                    || rule.vertical5) {
                rule.verticalLayout = Layout.SMUSH_R
            } else {
                rule.verticalLayout = Layout.FULL
            }
        } else if ((rule.verticalLayout == Layout.SMUSH_U
                        && rule.vertical1)
                || rule.vertical2
                || rule.vertical3
                || rule.vertical4
                || rule.vertical5) {
            rule.verticalLayout = Layout.SMUSH_R
        }
        return rule
    }

    private fun smushVerticalFigletLines(figlet1: Array<String>, figlet2: Array<String>, option: Option): Array<String> {
        val len1 = figlet1[0].length
        val len2 = figlet2[0].length
        val padded1: Array<String>
        val padded2: Array<String>
        if (len1 > len2) {
            padded1 = figlet1
            padded2 = padLines(figlet2, len1 - len2)
        } else if (len2 > len1) {
            padded1 = padLines(figlet1, len2 - len1)
            padded2 = figlet2
        } else {
            padded1 = figlet1
            padded2 = figlet2
        }
        val overlap = getVerticalSmushDist(padded1, padded2, option)
        return smushVertical(padded1, padded2, overlap, option)
    }

    private fun padLines(lines: Array<String>, numSpaces: Int): Array<String> {
        val padded = WHITESPACE.repeat(numSpaces)
        val result = Array(lines.size) { EMPTY }
        for (i in lines.indices) {
            result[i] = lines[i] + padded
        }
        return result
    }

    private fun getVerticalSmushDist(figlet1: Array<String>, figlet2: Array<String>, option: Option): Int {
        var curDist = 1
        val maxDist = figlet1.size
        val len1 = figlet1.size
        while (curDist <= maxDist) {
            val subLines1 = slice(figlet1, maxOf(0, len1 - curDist), len1)
            val subLines2 = slice(figlet2, 0, minOf(maxDist, curDist))
            var result = VALID
            for (i in subLines2.indices) {
                val ret = canSmushVertical(subLines1[i], subLines2[i], option)
                if (ret == END) {
                    result = ret
                } else if (ret == INVALID) {
                    result = ret
                    break
                }
            }
            if (result == INVALID) {
                curDist--
                break
            }
            if (result == END) {
                break
            }
            curDist++
        }
        return minOf(maxDist, curDist)
    }

    /**
     * Takes in two lines of text and returns one of the following:
     * "valid" - These lines can be smushed together given the current smushing rules.
     * "end" - The lines can be smushed, but we're at a stopping point.
     * "invalid" - The two lines cannot be smushed together.
     * @param line1  A line of text.
     * @param line2  A line of text.
     * @param option the option.
     */
    private fun canSmushVertical(line1: String, line2: String, option: Option): Int {
        val rule = option.rule
        if (rule.verticalLayout == Layout.FULL) {
            return INVALID
        }
        val len = minOf(line1.length, line2.length)
        var endSmush = false
        if (len == 0) {
            return INVALID
        }
        for (i in 0 until len) {
            val ch1 = substr(line1, i, 1)
            val ch2 = substr(line2, i, 1)
            if (ch1 != WHITESPACE && ch2 != WHITESPACE) {
                if (rule.verticalLayout == Layout.FITTED) {
                    return INVALID
                } else if (rule.verticalLayout == Layout.SMUSH_U) {
                    return END
                } else {
                    if (smushVerticalRule5(ch1, ch2).isNotEmpty()) {
                        continue
                    }
                    // rule 5 allow for "super" smushing, but only if we're not already ending this smush
                    var validSmush = false
                    if (rule.vertical1)
                        validSmush = smushVerticalRule1(ch1, ch2).isNotEmpty()
                    if (!validSmush && rule.vertical2)
                        validSmush = smushVerticalRule2(ch1, ch2).isNotEmpty()
                    if (!validSmush && rule.vertical3)
                        validSmush = smushVerticalRule3(ch1, ch2).isNotEmpty()
                    if (!validSmush && rule.vertical4)
                        validSmush = smushVerticalRule4(ch1, ch2).isNotEmpty()
                    endSmush = true
                    if (!validSmush) {
                        return INVALID
                    }
                }
            }
        }
        return if (endSmush) {
            END
        } else {
            VALID
        }
    }

    private fun smushVertical(figlet1: Array<String>, figlet2: Array<String>, overlap: Int, option: Option): Array<String> {
        val len1 = figlet1.size
        val len2 = figlet2.size
        val piece1 = slice(figlet1, 0, maxOf(0, len1 - overlap))
        val piece21 = slice(figlet1, maxOf(0, len1 - overlap), len1)
        val piece22 = slice(figlet2, 0, minOf(overlap, len2))
        val piece2 = Array(piece21.size) { EMPTY }
        for (i in piece21.indices) {
            piece2[i] = if (i >= len2) {
                piece21[i]
            } else {
                smushVerticalLines(piece21[i], piece22[i], option)
            }
        }
        val piece3 = slice(figlet2, minOf(overlap, len2), len2)
        return concat(piece1, piece2, piece3)
    }

    private fun smushVerticalLines(line1: String, line2: String, option: Option): String {
        val rule = option.rule
        val len = minOf(line1.length, line2.length)
        var validSmush = EMPTY
        val result = StringBuilder()
        for (i in 0 until len) {
            val ch1 = substr(line1, i, 1)
            val ch2 = substr(line2, i, 1)
            if (ch1 != WHITESPACE && ch2 != WHITESPACE) {
                if (rule.verticalLayout == Layout.FITTED) {
                    result.append(smushUniversal(ch1, ch2, null))
                } else if (rule.verticalLayout == Layout.SMUSH_U) {
                    result.append(smushUniversal(ch1, ch2, null))
                } else {
                    if (rule.vertical5)
                        validSmush = smushVerticalRule5(ch1, ch2)
                    if (validSmush.isEmpty() && rule.vertical1)
                        validSmush = smushVerticalRule1(ch1, ch2)
                    if (validSmush.isEmpty() && rule.vertical2)
                        validSmush = smushVerticalRule2(ch1, ch2)
                    if (validSmush.isEmpty() && rule.vertical3)
                        validSmush = smushVerticalRule3(ch1, ch2)
                    if (validSmush.isEmpty() && rule.vertical4)
                        validSmush = smushVerticalRule4(ch1, ch2)
                    result.append(validSmush)
                }
            } else {
                result.append(smushUniversal(ch1, ch2, null))
            }
        }
        return result.toString()
    }

    /**
     * Rule 1: EQUAL CHARACTER SMUSHING (code value 1)
     * Two sub-characters are smushed into a single sub-character
     * if they are the same.  This rule does not smush hardblanks.
     * (See rule 6 on hardblanks below)
     */
    private fun smushHorizontalRule1(s1: String, s2: String, hardBlank: String?): String {
        if (s1 == s2 && s1 != hardBlank) {
            return s1
        }
        return EMPTY
    }

    /**
     * Rule 2: UNDERSCORE SMUSHING (code value 2)
     * An underscore ("_") will be replaced by any of:
     * "|", "/", "\", "[", "]", "{", "}", "(", ")", "<" or ">".
     */
    private fun smushHorizontalRule2(s1: String, s2: String): String {
        val rule = "|/\\[]{}()<>"
        if (s1 == "_") {
            if (rule.contains(s2)) {
                return s2
            }
        } else if (s2 == "_") {
            if (rule.contains(s1)) {
                return s1
            }
        }
        return EMPTY
    }

    /**
     * Rule 3: HIERARCHY SMUSHING (code value 4)
     * A hierarchy of six classes is used: "|", "/\", "[]", "{}", "()", and "<>".
     * When two smushing sub-characters are from different classes,
     * the one from the latter class will be used.
     */
    private fun smushHorizontalRule3(s1: String, s2: String): String {
        val rule = "| /\\ [] {} () <>"
        val pos1 = rule.indexOf(s1)
        val pos2 = rule.indexOf(s2)
        if (pos1 != -1 && pos2 != -1 && pos1 != pos2 && abs(pos1 - pos2) != 1) {
            return substr(rule, maxOf(pos1, pos2), 1)
        }
        return EMPTY
    }

    /**
     * Rule 4: OPPOSITE PAIR SMUSHING (code value 8)
     * Smushes opposing brackets ("[]" or "]["), braces ("{}" or "}{")
     * and parentheses ("()" or ")(") together, replacing
     * any such pair with a vertical bar ("|").
     */
    private fun smushHorizontalRule4(s1: String, s2: String): String {
        val rule = "[] {} ()"
        val pos1 = rule.indexOf(s1)
        val pos2 = rule.indexOf(s2)
        if (pos1 != -1 && pos2 != -1 && abs(pos1 - pos2) <= 1) {
            return "|"
        }
        return EMPTY
    }

    /**
     * Rule 5: BIG X SMUSHING (code value 16)
     * Smushes "/\" into "|", "\/" into "Y", and "><" into "X".
     * Note that "<>" is not smushed in any way by this rule.
     * The name "BIG X" is historical; originally all three pairs
     * were smushed into "X".
     */
    private fun smushHorizontalRule5(s1: String, s2: String): String {
        val rule = "/\\ \\/ ><"
        val pos1 = rule.indexOf(s1)
        val pos2 = rule.indexOf(s2)
        if (pos1 != -1 && pos2 != -1 && pos2 - pos1 == 1) {
            if (pos1 == 0) {
                return "|"
            } else if (pos1 == 3) {
                return "Y"
            } else if (pos1 == 6) {
                return "X"
            }
        }
        return EMPTY
    }

    /**
     * Rule 6: HARDBLANK SMUSHING (code value 32)
     * Smushes two hardblanks together, replacing them with a single hardblank.
     * (See "Hardblanks" below.)
     */
    private fun smushHorizontalRule6(s1: String, s2: String, hardBlank: String?): String {
        if (hardBlank != null && s1 == hardBlank && s2 == hardBlank) {
            return hardBlank
        }
        return EMPTY
    }

    /**
     * Rule 1: EQUAL CHARACTER SMUSHING (code value 256)
     * Same as horizontal smushing rule 1.
     */
    private fun smushVerticalRule1(s1: String, s2: String): String {
        if (s1 == s2) {
            return s1
        }
        return EMPTY
    }

    /**
     * Rule 2: UNDERSCORE SMUSHING (code value 512)
     * Same as horizontal smushing rule 2.
     */
    private fun smushVerticalRule2(s1: String, s2: String): String {
        val rule = "|/\\[]{}()<>"
        if (s1 == "_") {
            if (rule.contains(s2)) {
                return s2
            }
        } else if (s2 == "_") {
            if (rule.contains(s1)) {
                return s1
            }
        }
        return EMPTY
    }

    /**
     * Rule 3: HIERARCHY SMUSHING (code value 1024)
     * Same as horizontal smushing rule 3.
     */
    private fun smushVerticalRule3(s1: String, s2: String): String {
        val rule = "| /\\ [] {} () <>"
        val pos1 = rule.indexOf(s1)
        val pos2 = rule.indexOf(s2)
        if (pos1 != -1 && pos2 != -1 && pos1 != pos2 && abs(pos1 - pos2) != 1) {
            return substr(rule, maxOf(pos1, pos2), 1)
        }
        return EMPTY
    }

    /**
     * Rule 4: HORIZONTAL LINE SMUSHING (code value 2048)
     * Smushes stacked pairs of "-" and "_", replacing them with
     * a single "=" sub-character.  It does not matter which is
     * found above the other.  Note that vertical smushing rule 1
     * will smush IDENTICAL pairs of horizontal lines, while this
     * rule smushes horizontal lines consisting of DIFFERENT
     * sub-characters.
     */
    private fun smushVerticalRule4(s1: String, s2: String): String {
        if ((s1 == "-" && s2 == "_") || (s1 == "_" && s2 == "-")) {
            return "="
        }
        return EMPTY
    }

    /**
     * Rule 5: VERTICAL LINE SUPERSMUSHING (code value 4096)
     * This one rule is different from all others, in that it
     * "supersmushes" vertical lines consisting of several
     * vertical bars ("|").  This creates the illusion that
     * FIGcharacters have slid vertically against each other.
     * Supersmushing continues until any sub-characters other
     * than "|" would have to be smushed.  Supersmushing can
     * produce impressive results, but it is seldom possible,
     * since other sub-characters would usually have to be
     * considered for smushing as soon as any such stacked
     * vertical lines are encountered.
     */
    private fun smushVerticalRule5(s1: String, s2: String): String {
        if (s1 == "|" && s2 == "|") {
            return "|"
        }
        return EMPTY
    }

    /**
     * Universal smushing simply overrides the sub-character from the
     * earlier FIGcharacter with the sub-character from the later
     * FIGcharacter.  This produces an "overlapping" effect with some
     * FIGfonts, wherin the latter FIGcharacter may appear to be "in front".
     */
    private fun smushUniversal(s1: String, s2: String, hardBlank: String?): String {
        return if (s2 == WHITESPACE || s2 == EMPTY) {
            s1
        } else if (s2 == hardBlank && s1 != WHITESPACE) {
            s1
        } else {
            s2
        }
    }

    /**
     * Combines string array.
     * @param texts the tests.
     * @return the combined string array.
     */
    private fun concat(vararg texts: Array<String>): Array<String> {
        if (texts.isEmpty()) {
            return arrayOf()
        }
        return texts.flatten().toTypedArray()
    }

    /**
     * Returns a string that is a substring of this string.
     * @param s      the string.
     * @param start  the beginning index.
     * @param length the length of specified substring.
     * @return the specified substring.
     */
    private fun substr(s: String, start: Int, length: Int): String {
        if (s.isEmpty()) {
            return EMPTY
        }
        return s.substring(start, minOf(start + length, s.length))
    }

    /**
     * Returns a section of an array.
     * @param array the array.
     * @param start the beginning of the specified portion of the array.
     * @param end   the end of the specified portion of the array.
     */
    private fun slice(array: Array<String>, start: Int, end: Int): Array<String> {
        return array.copyOfRange(start, end)
    }

    /**
     * Splits the string using Java's [String.split] semantics:
     * if the pattern matches nothing, the whole string is returned as a single element,
     * otherwise trailing empty strings are removed.
     */
    private fun javaSplit(text: String, regex: Regex): Array<String> {
        val matches = regex.findAll(text).toList()
        if (matches.isEmpty()) {
            return arrayOf(text)
        }
        val result = ArrayList<String>()
        var index = 0
        for (match in matches) {
            result.add(text.substring(index, match.range.first))
            index = match.range.last + 1
        }
        result.add(text.substring(index))
        while (result.isNotEmpty() && result[result.size - 1].isEmpty()) {
            result.removeAt(result.size - 1)
        }
        return result.toTypedArray()
    }

    /**
     * Reads lines the way [java.io.BufferedReader.readLine] does:
     * lines are separated by `\n`, `\r` or `\r\n`, and a trailing line
     * terminator does not produce an extra empty line.
     * An empty input produces no lines at all.
     */
    private fun readFigletLines(content: String): List<String> {
        val lines = ArrayList<String>()
        var start = 0
        var i = 0
        while (i < content.length) {
            val c = content[i]
            if (c == '\r' || c == '\n') {
                lines.add(content.substring(start, i))
                if (c == '\r' && i + 1 < content.length && content[i + 1] == '\n') {
                    i++
                }
                start = i + 1
            }
            i++
        }
        if (start < content.length) {
            lines.add(content.substring(start))
        }
        return lines
    }
}