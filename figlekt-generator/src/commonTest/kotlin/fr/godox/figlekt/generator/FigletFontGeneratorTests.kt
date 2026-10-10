package fr.godox.figlekt.generator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FigletFontGeneratorTests {

    @Test
    fun testNames() {
        assertEquals("Standard", FigletFontGenerator.fontName("Standard.flf"))
        assertEquals("standard", FigletFontGenerator.functionName("Standard.flf"))
        assertEquals("bigMoneyNe", FigletFontGenerator.functionName("Big_Money-ne.flf"))
        assertEquals("`3x5`", FigletFontGenerator.functionName("3x5.flf"))
        assertEquals("banner3D", FigletFontGenerator.accessorName("banner3-D.flf"))
    }

    @Test
    fun testGenerate() {
        val source = FigletFontGenerator.generate(
            "Standard.flf",
            "flf2a\$ 6 5\n| ||_|\n".encodeToByteArray(),
            "com.example.fonts",
        )
        assertEquals(
            // @formatter:off
            "package com.example.fonts\n" +
                    "\n" +
                    "import fr.godox.figlekt.FigletFont\n" +
                    "import fr.godox.figlekt.Font\n" +
                    "\n" +
                    "private var _standard: FigletFont? = null\n" +
                    "\n" +
                    "/** The FIGlet font `Standard.flf`. */\n" +
                    "public val Font.standard: FigletFont\n" +
                    "    get() = _standard ?: FigletFont(\"Standard\", \"Standard.flf\") {\n" +
                    "        \"flf2a\\$ 6 5\\n| ||_|\\n\"\n" +
                    "    }.also { _standard = it }\n" +
                    "\n",
            // @formatter:on
            source,
        )
    }

    @Test
    fun testGenerateSamePackage() {
        val source = FigletFontGenerator.generate(
            "doom.flf",
            "flf2a".encodeToByteArray(),
            "fr.godox.figlekt.core",
            "fr.godox.figlekt.core",
        )
        assertTrue(source.contains("public val Font.doom: FigletFont"))
        assertTrue(!source.contains("import"))
    }

    @Test
    fun testGenerateWithoutPackage() {
        val source = FigletFontGenerator.fontSource("Standard.flf", "flf2a\$$".encodeToByteArray())
        assertEquals(
            // @formatter:off
            "private var _standard: FigletFont? = null\n" +
                    "\n" +
                    "/** The FIGlet font `Standard.flf`. */\n" +
                    "public val Font.standard: FigletFont\n" +
                    "    get() = _standard ?: FigletFont(\"Standard\", \"Standard.flf\") {\n" +
                    "        \"flf2a\\$\\$\"\n" +
                    "    }.also { _standard = it }\n",
            // @formatter:on
            source,
        )
    }

    @Test
    fun testChunking() {
        val content = buildString { repeat(9000) { append('a' + (it % 26)) } }.encodeToByteArray()
        val source = FigletFontGenerator.fontSource("Big.flf", content)
        assertTrue(source.contains("buildString(9000)"))
        assertEquals(3, source.lines().count { it.contains("append(") })
    }

    @Test
    fun testAccessors() {
        val accessors = FigletFontGenerator.generateAccessors()
        assertTrue(accessors.contains("public class FigletsExtension : FigletsBase()"))
        assertTrue(accessors.contains("public val doom: FigletSelector = select(\"doom.flf\")"))
        assertEquals(FigletCatalog.values.size, accessors.lines().count { it.contains("FigletSelector = select(") })
    }
}