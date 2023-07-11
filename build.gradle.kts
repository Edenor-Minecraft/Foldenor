import io.papermc.paperweight.util.*
import io.papermc.paperweight.util.constants.*

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.papermc.paperweight.patcher") version "1.5.4"
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

repositories {
    mavenCentral()
    maven(paperMavenPublicUrl) {
        content { onlyForConfigurations(PAPERCLIP_CONFIG) }
    }
}

dependencies {
    remapper("net.fabricmc:tiny-remapper:0.8.6:fat")
    decompiler("net.minecraftforge:forgeflower:2.0.629.0")
    paperclip("io.papermc:paperclip:3.0.3")
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}

subprojects {
    tasks.withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }
    tasks.withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<ProcessResources> {
        filteringCharset = Charsets.UTF_8.name()
    }

    repositories {
        mavenCentral()
        maven(paperMavenPublicUrl)
        maven("https://jitpack.io")
    }
}

paperweight {
    serverProject.set(project(":foldenor-server"))

    remapRepo.set(paperMavenPublicUrl)
    decompileRepo.set(paperMavenPublicUrl)

    useStandardUpstream("Folia") {
        url.set(github("PaperMC", "Folia"))
        ref.set(providers.gradleProperty("foliaRef"))

        withStandardPatcher {
            baseName("Folia")

            apiPatchDir.set(layout.projectDirectory.dir("patches/api"))
            apiOutputDir.set(layout.projectDirectory.dir("Foldenor-api"))

            serverPatchDir.set(layout.projectDirectory.dir("patches/server"))
            serverOutputDir.set(layout.projectDirectory.dir("Foldenor-server"))
        }
    }
}

allprojects {
    publishing {
        repositories {
            maven("https://repo.papermc.io/repository/maven-snapshots/") {
                name = "paperSnapshots"
                credentials(PasswordCredentials::class)
            }
        }
    }
}

publishing {
    if (project.hasProperty("publishDevBundle")) {
        publications.create<MavenPublication>("devBundle") {
            artifact(tasks.generateDevelopmentBundle) {
                artifactId = "dev-bundle"
            }
        }
    }
}

tasks.register("foliaRefLatest") {
    // Update the foliaRef in gradle.properties to be the latest commit.
    val tempDir = layout.cacheDir("foliaRefLatest");
    val file = "gradle.properties";

    doFirst {
        data class GithubCommit(
            val sha: String
        )

        val foliaLatestCommitJson = layout.cache.resolve("foliaLatestCommit.json");
        download.get().download("https://api.github.com/repos/PaperMC/Folia/commits/master", foliaLatestCommitJson);
        val foliaLatestCommit = gson.fromJson<paper.libs.com.google.gson.JsonObject>(foliaLatestCommitJson)["sha"].asString;

        copy {
            from(file)
            into(tempDir)
            filter { line: String ->
                line.replace("foliaRef=.*".toRegex(), "foliaRef=$foliaLatestCommit")
            }
        }
    }

    doLast {
        copy {
            from(tempDir.file("gradle.properties"))
            into(project.file(file).parent)
        }
    }
}

tasks.withType<RebuildGitPatches> {
    filterPatches.set(false)
}

tasks.register("printMinecraftVersion") {
    doLast {
        println(providers.gradleProperty("mcVersion").get().trim())
    }
}

tasks.register("printPaperVersion") {
    doLast {
        println(project.version)
    }
}
