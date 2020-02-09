plugins {
    java
    idea
    eclipse
    maven
    id("com.github.hierynomus.license")
    id("ninja.miserable.blossom")
}

description = "The Ultimate Essentials Plugin API."

group = "io.github.nucleuspowered"

defaultTasks.add("licenseFormat")
defaultTasks.add("build")

repositories {
    jcenter()
    maven("http://repo.spongepowered.org/maven")
}

dependencies {
    implementation("org.spongepowered:spongeapi:" + rootProject.properties["spongeApiVersion"])
}

configure<nl.javadude.gradle.plugins.license.LicenseExtension> {
    val name: String = rootProject.name

    exclude("**/*.info")
    exclude("assets/**")
    exclude("*.properties")
    exclude("*.txt")

    header = file("../HEADER.txt")
    sourceSets = project.sourceSets

    ignoreFailures = false
    strictCheck = true

    mapping("java", "SLASHSTAR_STYLE")
}

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn.add(JavaPlugin.CLASSES_TASK_NAME)
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn.add(JavaPlugin.JAVADOC_TASK_NAME)
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

val copyJars by tasks.registering(Copy::class) {
    dependsOn(tasks.jar)
    from(
            project.tasks.jar,
            javadocJar
    )
    into(rootProject.file("output"))
}

tasks {

    jar {
        manifest {
            attributes["API-Title"] = project.name
            attributes["Implementation-Title"] = rootProject.name
            attributes["API-Version"] = project.version
            attributes["Implementation-Version"] = rootProject.version
            attributes["Git-Hash"] = project.name

        }

        archiveVersion.set("${rootProject.version}")
        archiveFileName.set("Nucleus-${rootProject.version}-API.jar")
    }

    build {
        dependsOn(javadocJar)
    }
}

blossom {
    replaceTokenIn("src/main/java/io/github/nucleuspowered/nucleus/api/NucleusAPITokens.java")
    replaceToken("@version@", rootProject.properties["nucleusVersion"])
    replaceToken("@description@", description)
    replaceToken("@gitHash@", rootProject.tasks.named("gitHash"))
}


artifacts {
    archives(javadocJar)
    archives(sourcesJar)
    archives(tasks.jar)
}