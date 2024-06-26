plugins {
    id("java")
}

group = "org.wiles.scheduler"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.0")
    testImplementation("net.datafaker:datafaker:2.2.2")
    implementation("com.google.ortools:ortools-java:9.10.4067")
    implementation("com.google.guava:guava:33.2.1-jre")
    // https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
    implementation("org.apache.commons:commons-lang3:3.14.0")

}

tasks.test {
    useJUnitPlatform()
}
