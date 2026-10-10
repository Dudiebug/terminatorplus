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
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    // Paper 26.3's dev bundle supplies Authlib 10.0.77 at runtime.
    compileOnly("com.mojang:authlib:10.0.77")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("io.papermc.paper:paper-api:26.3.build.+")
    testImplementation("com.mojang:authlib:10.0.77")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.0")
}

tasks.test {
    useJUnitPlatform()
}
