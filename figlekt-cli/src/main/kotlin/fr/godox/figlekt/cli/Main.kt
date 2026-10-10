package fr.godox.figlekt.cli

import fr.godox.figlekt.generator.FigletFontGenerator
import java.io.File

private const val USAGE =
    "usage: figlekt <font.flf> [output.kt] [--package=<package>] [--base-package=<package>]\n" +
        "       figlekt --accessors <output.kt>"

/**
 * Generates the Kotlin class of a FIGlet font, running the very same code as the
 * Kotlin/Wasm web page, or the `FigletsExtension` accessors of the Gradle plugin.
 */
fun main(args: Array<String>) {
    if (args.firstOrNull() == "--accessors") {
        val target = File(args.getOrElse(1) { error(USAGE) })
        target.parentFile?.mkdirs()
        target.writeText(FigletFontGenerator.generateAccessors())
        println("Generated ${FigletCatalogSize} accessors in ${target.path}")
        return
    }
    var filename: String? = null
    var output: String? = null
    var packageName: String? = null
    var basePackage: String? = null
    for (arg in args) {
        when {
            arg.startsWith("--package=") -> packageName = arg.substringAfter('=')
            arg.startsWith("--base-package=") -> basePackage = arg.substringAfter('=')
            filename == null -> filename = arg
            output == null -> output = arg
            else -> error(USAGE)
        }
    }
    val file = filename?.let { File(it) } ?: run {
        println(USAGE)
        return
    }
    if (!file.isFile) {
        error("Font file '${file.path}' does not exist")
    }
    val source = FigletFontGenerator.generate(file.name, file.readBytes(), packageName ?: "", basePackage ?: "")
    if (output == null) {
        print(source)
        return
    }
    val target = File(output)
    target.parentFile?.mkdirs()
    target.writeText(source)
    println("Generated the ${FigletFontGenerator.functionName(file.name)} font in ${target.path}")
}

private val FigletCatalogSize = fr.godox.figlekt.generator.FigletCatalog.values.size