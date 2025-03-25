plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.trustt"
version = "1.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.3")
    type.set("IC") // IntelliJ Community Edition
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("org.json:json:20231013")
}
