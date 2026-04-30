plugins {
    java
    id("net.nuggetmc.java-conventions")
}

val jarName = "TerminatorPlus-" + version;

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":TerminatorPlus-Plugin"))
    implementation(project(":TerminatorPlus-API"))
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    from(configurations.compileClasspath.get().map { if (it.getName().endsWith(".jar")) zipTree(it) else it })
    archiveFileName.set(jarName + ".jar")
}

val movementOnlySources = listOf(
    file("TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/movement"),
    file("TerminatorPlus-API/src/main/java/net/nuggetmc/tplus/api/agent/legacyagent/ai/movement")
)

fun stripJavaCommentsAndStrings(source: String): String {
    val withoutBlocks = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL).replace(source) {
        "\n".repeat(it.value.count { ch -> ch == '\n' })
    }
    val withoutLines = withoutBlocks.lineSequence()
        .joinToString("\n") { line -> line.replace(Regex("//.*$"), "") }
    return Regex("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'").replace(withoutLines, "\"\"")
}

tasks.register("checkMovementOnlyContract") {
    group = "verification"
    description = "Scans movement-layer Java sources for banned combat-authority calls."

    val sourceFiles = movementOnlySources
        .filter { it.exists() }
        .flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension == "java" }.toList() }
    inputs.files(sourceFiles)

    doLast {
        val bannedCall = Regex(
            "(?i)(?:\\.|\\b)(" +
                    listOf(
                        "attack",
                        "punch",
                        "block",
                        "setBlocking",
                        "setShield",
                        "setBlockUse",
                        "selectMaterial",
                        "selectHotbarSlot",
                        "setSelectedHotbarSlot",
                        "applyNamedLoadoutToBot",
                        "applyTrainingLoadout",
                        "getBotInventory",
                        "setItem",
                        "setItemOffhand",
                        "useItem",
                        "startUsingItem",
                        "stopUsingItem",
                        "spawnProjectile",
                        "launchProjectile",
                        "throwPearl",
                        "throwWindCharge",
                        "placeCrystal",
                        "detonateCrystal",
                        "placeAnchor",
                        "detonateAnchor",
                        "placeCobweb",
                        "placeLava",
                        "useFirework",
                        "ticksFor"
                    ).joinToString("|") +
                    ")\\s*\\("
        )
        val violations = mutableListOf<String>()
        for (file in sourceFiles) {
            val stripped = stripJavaCommentsAndStrings(file.readText())
            bannedCall.findAll(stripped).forEach { match ->
                val line = stripped.substring(0, match.range.first).count { it == '\n' } + 1
                violations += "${file.relativeTo(projectDir)}:$line uses banned movement-layer call '${match.groupValues[1]}'"
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Movement-only contract violated:\n" + violations.joinToString("\n") +
                        "\nMovement code may walk/jump/sprint/sneak/report state, but combat actions stay in CombatDirector."
            )
        }
    }
}

tasks.named("check") {
    dependsOn("checkMovementOnlyContract")
}

//TODO currently, the resources are in src/main/resources, because gradle is stubborn and won't include the resources in TerminatorPlus-Plugin/src/main/resources, will need to fix

/*
task copyPlugin(type: Copy) {
    from 'build/libs/' + jarName + '.jar'
    into 'run/plugins'
}
 */

tasks.register("copyPlugin", Copy::class.java) {
    from("build/libs/" + jarName + ".jar")
    into("run/plugins")
}
