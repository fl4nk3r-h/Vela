package dev.vela.gradle

import dev.vela.tooling.ProjectConfig
import org.gradle.api.*
import org.gradle.api.tasks.compile.JavaCompile

class VelaPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.pluginManager.apply('com.android.application')
        def config = ProjectConfig.read(project.projectDir.toPath())
        def android = project.extensions.getByName('android')
        android.namespace = config.packageName()
        android.compileSdk = config.compileSdk()
        android.defaultConfig {
            applicationId = config.packageName(); minSdk = config.minSdk(); targetSdk = config.targetSdk()
            versionCode = 1; versionName = '1.0'
            testInstrumentationRunner = 'android.test.InstrumentationTestRunner'
        }
        android.compileOptions {
            coreLibraryDesugaringEnabled = true
            sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17
        }
        project.dependencies.add('implementation', 'dev.vela:vela-android:0.1.0')
        project.dependencies.add('debugImplementation', 'dev.vela:vela-devtools-android:0.1.0')
        project.dependencies.add('coreLibraryDesugaring', 'com.android.tools:desugar_jdk_libs:2.1.5')
        def resources = project.tasks.register('generateVelaResources', GenerateResources) {
            inputDirectory.set(project.layout.projectDirectory.dir('src/main/vela'))
            resourcePackage.set(config.packageName())
            outputDirectory.set(project.layout.buildDirectory.dir('generated/vela/resources'))
        }
        def bootstrap = project.tasks.register('generateVelaBootstrap', GenerateBootstrap) {
            configurationFile.set(project.layout.projectDirectory.file('vela.yaml'))
            outputDirectory.set(project.layout.buildDirectory.dir('generated/vela/bootstrap'))
        }
        android.sourceSets.main {
            java.srcDir(resources.flatMap { it.outputDirectory.dir('java') })
            assets.srcDir(resources.flatMap { it.outputDirectory.dir('assets') })
            res.srcDir(resources.flatMap { it.outputDirectory.dir('res') })
            res.srcDir(bootstrap.flatMap { it.outputDirectory.dir('res') })
            manifest.srcFile(project.layout.buildDirectory.file('generated/vela/bootstrap/AndroidManifest.xml').get().asFile)
        }
        android.sourceSets.debug.assets.srcDir(project.layout.projectDirectory.dir('.vela/dev-assets'))
        project.tasks.named('preBuild').configure { dependsOn(resources, bootstrap) }
        android.buildTypes.release.proguardFiles(project.layout.buildDirectory.file('generated/vela/bootstrap/keep.pro').get().asFile)
        project.afterEvaluate {
            def javac = project.tasks.named('compileDebugJavaWithJavac', JavaCompile)
            project.tasks.register('velaReload', ReloadDex) {
                dependsOn(javac)
                classes.from(javac.flatMap { it.destinationDirectory })
                compileClasspath.from(javac.map { it.classpath })
                desugaring.from(project.configurations.getByName('coreLibraryDesugaring'))
                androidJar.set(new File(android.sdkDirectory, "platforms/android-${config.compileSdk()}/android.jar"))
                def suffix = System.getProperty('os.name').toLowerCase().contains('windows') ? '.bat' : ''
                d8.set(new File(android.sdkDirectory, "build-tools/${android.buildToolsVersion}/d8${suffix}"))
                minimumApi.set(config.minSdk())
                outputDirectory.set(project.layout.buildDirectory.dir('vela/reload'))
            }
        }
    }
}
