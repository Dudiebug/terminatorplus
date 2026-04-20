plugins {
    java
    id("net.nuggetmc.java-conventions")
}

group = "net.nuggetmc"

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
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly("com.mojang:authlib:3.2.38")
    compileOnly("com.googlecode.json-simple:json-simple:1.1.1")
}
