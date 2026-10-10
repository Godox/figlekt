package fr.godox.figlekt.core

import kotlin.math.abs

/** The smushing algorithm, the FIGlet characters laid side by side and stacked. */

internal fun generateFigletLine(text: String, figletMap: Map<Int, Array<String>>, option: Option): Array<String> {
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

internal fun smushHorizontal(figlet1: Array<String>, figlet2: Array<String>, overlap: Int, option: Option): Array<String> {
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

internal fun getHorizontalSmushLength(text1: String, text2: String, option: Option): Int {
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

internal fun smushVerticalFigletLines(figlet1: Array<String>, figlet2: Array<String>, option: Option): Array<String> {
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

internal fun padLines(lines: Array<String>, numSpaces: Int): Array<String> {
    val padded = WHITESPACE.repeat(numSpaces)
    val result = Array(lines.size) { EMPTY }
    for (i in lines.indices) {
        result[i] = lines[i] + padded
    }
    return result
}

internal fun getVerticalSmushDist(figlet1: Array<String>, figlet2: Array<String>, option: Option): Int {
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
internal fun canSmushVertical(line1: String, line2: String, option: Option): Int {
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

internal fun smushVertical(figlet1: Array<String>, figlet2: Array<String>, overlap: Int, option: Option): Array<String> {
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

internal fun smushVerticalLines(line1: String, line2: String, option: Option): String {
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
internal fun smushHorizontalRule1(s1: String, s2: String, hardBlank: String?): String {
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
internal fun smushHorizontalRule2(s1: String, s2: String): String {
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
internal fun smushHorizontalRule3(s1: String, s2: String): String {
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
internal fun smushHorizontalRule4(s1: String, s2: String): String {
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
internal fun smushHorizontalRule5(s1: String, s2: String): String {
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
internal fun smushHorizontalRule6(s1: String, s2: String, hardBlank: String?): String {
    if (hardBlank != null && s1 == hardBlank && s2 == hardBlank) {
        return hardBlank
    }
    return EMPTY
}

/**
 * Rule 1: EQUAL CHARACTER SMUSHING (code value 256)
 * Same as horizontal smushing rule 1.
 */
internal fun smushVerticalRule1(s1: String, s2: String): String {
    if (s1 == s2) {
        return s1
    }
    return EMPTY
}

/**
 * Rule 2: UNDERSCORE SMUSHING (code value 512)
 * Same as horizontal smushing rule 2.
 */
internal fun smushVerticalRule2(s1: String, s2: String): String {
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
internal fun smushVerticalRule3(s1: String, s2: String): String {
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
internal fun smushVerticalRule4(s1: String, s2: String): String {
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
internal fun smushVerticalRule5(s1: String, s2: String): String {
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
internal fun smushUniversal(s1: String, s2: String, hardBlank: String?): String {
    return if (s2 == WHITESPACE || s2 == EMPTY) {
        s1
    } else if (s2 == hardBlank && s1 != WHITESPACE) {
        s1
    } else {
        s2
    }
}
