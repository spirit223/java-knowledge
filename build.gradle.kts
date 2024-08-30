plugins {
    java
    id("org.springframework.boot") version "3.3.2"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "cc.sika"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
//        languageVersion = JavaLanguageVersion.of(21)
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
//tasks.withType<JavaExec> {
//    systemProperty("file.encoding", "UTF-8")
//}
tasks.withType<JavaExec> {
    systemProperty("file.encoding", "UTF-8")
}
//tasks.withType(JavaCompile::class.java) {
//    options.encoding = "UTF-8"
//}
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
tasks.withType<Test> {
    useJUnitPlatform()
}
