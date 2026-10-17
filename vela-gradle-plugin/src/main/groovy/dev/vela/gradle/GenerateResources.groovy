package dev.vela.gradle

import dev.vela.tooling.ResourceCompiler
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
abstract class GenerateResources extends DefaultTask {
    @InputDirectory @PathSensitive(PathSensitivity.RELATIVE) abstract DirectoryProperty getInputDirectory()
    @Input abstract Property<String> getResourcePackage()
    @OutputDirectory abstract DirectoryProperty getOutputDirectory()

    @TaskAction void generate() {
        new ResourceCompiler().generate(inputDirectory.get().asFile.toPath(), resourcePackage.get(), outputDirectory.get().asFile.toPath())
    }
}
