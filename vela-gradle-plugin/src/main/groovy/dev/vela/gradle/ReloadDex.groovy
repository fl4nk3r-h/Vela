package dev.vela.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import javax.inject.Inject
import java.nio.file.Files
import java.util.zip.*

/** Uses public SDK D8 tooling and Javac outputs; never assembles an APK. */
@CacheableTask
abstract class ReloadDex extends DefaultTask {
    @InputFiles @PathSensitive(PathSensitivity.RELATIVE) abstract ConfigurableFileCollection getClasses()
    @Classpath abstract ConfigurableFileCollection getCompileClasspath()
    @InputFile @PathSensitive(PathSensitivity.NONE) abstract RegularFileProperty getAndroidJar()
    @InputFile @PathSensitive(PathSensitivity.NONE) abstract RegularFileProperty getD8()
    @Input abstract Property<Integer> getMinimumApi()
    @OutputDirectory abstract DirectoryProperty getOutputDirectory()
    @Inject abstract ExecOperations getExecOperations()

    @TaskAction void compile() {
        def output = outputDirectory.get().asFile.toPath()
        Files.createDirectories(output)
        def jar = output.resolve('application.jar')
        def seen = new HashSet<String>()
        new ZipOutputStream(Files.newOutputStream(jar)).withCloseable { zip ->
            classes.files.sort().each { root ->
                if (root.isDirectory()) Files.walk(root.toPath()).withCloseable { paths ->
                    paths.filter { Files.isRegularFile(it) && it.toString().endsWith('.class') }.sorted().forEach { file ->
                        def name = root.toPath().relativize(file).toString().replace('\\', '/')
                        if (!name.matches('.*(^|/)(R(\\$.*)?|BuildConfig)\\.class') && seen.add(name)) {
                            def entry = new ZipEntry(name); entry.time = 0
                            zip.putNextEntry(entry); Files.copy(file, zip); zip.closeEntry()
                        }
                    }
                }
            }
        }
        Files.write(output.resolve('classes.list'), seen.sort().collect { it.substring(0, it.length() - 6).replace('/', '.') })
        Files.list(output).withCloseable { files ->
            files.filter { it.fileName.toString().matches('classes[0-9]*\\.dex') || it.fileName.toString() == 'restart-required' }
                    .forEach { Files.delete(it) }
        }
        def args = ['--debug', '--min-api', minimumApi.get().toString(), '--lib', androidJar.get().asFile.absolutePath, '--output', output.toString()]
        compileClasspath.files.findAll { it.exists() }.sort().each { args.addAll(['--classpath', it.absolutePath]) }
        args.add(jar.toString())
        def executable = d8.get().asFile.absolutePath
        def command = System.getProperty('os.name').toLowerCase().contains('windows') ? ['cmd', '/c', executable] : [executable]
        execOperations.exec { commandLine(command + args) }.assertNormalExitValue()
        if (Files.exists(output.resolve('classes2.dex'))) {
            Files.writeString(output.resolve('restart-required'), 'multiple-dex')
            throw new IllegalStateException('Reload payload needs multiple DEX files; use full restart for this application')
        }
    }
}
