package fr.godox.figlekt.core

/** The constants shared by the FIGlet rendering, kept apart from the public API. */

internal const val EMPTY = ""
internal const val WHITESPACE = " "

/** The smushing verdicts, as returned by [canSmushVertical]. */
internal const val INVALID = 0
internal const val VALID = 1
internal const val END = 2

private const val ASCII_FIRST = 32
private const val ASCII_LAST = 127

/** The ASCII codes, followed by the extra Latin-1 and Latin Extended-A ones. */
private val EXTRA_CODES = listOf(196, 214, 220, 223, 228, 246, 252)

internal val CODES: List<Int> = (ASCII_FIRST until ASCII_LAST) + EXTRA_CODES

internal val LINE_BREAK = Regex("\\r?\n")
internal val SPACE = Regex(" ")