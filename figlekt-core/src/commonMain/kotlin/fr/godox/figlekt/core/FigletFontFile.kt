package fr.godox.figlekt.core

/** Reads a FIGlet font file: its header, its comments and its characters. */

internal fun buildMeta(font: FigletFont): Meta {
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

/**
 * Splits the string using Java's [String.split] semantics:
 * if the pattern matches nothing, the whole string is returned as a single element,
 * otherwise trailing empty strings are removed.
 */
internal fun javaSplit(text: String, regex: Regex): Array<String> {
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
internal fun readFigletLines(content: String): List<String> {
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
