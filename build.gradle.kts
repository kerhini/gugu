plugins {
    java
}

group = "com.jukeboxboat"
version = "1.21.11"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

sourceSets {
    create("mixerStubs") {
        java.srcDir("mixer-api-stubs")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    // Mixer API стабы для компиляции (compileOnly — не попадут в JAR)
    compileOnly(sourceSets["mixerStubs"].output)

    // Стабам тоже нужен Paper API
    "mixerStubsCompileOnly"("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}

tasks.named("compileJava") {
    dependsOn("compileMixerStubsJava")
}

tasks.jar {
    archiveBaseName.set("JukeboxBoat")
    archiveVersion.set(version.toString())
    // Исключаем стабы из JAR — они нужны только для компиляции
    exclude("me/andromedov/**")
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to version)
    }
}
