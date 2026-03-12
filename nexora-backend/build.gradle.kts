plugins {
	java
	id("org.springframework.boot") version "3.3.4"
	id("io.spring.dependency-management") version "1.1.6"
	jacoco
}

group = "com.nexora"
version = "2.0.0-SNAPSHOT"

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
val jjwtVersion = "0.12.6"

dependencies {
	// Spring Boot Starters
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-security")

	// Database
	runtimeOnly("org.postgresql:postgresql")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")

	// JWT
	implementation("io.jsonwebtoken:jjwt-api:${jjwtVersion}")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:${jjwtVersion}")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:${jjwtVersion}")

	// OpenAPI / Swagger UI
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

	// Security utils (BCrypt sem Spring Security completo ainda)
	implementation("org.springframework.security:spring-security-crypto")

	// Testing
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
	testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testRuntimeOnly("com.h2database:h2")
}

tasks.withType<Test> {
	useJUnitPlatform()
	environment("SPRING_PROFILES_ACTIVE", "test")
	testLogging {
		events("passed", "skipped", "failed")
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
	}
	finalizedBy(tasks.jacocoTestReport)
}

jacoco {
	toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required = true
		html.required = true
		html.outputLocation = layout.buildDirectory.dir("reports/jacoco")
	}
	// Exclui classes geradas e infraestrutura do relatório
	classDirectories.setFrom(
		files(classDirectories.files.map {
			fileTree(it) {
				exclude(
					"**/infrastructure/persistence/entity/**",
					"**/NexoraApplication*",
					"**/config/**"
				)
			}
		})
	)
}

// Melhoria: Virtual Threads (Java 21 - Loom) ativado via configuração do Spring
// Ver application.yml: spring.threads.virtual.enabled=true