package fr.godox.figlekt.core

/** The text helpers, mirroring the StringBuilder logic of the original implementation. */

/**
 * Joins the generated lines, mirroring the StringBuilder logic of the original implementation.
 */
internal fun joinFiglet(lines: Array<String>): String {
    if (lines.isEmpty()) {
        return EMPTY
    }
    val joined = lines.joinToString("\n")
    return if (joined.isEmpty()) "\n" else joined
}

/**
 * Combines string array.
 * @param texts the tests.
 * @return the combined string array.
 */
internal fun concat(vararg texts: Array<String>): Array<String> {
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
internal fun substr(s: String, start: Int, length: Int): String {
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
internal fun slice(array: Array<String>, start: Int, end: Int): Array<String> {
    return array.copyOfRange(start, end)
}
