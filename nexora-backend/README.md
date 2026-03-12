# Nexora Backend

> Sistema de gestão de loja — backend principal em Java/Spring Boot  
> Projeto de portfólio desenvolvido por **Renan Pires**

---

## 🏗️ Arquitetura

O projeto segue **Arquitetura Hexagonal (Ports and Adapters)** com separação estrita de camadas:

```
com.nexora/
├── domain/              # Núcleo — zero dependências de framework
│   ├── model/           # Entidades e Value Objects
│   ├── repository/      # Output Ports (interfaces)
│   └── exception/       # Exceções do domínio (hierarquia sealed)
│
├── application/         # Casos de uso — orquestra o domínio
│   ├── usecase/         # Input Ports (interfaces)
│   ├── service/         # Implementações dos casos de uso
│   └── dto/             # Data Transfer Objects (Java Records)
│
├── infrastructure/      # Configurações e entidades JPA
│   ├── persistence/     # Entidades @Entity (isoladas)
│   └── config/          # Beans Spring (OpenAPI, Security Crypto)
│
├── adapter/             # Adaptadores de entrada e saída
│   ├── input/rest/      # Controllers REST
│   └── output/persistence/ # Implementações JPA dos repositórios
│
└── shared/              # Utilitários transversais
```

## 🚀 Melhorias Implementadas

| Melhoria | Descrição |
|----------|-----------|
| **Value Objects** | `Money` e `StockQuantity` garantem invariantes no domínio |
| **Sealed Exceptions** | Hierarquia de exceções com `sealed class` (Java 17+) |
| **Java Records** | DTOs imutáveis com boilerplate zero |
| **Virtual Threads** | `spring.threads.virtual.enabled=true` (Java 21/Loom) |
| **RFC 7807** | `ProblemDetail` nativo do Spring 6 para erros padronizados |
| **OpenAPI 3** | Swagger UI disponível desde a Fase 1 |
| **BCrypt** | Hash de senhas com `spring-security-crypto` (sem Spring Security completo) |
| **Soft Delete** | Deleção lógica em produtos e usuários |
| **Testcontainers** | Testes de integração com PostgreSQL real |
| **Perfis YAML** | Separação explícita de configurações por ambiente |

---

## ⚙️ Pré-requisitos

- **Java 21+**
- **PostgreSQL 16+** (ou Docker)
- **Gradle 8.10+** (ou use o wrapper `./gradlew`)

---

## 🏃 Executando o projeto

### 1. Suba o banco de dados

```bash
# Com Docker (recomendado)
docker-compose up -d postgres

# Ou configure um PostgreSQL local com:
# database: nexora | user: nexora | password: nexora | port: 5432
```

### 2. Execute o backend

```bash
# Compilar
./gradlew build

# Executar testes
./gradlew test

# Executar testes de integração (requer Docker)
./gradlew test --tests "*IntegrationTest"

# Iniciar a aplicação
./gradlew bootRun
```

### 3. Acesse a API

| URL | Descrição |
|-----|-----------|
| `http://localhost:8080/swagger-ui.html` | Documentação interativa |
| `http://localhost:8080/api/v1/products` | API de produtos |
| `http://localhost:8080/api/v1/users` | API de usuários |
| `http://localhost:8080/actuator/health` | Health check |

---

## 📋 Endpoints

### Produtos `GET /api/v1/products`

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/v1/products` | Criar produto |
| `GET` | `/api/v1/products` | Listar todos (`?activeOnly=true`) |
| `GET` | `/api/v1/products/{id}` | Buscar por ID |
| `PUT` | `/api/v1/products/{id}` | Atualizar |
| `PATCH` | `/api/v1/products/{id}/stock/replenish?quantity=N` | Repor estoque |
| `DELETE` | `/api/v1/products/{id}` | Desativar (soft delete) |

### Usuários `GET /api/v1/users`

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/v1/users` | Criar usuário |
| `GET` | `/api/v1/users` | Listar todos (`?role=ADMIN`) |
| `GET` | `/api/v1/users/{id}` | Buscar por ID |
| `PUT` | `/api/v1/users/{id}` | Atualizar perfil |
| `PATCH` | `/api/v1/users/{id}/role?role=MANAGER` | Alterar papel |
| `DELETE` | `/api/v1/users/{id}` | Desativar (soft delete) |

---

## 🗂️ Fases do Projeto

| Fase | Status | Descrição |
|------|--------|-----------|
| **Fase 1** | ✅ Concluída | Backend executável, arquitetura hexagonal, produtos, usuários |
| **Fase 2** | 🔜 Planejada | Pedidos, autenticação JWT, controle de acesso por papel |
| **Fase 3** | 🔜 Planejada | Docker completo, Redis (cache), Kafka (eventos) |

---

## 🔑 Dados de Seed (desenvolvimento)

| Email | Senha | Papel |
|-------|-------|-------|
| `admin@nexora.com` | `admin@123` | ADMIN |
| `gerente@nexora.com` | `admin@123` | MANAGER |

---

## 🧪 Estratégia de Testes

```
Testes de Domínio (puro JUnit)
  └── MoneyTest, ProductTest — zero framework, execução instantânea

Testes de Aplicação (Mockito)
  └── ProductApplicationServiceTest — lógica de orquestração isolada

Testes de Controller (Spring @WebMvcTest)
  └── ProductControllerTest — camada web sem banco de dados

Testes de Integração (Testcontainers)
  └── ProductIntegrationTest — stack completo com PostgreSQL real
```