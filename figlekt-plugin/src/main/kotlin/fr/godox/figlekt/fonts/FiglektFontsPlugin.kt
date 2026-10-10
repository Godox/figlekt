package fr.godox.figlekt.fonts

import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Generates, for every source set, one `FigletFont` class and one `Font` extension per selected FIGlet font,
 * the font content itself being compiled into the artifact as plain Kotlin source.
 *
 * A font named `standard.flf` therefore becomes:
 *
 * ```kotlin
 * Figlekt.figletify("Hello, World!", Font.standard)
 * ```
 *
 * The generated sources are identical to the ones the Kotlin/Wasm web page produces.
 */
class FiglektFontsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("figlektFonts", FiglektFontsExtension::class.java).apply {
            figlektPackage.convention("fr.godox.figlekt")
            packageName.convention(figlektPackage)
        }
        project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
            kotlin.sourceSets.all { sourceSet ->
                val output = project.layout.buildDirectory.dir("generated/figletFonts/${sourceSet.name}")
                val task = project.tasks.register(
                    "generate${sourceSet.name.replaceFirstChar { it.uppercase() }}FigletFonts",
                    GenerateFigletFonts::class.java,
                ) { task ->
                    task.group = "figlekt"
                    task.description = "Generates FigletFont classes for the ${sourceSet.name} source set."
                    task.packageName.set(extension.packageName)
                    task.figlektPackage.set(extension.figlektPackage)
                    task.specs.set(project.provider { extension.figlets.specs.toList() })
                    // The fonts configured globally are generated once, by the default source set,
                    // to not compile them twice in the same target.
                    task.generate.set(project.provider {
                        val isDefaultSourceSet = sourceSet.name == "commonMain"
                        val isGlobal = extension.fonts.isPresent || extension.figlets.specs.isNotEmpty()
                        !isGlobal || isDefaultSourceSet
                    })
                    task.fonts.from(
                        extension.fonts
                            .orElse(project.layout.projectDirectory.dir("src/${sourceSet.name}/resources/flf"))
                            .map { it.asFileTree },
                    )
                    task.fonts.from(
                        project.provider {
                            extension.figlets.specs
                                .filter { it.startsWith("${FigletSpec.KIND_FILE}:") }
                                .map { File(it.substringAfter(':')) }
                        },
                    )
                    task.output.set(output)
                }
                sourceSet.kotlin.srcDir(output)
                project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
                    it.dependsOn(task)
                }
            }
        }
    }
}