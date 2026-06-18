plugins {
    java
    id("org.springframework.boot") version "3.5.12"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "io.github.qifan777"
version = "1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}


dependencies {
    implementation(platform("org.springframework.ai:spring-ai-bom:1.1.2"))
    implementation("org.xerial:sqlite-jdbc")
    implementation("org.springframework.ai:spring-ai-starter-vector-store-pgvector")
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    implementation("com.alibaba.cloud.ai:spring-ai-alibaba-graph-core:1.1.2.2")
    implementation("io.github.a2asdk:a2a-java-sdk-transport-jsonrpc:0.3.2.Final")
    implementation("io.projectreactor.netty:reactor-netty")
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:3.5.12")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.postgresql:postgresql")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}
