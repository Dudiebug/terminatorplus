plugins {
    java
    id("net.nuggetmc.java-conventions")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "minecraft-repo"
        url = uri("https://libraries.minecraft.net/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    // Paper 26.2's dev bundle supplies Authlib 9.0.75 at runtime.
    compileOnly("com.mojang:authlib:9.0.75")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("io.papermc.paper:paper-api:26.2.build.+")
    testImplementation("com.mojang:authlib:9.0.75")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.0")
}

tasks.test {
    useJUnitPlatform()
}
