# Etapa 1 — Build da aplicação
FROM gradle:8.7-jdk21 AS build

WORKDIR /app

# Copia arquivos do gradle primeiro (melhora cache)
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle

# Baixa dependências antes de copiar o código
RUN gradle build -x test || true

# Copia o código da aplicação
COPY src ./src

# Build final
RUN gradle clean bootJar

# Etapa 2 — Imagem final mais leve
FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
