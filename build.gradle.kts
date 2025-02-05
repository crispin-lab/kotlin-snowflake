import org.jmailen.gradle.kotlinter.tasks.InstallPreCommitHookTask
import org.jmailen.gradle.kotlinter.tasks.InstallPrePushHookTask

plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jmailen.kotlinter") version "5.0.1"
}

group = "com.crispin-lab"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.assertj:assertj-core:3.27.3")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

if (!rootProject.extra.has("install-git-hooks")) {
    rootProject.extra["install-git-hooks"] = true

    val preCommit: InstallPreCommitHookTask by project.rootProject.tasks.creating(
        InstallPreCommitHookTask::class
    ) {
        group = "build setup"
        description = "Installs Kotlinter Git pre-commit hook"
    }

    val prePush: InstallPrePushHookTask by project.rootProject.tasks.creating(
        InstallPrePushHookTask::class
    ) {
        group = "build setup"
        description = "Installs Kotlinter Git pre-push hook"
    }

    project.rootProject.tasks.getByName("prepareKotlinBuildScriptModel") {
        dependsOn(preCommit, prePush)
    }
}
