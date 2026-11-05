plugins {
    java
    pmd
    id("com.diffplug.spotless") version "8.10.2"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

spotless {
    java {
        googleJavaFormat().aosp()
    }
}

pmd {
    toolVersion = "7.27.0"
}

tasks.test {
    useJUnitPlatform()
}
