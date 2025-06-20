import com.vanniktech.maven.publish.SonatypeHost
import org.jmailen.gradle.kotlinter.tasks.InstallPreCommitHookTask
import org.jmailen.gradle.kotlinter.tasks.InstallPrePushHookTask

val libraryVersion = "1.0.1"

plugins {
    kotlin("jvm") version "1.9.25"
    id("org.jmailen.kotlinter") version "4.3.0"
    id("jacoco")
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "com.crispin-lab"
version = libraryVersion

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.assertj:assertj-core:3.27.3")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy("jacocoTestReport")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
        html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
    }
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

mavenPublishing {
    coordinates(
        groupId = "io.github.crispindeity",
        artifactId = "kotlin-snowflake",
        version = libraryVersion
    )

    pom {
        name.set("Kotlin Snowflake")
        description.set("A Kotlin library for generating Snowflake IDs.")
        inceptionYear.set("2025")
        url.set("https://github.com/crispin-lab/kotlin-snowflake")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("crispindeity")
                name.set("crispin")
                email.set("h.c.shin.dev09@gmail.com")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/crispin-lab/kotlin-snowflake.git")
            developerConnection.set(
                "scm:git:ssh://github.com/crispin-lab/kotlin-snowflake.git"
            )
            url.set("https://github.com/crispin-lab/kotlin-snowflake")
        }
    }
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
}
