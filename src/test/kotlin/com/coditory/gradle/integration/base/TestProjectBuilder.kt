package com.coditory.gradle.integration.base

import com.coditory.gradle.integration.IntegrationTestPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.reflect.KClass

class TestProjectBuilder private constructor(projectDir: File, name: String) {
    private val project = ProjectBuilder.builder()
        .withProjectDir(projectDir)
        .withName(name)
        .build() as DefaultProject

    fun withGroup(group: String): TestProjectBuilder {
        project.group = group
        return this
    }

    fun withVersion(version: String): TestProjectBuilder {
        project.version = version
        return this
    }

    fun withExtProperty(name: String, value: String): TestProjectBuilder {
        project.extensions.extraProperties[name] = value
        return this
    }

    fun withPlugins(vararg plugins: KClass<out Plugin<*>>): TestProjectBuilder {
        plugins
            .toList()
            .forEach { project.plugins.apply(it.java) }
        return this
    }

    fun withKtsBuildGradle(content: String): TestProjectBuilder {
        val buildFile = project.rootDir.resolve("build.gradle.kts")
        buildFile.writeText(content.trimIndent().trim())
        return this
    }

    fun withBuildGradle(content: String): TestProjectBuilder {
        val buildFile = project.rootDir.resolve("build.gradle")
        buildFile.writeText(content.trimIndent().trim())
        return this
    }

    fun withFile(path: String, content: String): TestProjectBuilder {
        val filePath = project.rootDir.resolve(path).toPath()
        Files.createDirectories(filePath.parent)
        val testFile = Files.createFile(filePath).toFile()
        testFile.writeText(content.trimIndent().trim())
        return this
    }

    fun withDirectory(path: String): TestProjectBuilder {
        val filePath = project.rootDir.resolve(path).toPath()
        Files.createDirectories(filePath)
        return this
    }

    fun build(): Project {
        project.evaluate()
        return project
    }

    companion object {
        private var projectDirs = mutableListOf<File>()

        fun createProject(): Project {
            return projectWithPlugins().build()
        }

        fun project(name: String = "sample-project"): TestProjectBuilder {
            return TestProjectBuilder(createProjectDir(name), name)
        }

        private fun projectWithPlugins(name: String = "sample-project"): TestProjectBuilder {
            return project(name)
                .withPlugins(JavaPlugin::class, IntegrationTestPlugin::class)
        }

        @Suppress("EXPERIMENTAL_API_USAGE_ERROR")
        private fun createProjectDir(directory: String): File {
            removeProjectDirs()
            val projectParentDir = createTempDirectory().toFile()
            val projectDir = projectParentDir.resolve(directory)
            projectDir.mkdir()
            projectDirs.add(projectParentDir)
            return projectDir
        }

        private fun removeProjectDirs() {
            projectDirs.forEach {
                it.deleteRecursively()
            }
        }
    }
}

fun Project.toBuildPath(vararg paths: String): String {
    return paths.joinToString(File.pathSeparator) {
        "${this.layout.buildDirectory.get()}${File.separator}${it.replace("/", File.separator)}"
    }
}

fun Project.readFileFromBuildDir(path: String): String? {
    return this.layout.buildDirectory.file(path).get().asFile.readText()
}
