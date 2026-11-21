import java.nio.file.Files
import java.nio.file.attribute.DosFileAttributeView

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
}

// Windows & OneDrive safeguard: Ensure build directories are writable before task execution
allprojects {
    tasks.configureEach {
        doFirst {
            if (System.getProperty("os.name")?.lowercase()?.contains("windows") == true) {
                try {
                    val buildDir = layout.buildDirectory.asFile.orNull
                    if (buildDir != null && buildDir.exists()) {
                        buildDir.walkBottomUp().forEach { f ->
                            try {
                                val path = f.toPath()
                                val dosView = Files.getFileAttributeView(path, DosFileAttributeView::class.java)
                                if (dosView != null && dosView.readAttributes().isReadOnly) {
                                    dosView.setReadOnly(false)
                                }
                            } catch (_: Exception) {
                                // Non-fatal permission safeguard
                            }
                            if (!f.canWrite()) {
                                f.setWritable(true, false)
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Non-fatal permission safeguard
                }
            }
        }
    }
}
