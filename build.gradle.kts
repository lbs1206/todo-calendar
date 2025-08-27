plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.benson"
version = "1.0.2"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure IntelliJ Platform Gradle Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        create("IC", "2024.2")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here, example:
        // bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        version = project.version.toString()

        ideaVersion {
            sinceBuild = "242"
            untilBuild = "252.*"
        }

        changeNotes = """
            <h2>1.0.0 - Initial Release</h2>
            <ul>
                <li>📅 Calendar view for task visualization</li>
                <li>📋 Today panel with priority indicators</li>
                <li>✏️ Inline table editing with auto-save</li>
                <li>🎯 Smart startup notifications</li>
                <li>📊 Full task management (CRUD operations)</li>
                <li>📈 Status tracking (Waiting, In Progress, Done)</li>
                <li>🔄 Importance levels and priority system</li>
                <li>💾 Automatic data persistence per project</li>
            </ul>
        """.trimIndent()

        description = """
            <h1>Todo Calendar - Advanced Task Management for IntelliJ IDEA</h1>

            <p><strong>Todo Calendar</strong> is a comprehensive task management plugin that integrates seamlessly with IntelliJ IDEA to help developers organize and track their daily tasks.</p>

            <h2>✨ Key Features</h2>
            <ul>
                <li><strong>📅 Calendar View</strong> - Visualize your tasks in a monthly calendar format</li>
                <li><strong>📋 Today Panel</strong> - Quick overview of today's tasks with priority indicators</li>
                <li><strong>✏️ Inline Editing</strong> - Edit tasks directly in the table without dialog popups</li>
                <li><strong>🎯 Smart Notifications</strong> - Get notified about today's pending tasks at startup</li>
                <li><strong>📊 Task Management</strong> - Full CRUD operations with priority levels and importance settings</li>
                <li><strong>📈 Status Tracking</strong> - Track task progress with Waiting, In Progress, and Done states</li>
                <li><strong>🔄 Auto-save</strong> - All changes are automatically saved as you type</li>
            </ul>
        """.trimIndent()
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    buildSearchableOptions {
        enabled = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
