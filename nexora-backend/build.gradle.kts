plugins {
	java
	id("org.springframework.boot") version "3.3.4"
	id("io.spring.dependency-management") version "1.1.6"
}

group = "com.nexora"
version = "1.0.0-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

val testcontainersVersion = "1.20.1"
val springdocVersion = "2.6.0"

dependencies {
	// Spring Boot Starters
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	// Database
	runtimeOnly("org.postgresql:postgresql")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")

	// OpenAPI / Swagger UI
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

	// Security utils (BCrypt sem Spring Security completo ainda)
	implementation("org.springframework.security:spring-security-crypto")

	// Testing
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
	testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")

	// H2 para testes de slice (@DataJpaTest sem Testcontainers)
	testRuntimeOnly("com.h2database:h2")
}

tasks.withType<Test> {
	useJUnitPlatform()
	environment("SPRING_PROFILES_ACTIVE", "test")
	testLogging {
		events("passed", "skipped", "failed")
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
	}
}

// Melhoria: Virtual Threads (Java 21 - Loom) ativado via configuração do Spring
// Ver application.yml: spring.threads.virtual.enabled=true