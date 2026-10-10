package fr.godox.figlekt.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FiglektTests {

    @Test
    fun testExample() {
        // @formatter:off
        val expected =
                "  _   _      _ _          _____ _       _      _    _   _ \n" +
                " | | | | ___| | | ___    |  ___(_) __ _| | ___| | _| |_| |\n" +
                " | |_| |/ _ \\ | |/ _ \\   | |_  | |/ _` | |/ _ \\ |/ / __| |\n" +
                " |  _  |  __/ | | (_) |  |  _| | | (_| | |  __/   <| |_|_|\n" +
                " |_| |_|\\___|_|_|\\___( ) |_|   |_|\\__, |_|\\___|_|\\_\\\\__(_)\n" +
                "                     |/           |___/                   ";
        // @formatter:on
        assertEquals(expected, Figlekt.figletify("Hello, Figlekt!"))
    }

    @Test
    fun testMultiline() {
        val figlet = Figlekt.generateFiglet("Hello\nWorld")
        assertEquals(10, figlet.size)
        assertEquals(10, Figlekt.figletify("Hello\nWorld").count { it == '\n' } + 1)
    }

    @Test
    fun testLayout() {
        assertTrue(Figlekt.figletify("Hello", Layout.FULL).length > Figlekt.figletify("Hello").length)
    }

    @Test
    fun testFont() {
        assertEquals("Standard", Font.standard.name)
        assertEquals("Standard.flf", Font.standard.filename)
        assertTrue(Font.standard.data().startsWith("flf2a$ 6 5 16 15 13"))
        assertTrue(Font.standard === Font.standard)
    }
}
