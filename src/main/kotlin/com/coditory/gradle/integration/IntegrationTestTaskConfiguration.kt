package com.coditory.gradle.integration

import com.coditory.gradle.integration.IntegrationTestPlugin.Companion.INTEGRATION_CONFIG_PREFIX
import com.coditory.gradle.integration.IntegrationTestPlugin.Companion.INTEGRATION_TEST_TASK_NAME
import com.coditory.gradle.integration.TestSkippingConditions.skipIntegrationTest
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.testing.Test
import org.gradle.language.base.plugins.LifecycleBasePlugin

internal object IntegrationTestTaskConfiguration {
    fun apply(project: Project) {
        val sourceSet = setupSourceSet(project)
        setupConfiguration(project)
        setupTestTask(project, sourceSet)
    }

    private fun setupSourceSet(project: Project): SourceSet {
        val javaConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
        val main = javaConvention.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        val test = javaConvention.sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)
        return javaConvention.sourceSets.create(INTEGRATION_CONFIG_PREFIX) {
            it.compileClasspath += test.output + main.output + test.compileClasspath
            it.runtimeClasspath += test.output + main.output + test.runtimeClasspath
        }
    }

    private fun setupConfiguration(project: Project) {
        listOf(
            "testAnnotationProcessor",
            "testCompile",
            "testCompileClasspath",
            "testCompileOnly",
            "testImplementation",
            "testRuntime",
            "testRuntimeClasspath",
            "testRuntimeOnly"
        )
            .filter { project.configurations.names.contains(it) }
            .forEach { setupConfiguration(project, it) }
    }

    private fun setupConfiguration(project: Project, testConfigName: String) {
        val integrationConfigName = testConfigName.replaceFirst("test", INTEGRATION_CONFIG_PREFIX)
        project.configurations.getByName(integrationConfigName) {
            it.extendsFrom(project.configurations.getByName(testConfigName))
            it.isVisible = true
            it.isTransitive = true
        }
    }

    private fun setupTestTask(project: Project, sourceSet: SourceSet) {
        val integrationTest = project.tasks.register(INTEGRATION_TEST_TASK_NAME, Test::class.java) {
            it.description = "Runs the $INTEGRATION_CONFIG_PREFIX tests."
            it.group = LifecycleBasePlugin.VERIFICATION_GROUP
            it.testClassesDirs = sourceSet.output.classesDirs
            it.classpath = sourceSet.runtimeClasspath
            it.mustRunAfter(JavaPlugin.TEST_TASK_NAME)
            it.onlyIf { !skipIntegrationTest(project) }
        }
        project.tasks.getByName(JavaBasePlugin.CHECK_TASK_NAME)
            .dependsOn(integrationTest)
    }
}
