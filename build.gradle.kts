buildscript {
    repositories {
        mavenCentral()
    }
}

fun properties(key: String) = project.findProperty(key).toString()


plugins {
    id("org.jetbrains.intellij") version "1.13.1"
    java
    `java-library-distribution`
    publishing
    `maven-publish`
    signing
}

tasks.compileJava {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

group = "org.antlr"
version = properties("libraryVersion")

intellij {
    version.set(properties("ideaVersion"))

    pluginName.set("antlr4-intellij-adaptor")
    downloadSources.set(true)
    updateSinceUntilBuild.set(false)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.antlr:antlr4-runtime:${properties("antlr4Version")}") {
        exclude(group = "com.ibm.icu", module = "icu4j")
    }
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion")
    }
}


val sourcesJar = tasks.register<Jar>("sourcesJar") {
    from(sourceSets.main.get().allJava)
    archiveClassifier.set("sources")
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    from(tasks.javadoc)
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        this.register<MavenPublication>("maven") {
            groupId = "com.desiderantes"
            artifactId = "antlr4-intellij-adaptor"
            version = properties("libraryVersion")

            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)

            pom {
                name.set("ANTLRV4 adaptor for IntelliJ-based IDEs")
                description.set("Support for using ANTLR-generated parsers/lexers in IntelliJ-based IDE plug-ins.")
                url.set("https://github.com/antlr/antlr4-intellij-adaptor")
                licenses {
                    license {
                        name.set("BSD 2-Clause \"Simplified\" License")
                        url.set("https://github.com/desiderantes/antlr4-intellij-adaptor/blob/master/LICENSE")
                    }
                }
                developers {

                    rootDir.resolve("contributors.txt").bufferedReader()
                        .lines()
                        .filter { !it.startsWith("#") }
                        .filter { it.isNotBlank() }
                        .filter { !it.startsWith("YYYY") }
                        .map { it.split(",").map { s -> s.trim() } }
                        .forEach {
                            developer {
                                id.set(it[1])
                                name.set(it[2])
                                email.set(it[3])
                            }
                        }
                }
                scm {
                    connection.set("scm:git:git://github.com/desiderantes/antlr4-intellij-adaptor.git")
                    developerConnection.set("scm:git:git@github.com:desiderantes/antlr4-intellij-adaptor.git")
                    url.set("https://github.com/desiderantes/antlr4-intellij-adaptor")
                }
            }

        }
    }
    repositories {
        maven {
            uri("https://oss.sonatype.org/${if (properties("libraryVersion").contains("-SNAPSHOT")) "content/repositories/snapshots" else "service/local/staging/deploy/maven2"}")
            credentials {
                username = properties("sonatypeUsername")
                password = properties("sonatypePassword")
            }
        }
    }
}

signing {
    sign(publishing.publications.named("maven").get())
}
