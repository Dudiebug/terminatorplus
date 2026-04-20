plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("net.nuggetmc.java-conventions")
}

group = "net.nuggetmc"
description = "TerminatorPlus"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

dependencies {
    paperweight.paperDevBundle("26.1.1.build.+")

    implementation(project(":TerminatorPlus-API"))
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(25)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }
}
