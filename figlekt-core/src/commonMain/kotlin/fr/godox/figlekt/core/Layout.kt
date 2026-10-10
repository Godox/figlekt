package fr.godox.figlekt.core

import kotlin.jvm.JvmStatic

/**
 * Layouts.
 *
 */
enum class Layout(val code: Int) {
    DEFAULT(-1),
    FULL(0),
    FITTED(1),
    SMUSH_U(2),
    SMUSH_R(3);

    companion object {
        @JvmStatic
        fun get(code: Int?): Layout? {
            if (code == null) {
                return null
            }
            for (e in values()) {
                if (code == e.code) {
                    return e
                }
            }
            return null
        }
    }
}