package fr.godox.figlekt.fonts

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

/**
 * Configuration of the `fr.godox.figlekt.fonts` plugin.
 *
 * ```kotlin
 * figlektFonts {
 *     figlektPackage.set("fr.godox.figlekt")
 *     packageName.set("com.example.fonts")
 *     fonts.set(layout.projectDirectory.dir("src/main/resources/flf"))
 *     figlets {
 *         +basic
 *         +doom
 *         +file("fonts/MyFont.flf")
 *         +url("https://www.figlet.org/fonts/big.flf")
 *     }
 * }
 * ```
 *
 * When no font is selected, all the fonts of [fonts] are compiled.
 */
abstract class FiglektFontsExtension {

    /**
     * The package holding the `Font` object and the `FigletFont` class, which the generated code refers to.
     * Defaults to `fr.godox.figlekt`.
     */
    abstract val figlektPackage: Property<String>

    /**
     * The package of the generated code. Defaults to [figlektPackage].
     */
    abstract val packageName: Property<String>

    /**
     * The directory holding the `.flf` files, shared by all source sets.
     * Defaults to `<project>/src/<sourceSet>/resources/flf` for each source set.
     */
    abstract val fonts: DirectoryProperty

    /**
     * The fonts to compile.
     */
    internal val figlets = FigletsExtension()

    /**
     * Configures the fonts to compile.
     */
    fun figlets(action: FigletsExtension.() -> Unit) {
        figlets.action()
    }
}