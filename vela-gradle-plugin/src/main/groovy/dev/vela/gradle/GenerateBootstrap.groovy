package dev.vela.gradle

import dev.vela.tooling.ProjectConfig
import dev.vela.tooling.ResourceCompiler
import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import java.nio.charset.StandardCharsets

@CacheableTask
abstract class GenerateBootstrap extends DefaultTask {
    @InputFile @PathSensitive(PathSensitivity.RELATIVE) abstract RegularFileProperty getConfigurationFile()
    @OutputDirectory abstract DirectoryProperty getOutputDirectory()

    @TaskAction void generate() {
        def config = ProjectConfig.read(configurationFile.get().asFile.parentFile.toPath())
        def out = outputDirectory.get().asFile.toPath()
        def name = ResourceCompiler.xml(config.name()).replace('"', '&quot;')
        def manifest = """<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <application android:label="${name}" android:theme="@style/VelaTheme" android:allowBackup="false">
    <meta-data android:name="dev.vela.entry" android:value="${config.entry()}" />
    <activity android:name="dev.vela.android.VelaActivity" android:exported="true">
      <intent-filter><action android:name="android.intent.action.MAIN" /><category android:name="android.intent.category.LAUNCHER" /></intent-filter>
    </activity>
  </application>
</manifest>
"""
        def theme = '''<resources>
  <style name="VelaTheme" parent="android:style/Theme.Material.Light.NoActionBar">
    <item name="android:fontFamily">sans</item>
    <item name="android:windowLightStatusBar">true</item>
    <item name="android:windowActionModeOverlay">true</item>
    <item name="android:windowBackground">#FFFFFF</item>
  </style>
</resources>
'''
        ResourceCompiler.writeIfChanged(out.resolve('AndroidManifest.xml'), manifest.getBytes(StandardCharsets.UTF_8))
        ResourceCompiler.writeIfChanged(out.resolve('res/values/vela_theme.xml'), theme.getBytes(StandardCharsets.UTF_8))
        ResourceCompiler.writeIfChanged(out.resolve('keep.pro'), ("-keep class ${config.entry()} { public <init>(); }\n").getBytes(StandardCharsets.UTF_8))
    }
}
