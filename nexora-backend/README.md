<div align="center">

# Nexora Backend

**Sistema de Gestão de Loja — Backend Principal**

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.4-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![Kafka](https://img.shields.io/badge/Kafka-7.6-231F20?logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![Gradle](https://img.shields.io/badge/Gradle-8.10-02303A?logo=gradle&logoColor=white)](https://gradle.org/)

*Projeto de portfólio desenvolvido por **Renan Pires***

</div>

---

## Sumário

- [Visão Geral](#-visão-geral)
- [Arquitetura](#-arquitetura)
- [Stack Tecnológica](#-stack-tecnológica)
- [Funcionalidades por Fase](#-funcionalidades-por-fase)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Pré-requisitos](#-pré-requisitos)
- [Instalação e Execução](#-instalação-e-execução)
- [Endpoints da API](#-endpoints-da-api)
- [Autenticação e Autorização](#-autenticação-e-autorização)
- [Eventos de Domínio (Kafka)](#-eventos-de-domínio-kafka)
- [Cache (Redis)](#-cache-redis)
- [Migrações de Banco de Dados](#-migrações-de-banco-de-dados)
- [Estratégia de Testes](#-estratégia-de-testes)
- [Dados de Seed](#-dados-de-seed)
- [Variáveis de Configuração](#-variáveis-de-configuração)
- [Docker Compose](#-docker-compose)
- [Decisões de Design](#-decisões-de-design)

---

## 📌 Visão Geral

O **Nexora Backend** é uma API RESTful para gerenciamento completo de uma loja, cobrindo produtos, categorias, pedidos, controle de estoque e usuários com papéis hierárquicos. O projeto foi desenvolvido em fases incrementais como portfólio de engenharia de software, demonstrando boas práticas que vão de modelagem de domínio rico até sistemas orientados a eventos.

**Destaques técnicos:**
- Arquitetura Hexagonal (Ports & Adapters) rigorosamente aplicada
- Domínio rico com Value Objects, máquina de estados e invariantes garantidas por design
- JWT stateless com access token (15 min) + refresh token (7 dias)
- Cache Redis com TTLs por recurso via `@Cacheable` / `@CacheEvict`
- Eventos de domínio imutáveis publicados no Kafka com roteamento por tipo
- Transactional Outbox Pattern para garantia de entrega eventual
- 4 níveis de teste: domínio puro → Mockito → `@WebMvcTest` → Testcontainers
- **87 classes de produção · 11 classes de teste · 9 migrações Flyway**

---

## 🏗 Arquitetura

O projeto segue a **Arquitetura Hexagonal** (*Ports and Adapters*), garantindo que o núcleo de domínio não tenha nenhuma dependência de framework, banco de dados ou infraestrutura externa.

```
┌─────────────────────────────────────────────────────────────────┐
│                      ADAPTERS DE ENTRADA                        │
│        REST Controllers · GlobalExceptionHandler                │
└──────────────────────────┬──────────────────────────────────────┘
                           │ chama Input Ports (UseCase interfaces)
┌──────────────────────────▼──────────────────────────────────────┐
│                      APPLICATION LAYER                          │
│   AuthUseCase · ProductUseCase · OrderUseCase · CategoryUseCase │
│   Services: orquestram domínio, publicam eventos, gerenciam cache│
└────────────┬──────────────────────────────────┬─────────────────┘
             │ usa domain models                 │ chama Output Ports
┌────────────▼──────────────────┐  ┌─────────────▼────────────────┐
│         DOMAIN LAYER          │  │     ADAPTERS DE SAÍDA        │
│  Product · Order · User       │  │  JPA Persistence Adapters    │
│  Category · StockMovement     │  │  KafkaEventPublisher         │
│  Money · StockQuantity        │  │  Redis Cache (Spring Cache)  │
│  Domain Events (records)      │  └──────────────────────────────┘
│  Repositórios (interfaces)    │
│  EventPublisher (interface)   │
└───────────────────────────────┘
```

### Fluxo de uma Requisição

```
HTTP Request
  → JwtAuthenticationFilter    (valida Bearer token, popula SecurityContext)
  → SecurityFilterChain        (verifica autorização por rota e papel)
  → Controller                 (deserialização + @Valid)
  → UseCase / Service          (orquestração + regras de negócio)
  → Domain Model               (invariantes do domínio)
  → Repository Port            (persistência via JPA)
  → EventPublisher Port        (publicação Kafka)
  → Cache Eviction/Population  (Redis @CacheEvict / @Cacheable)
  ← HTTP Response              (DTO serializado)
```

---

## 🛠 Stack Tecnológica

| Categoria | Tecnologia | Versão | Propósito |
|-----------|-----------|--------|-----------|
| Linguagem | Java | 21 | Virtual Threads, Records, Sealed Classes, Pattern Matching |
| Framework | Spring Boot | 3.3.4 | Container IoC, MVC, Security, Data |
| Segurança | Spring Security + JJWT | 6.x / 0.12.6 | Autenticação JWT stateless, RBAC |
| Banco | PostgreSQL | 16 | Persistência principal |
| ORM | Spring Data JPA / Hibernate | 6.x | Mapeamento objeto-relacional |
| Migrações | Flyway | 10.x | Controle de versão do schema |
| Cache | Redis + Spring Cache | 7.x | Cache de produtos e categorias |
| Mensageria | Kafka + Spring Kafka | 7.6.1 | Eventos de domínio assíncronos |
| Documentação | SpringDoc OpenAPI 3 | 2.6.0 | Swagger UI interativo |
| Build | Gradle (Kotlin DSL) | 8.10 | Build e gerenciamento de dependências |
| Testes | JUnit 5, Mockito, Testcontainers | — | 4 níveis de teste |
| Cobertura | JaCoCo | 0.8.12 | Relatórios HTML e XML |
| Container | Docker + Docker Compose v2 | — | Ambiente local e stack completa |

---

## 📦 Funcionalidades por Fase

### Fase 1 — Fundação ✅

Fundação do projeto com arquitetura hexagonal e as entidades centrais do sistema.

- **Arquitetura Hexagonal** rigorosa: domínio sem dependências de framework
- **Value Objects** `Money` (invariante de valor não-negativo, normalização de escala 2 casas) e `StockQuantity` (garante estoque ≥ 0 com operações add/subtract seguras)
- **CRUD completo de Produtos** — nome, descrição, SKU único, preço, estoque, soft delete
- **CRUD completo de Usuários** — hierarquia de papéis `CUSTOMER → SELLER → MANAGER → ADMIN`
- **Senhas com BCrypt** (strength 12) — nunca armazenadas em texto plano
- **Flyway** — migrações versionadas com seeds de desenvolvimento
- **Swagger UI** — documentação interativa desde o primeiro endpoint
- **RFC 7807 ProblemDetail** — respostas de erro padronizadas (`application/problem+json`)
- **Java Records** para todos os DTOs — boilerplate zero, imutabilidade garantida
- **Virtual Threads** (Java 21 / Project Loom) — `spring.threads.virtual.enabled=true`
- **Sealed Class** para hierarquia de exceções de domínio tipadas

---

### Fase 2 — Segurança e Pedidos ✅

Autenticação completa com JWT, ciclo de vida de pedidos e rastreamento de estoque.

- **Spring Security + JWT** stateless
  - Access Token: 15 minutos (HMAC-SHA256)
  - Refresh Token: 7 dias
  - `JwtAuthenticationFilter` popula `SecurityContext` a partir do Bearer token
- **Aggregate `Order`** com máquina de estados embutida no enum `OrderStatus`:
  ```
  PENDING → CONFIRMED → SHIPPED → DELIVERED
  PENDING → CANCELLED
  CONFIRMED → CANCELLED
  ```
- **Sincronização automática de estoque**: confirmação decrementa, cancelamento (se `CONFIRMED` ou `SHIPPED`) restaura automaticamente
- **`StockMovement`** — entidade imutável de auditoria de toda movimentação (tipo, quantidade antes/depois, motivo, referência, executor)
- **`Category`** — catálogo hierárquico; produtos podem ser atribuídos a categorias
- **Paginação** em todos os endpoints de listagem (`Pageable`, `Page<T>`)
- **RBAC** — controle de acesso por papel por rota (`SecurityConfig`) e por método (`@PreAuthorize`)
- **`@CurrentUser`** — meta-anotação que injeta o UUID do usuário logado diretamente nos parâmetros do controller, sem `SecurityContextHolder`

---

### Fase 3 — Eventos e Cache ✅ *(atual)*

Arquitetura orientada a eventos com Kafka e cache distribuído com Redis.

- **Eventos de Domínio** imutáveis (`record` Java) no pacote `domain/event`:
  - `OrderConfirmedEvent` — snapshot completo com itens e total
  - `OrderCancelledEvent` — motivo e ID do executor
  - `StockReplenishedEvent` — produto, quantidade e estoque resultante
  - `UserRegisteredEvent` — email e papel do novo usuário
- **`EventPublisher`** — Output Port no domínio. Implementação Kafka na infraestrutura; bean no-op (`@ConditionalOnMissingBean`) habilita testes sem Kafka, sem configuração adicional
- **`KafkaEventPublisher`** — roteamento automático por prefixo de tipo (`order.*` → `nexora.orders`, `stock.*` → `nexora.stock`, `user.*` → `nexora.users`); chave de partição = `aggregateId` (garante ordenação causal por entidade)
- **Cache Redis** com TTLs distintos por cache (`CacheConfig`):
  - `products`: 5 minutos
  - `categories`: 30 minutos
  - `stock`: 1 minuto
- **`@Cacheable` / `@CacheEvict`** aplicados em todas as operações de leitura e escrita
- **`KafkaConfig`** — cria tópicos Kafka automaticamente na inicialização (`@ConditionalOnProperty`)
- **V9 Migration** — tabela `domain_events_outbox` (Transactional Outbox Pattern) + `revoked_tokens` (controle de invalidação de JWT)
- **`docker-compose.yml`** completo: PostgreSQL, Redis, Kafka + Zookeeper, Kafka UI, Prometheus, Grafana

---

## 🗂 Estrutura do Projeto

```
nexora-backend/
│
├── build.gradle.kts                    # Gradle Kotlin DSL — deps, JaCoCo, toolchain Java 21
├── docker-compose.yml                  # Stack completa (PostgreSQL · Redis · Kafka · UI · Monitoring)
│
└── src/
    ├── main/
    │   ├── java/com/nexora/
    │   │   ├── NexoraApplication.java              # Entry point + @EnableCaching
    │   │   │
    │   │   ├── domain/                             # ─── NÚCLEO (zero framework) ───
    │   │   │   ├── model/
    │   │   │   │   ├── Money.java                  # VO: valor monetário + moeda + escala
    │   │   │   │   ├── StockQuantity.java           # VO: estoque ≥ 0, add/subtract seguros
    │   │   │   │   ├── Product.java                # Entidade: preço, estoque, categoria
    │   │   │   │   ├── Category.java               # Entidade: catálogo ativo/inativo
    │   │   │   │   ├── Order.java                  # Aggregate Root: máquina de estados
    │   │   │   │   ├── OrderItem.java              # VO: snapshot imutável (preço no pedido)
    │   │   │   │   ├── OrderStatus.java            # Enum: allowedTransitions() embutido
    │   │   │   │   ├── StockMovement.java          # Entidade imutável de auditoria
    │   │   │   │   ├── User.java                   # Entidade: email validado, hash de senha
    │   │   │   │   └── UserRole.java               # Enum: hierarquia + hasPermissionOf()
    │   │   │   ├── event/                          # Eventos de domínio (Phase 3)
    │   │   │   │   ├── DomainEvent.java            # Interface: eventId · eventType · aggregateId
    │   │   │   │   ├── OrderConfirmedEvent.java    # Snapshot completo do pedido confirmado
    │   │   │   │   ├── OrderCancelledEvent.java
    │   │   │   │   ├── StockReplenishedEvent.java
    │   │   │   │   └── UserRegisteredEvent.java
    │   │   │   ├── port/
    │   │   │   │   └── EventPublisher.java         # Output Port para publicação de eventos
    │   │   │   ├── repository/                     # Output Ports de persistência
    │   │   │   │   ├── ProductRepository.java
    │   │   │   │   ├── OrderRepository.java
    │   │   │   │   ├── CategoryRepository.java
    │   │   │   │   ├── StockMovementRepository.java
    │   │   │   │   └── UserRepository.java
    │   │   │   └── exception/
    │   │   │       ├── DomainException.java        # sealed class base
    │   │   │       ├── ResourceNotFoundException.java
    │   │   │       ├── DuplicateResourceException.java
    │   │   │       └── BusinessRuleException.java
    │   │   │
    │   │   ├── application/                        # ─── CASOS DE USO ───
    │   │   │   ├── usecase/                        # Input Ports (interfaces)
    │   │   │   │   ├── AuthUseCase.java
    │   │   │   │   ├── ProductUseCase.java
    │   │   │   │   ├── OrderUseCase.java
    │   │   │   │   ├── CategoryUseCase.java
    │   │   │   │   └── UserUseCase.java
    │   │   │   ├── service/                        # Implementações dos Input Ports
    │   │   │   │   ├── AuthApplicationService.java
    │   │   │   │   ├── ProductApplicationService.java  # @Cacheable + @CacheEvict + eventos
    │   │   │   │   ├── OrderApplicationService.java    # Ciclo de vida + estoque + eventos
    │   │   │   │   ├── CategoryApplicationService.java # @Cacheable + @CacheEvict
    │   │   │   │   └── UserApplicationService.java     # BCrypt + UserRegisteredEvent
    │   │   │   └── dto/                            # Java Records por aggregate
    │   │   │       ├── auth/     LoginRequest · AuthResponse · RefreshTokenRequest
    │   │   │       ├── product/  CreateProductRequest · UpdateProductRequest · ProductResponse
    │   │   │       ├── order/    CreateOrderRequest · OrderItemRequest · OrderResponse · OrderItemResponse
    │   │   │       ├── category/ CreateCategoryRequest · CategoryResponse
    │   │   │       ├── stock/    StockMovementResponse
    │   │   │       └── user/     CreateUserRequest · UpdateUserRequest · UserResponse
    │   │   │
    │   │   ├── infrastructure/                     # ─── INFRAESTRUTURA ───
    │   │   │   ├── config/
    │   │   │   │   ├── SecurityConfig.java         # FilterChain · CORS · stateless · @EnableMethodSecurity
    │   │   │   │   ├── CacheConfig.java            # RedisCacheManager com TTLs por cache
    │   │   │   │   ├── KafkaConfig.java            # NewTopic beans (@ConditionalOnProperty)
    │   │   │   │   ├── EventPublisherConfig.java   # No-op fallback (@ConditionalOnMissingBean)
    │   │   │   │   ├── NexoraProperties.java       # @ConfigurationProperties prefix="nexora"
    │   │   │   │   └── OpenApiConfig.java          # Bearer auth global + info do projeto
    │   │   │   ├── security/
    │   │   │   │   ├── JwtTokenProvider.java       # Geração/validação HMAC-SHA256
    │   │   │   │   ├── JwtAuthenticationFilter.java# OncePerRequestFilter
    │   │   │   │   ├── JwtProperties.java          # @ConfigurationProperties prefix="nexora.jwt"
    │   │   │   │   ├── NexoraUserDetails.java      # Adapta User → UserDetails
    │   │   │   │   └── NexoraUserDetailsService.java
    │   │   │   ├── messaging/
    │   │   │   │   └── KafkaEventPublisher.java    # @ConditionalOnBean(KafkaTemplate.class)
    │   │   │   └── persistence/entity/
    │   │   │       ├── ProductEntity.java
    │   │   │       ├── OrderEntity.java            # @ElementCollection para itens
    │   │   │       ├── OrderItemEmbeddable.java    # @Embeddable
    │   │   │       ├── CategoryEntity.java
    │   │   │       ├── StockMovementEntity.java
    │   │   │       └── UserEntity.java
    │   │   │
    │   │   ├── adapter/                            # ─── ADAPTADORES ───
    │   │   │   ├── input/rest/
    │   │   │   │   ├── AuthController.java
    │   │   │   │   ├── ProductController.java      # Paginação · replenish · categoria
    │   │   │   │   ├── OrderController.java        # Ciclo de vida completo
    │   │   │   │   ├── CategoryController.java
    │   │   │   │   ├── UserController.java
    │   │   │   │   └── GlobalExceptionHandler.java # @RestControllerAdvice → ProblemDetail
    │   │   │   └── output/persistence/
    │   │   │       ├── {Product,Order,Category,StockMovement,User}PersistenceAdapter.java
    │   │   │       └── jpa/  {Product,Order,Category,StockMovement,User}JpaRepository.java
    │   │   │
    │   │   └── shared/security/
    │   │       └── CurrentUser.java                # Meta-anotação @AuthenticationPrincipal UUID
    │   │
    │   └── resources/
    │       ├── application.yml                     # Base + perfis: local, default
    │       └── db/migration/
    │           ├── V1__create_users_table.sql
    │           ├── V2__create_products_table.sql
    │           ├── V7__phase3_kafka_outbox.sql
    │           ├── V3__create_categories_table.sql
    │           ├── V4__add_category_to_products.sql
    │           ├── V5__create_stock_movements_table.sql
    │           ├── V6__create_orders_tables.sql
    │           ├── V8__register_and_performace_indeexes.sql
    │           └── V9__seed_initial_data.sql
    │
    └── test/
        ├── java/com/nexora/
        │   ├── NexoraApplicationTests.java             # Smoke test: contexto sobe sem erros
        │   ├── domain/model/
        │   │   ├── MoneyTest.java                      # Puro JUnit: invariantes do VO
        │   │   ├── ProductTest.java                    # Puro JUnit: comportamentos da entidade
        │   │   ├── OrderStatusTest.java                # Puro JUnit: todas as transições (parameterized)
        │   │   └── OrderTest.java                      # Puro JUnit: aggregate root completo
        │   ├── application/service/
        │   │   ├── OrderApplicationServiceTest.java    # Mockito: ciclo de vida + estoque + eventos
        │   │   └── ProductApplicationServiceTest.java  # Mockito: CRUD + cache + eventos
        │   ├── adapter/input/rest/
        │   │   └── ProductControllerTest.java          # @WebMvcTest + @WithMockUser
        │   ├── security/
        │   │   └── JwtTokenProviderTest.java           # Geração, validação, tamper, expiração
        │   └── integration/
        │       ├── ProductIntegrationTest.java         # Testcontainers: E2E com PostgreSQL real
        │       └── AuthOrderIntegrationTest.java       # Testcontainers: auth → pedido → confirmação
        └── resources/
            └── application.yml                         # H2 · Flyway off · Kafka/Redis desabilitados
```

---

## ⚙ Pré-requisitos

| Requisito | Versão Mínima | Notas |
|-----------|--------------|-------|
| Java (JDK) | 21 | Necessário para Virtual Threads e `record` |
| Docker | 24+ | Para PostgreSQL, Redis e Kafka em containers |
| Docker Compose | v2+ | Integrado ao Docker CLI como `docker compose` |

> O projeto usa o **Gradle Wrapper** (`./gradlew`) — não é necessário instalar Gradle separadamente.

---

## 🚀 Instalação e Execução

### 1. Clone o repositório

```bash
git clone https://github.com/renan-pires/nexora-backend.git
cd nexora-backend
```

### 2. Suba a infraestrutura

Escolha o nível de infraestrutura desejado:

```bash
# Apenas banco de dados (desenvolvimento mínimo — sem cache/eventos)
docker-compose up -d postgres

# Stack completa da Fase 3 (recomendado)
docker-compose up -d postgres redis zookeeper kafka

# Stack completa + Kafka UI (inspecionar tópicos via browser)
docker-compose --profile tools up -d

# Stack completa + Prometheus + Grafana
docker-compose --profile monitoring up -d
```

Aguarde os serviços ficarem saudáveis:
```bash
docker-compose ps
# Todos devem mostrar "running" ou "healthy"
```

### 3. Execute a aplicação

```bash
# Perfil local — DEBUG habilitado, logs detalhados de SQL e Security
./gradlew bootRun --args='--spring.profiles.active=local'

# Sem perfil — configurações padrão
./gradlew bootRun
```

### 4. Acesse os serviços

| URL | Serviço |
|-----|---------|
| `http://localhost:8080/swagger-ui.html` | Swagger UI interativo |
| `http://localhost:8080/api-docs` | OpenAPI 3 JSON spec |
| `http://localhost:8080/actuator/health` | Health check |
| `http://localhost:8080/actuator/caches` | Estado dos caches Redis |
| `http://localhost:8090` | Kafka UI *(requer `--profile tools`)* |
| `http://localhost:9090` | Prometheus *(requer `--profile monitoring`)* |
| `http://localhost:3001` | Grafana — admin/nexora *(requer `--profile monitoring`)* |

---

## 🧪 Rodando os Testes

```bash
# Todos os testes — unitários + integração
# Requer Docker para os testes de integração (Testcontainers)
./gradlew test

# Apenas testes unitários (sem Docker necessário)
./gradlew test --tests "com.nexora.domain.*"
./gradlew test --tests "com.nexora.application.*"
./gradlew test --tests "com.nexora.adapter.*"
./gradlew test --tests "com.nexora.security.*"

# Apenas testes de integração (Docker obrigatório)
./gradlew test --tests "com.nexora.integration.*"

# Gerar relatório de cobertura
# Resultado em: build/reports/jacoco/index.html
./gradlew jacocoTestReport

# Build completo (compilar + testar + empacotar)
./gradlew build
```

> **Sobre Testcontainers**: os testes em `integration/` sobem automaticamente um container `postgres:16-alpine` isolado via Docker. O `@DynamicPropertySource` sobrescreve o datasource, re-habilita o Flyway e injeta o JWT secret sem afetar outros testes. Kafka e Redis permanecem desabilitados via `application.yml` de teste.

---

## 📡 Endpoints da API

### Autenticação — `/api/v1/auth`

| Método | Endpoint | Acesso | Descrição |
|--------|----------|--------|-----------|
| `POST` | `/api/v1/auth/register` | **Público** | **Auto-cadastro de cliente** — cria conta CUSTOMER + retorna JWT imediato |
| `POST` | `/api/v1/auth/login` | **Público** | Login com email/senha → access + refresh token |
| `POST` | `/api/v1/auth/refresh` | **Público** | Renova o access token usando o refresh token |
| `PATCH` | `/api/v1/auth/me/password` | Autenticado | Troca de senha — exige a senha atual para confirmar identidade |
| `GET` | `/api/v1/auth/me` | Autenticado | Dados do perfil do usuário logado |

> **Regra de papel:** `POST /auth/register` sempre cria usuários com papel `CUSTOMER`.
> Para criar funcionários com papéis elevados (`SELLER`, `MANAGER`, `ADMIN`), use
> `POST /api/v1/users` com credenciais de `MANAGER` ou `ADMIN`.

**Exemplo — auto-cadastro público:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "email": "joao@email.com",
    "password": "senha123"
  }'
```

**Resposta (201 Created):**
```json
{
  "accessToken":  "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType":    "Bearer",
  "expiresIn":    900,
  "userId":       "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "email":        "joao@email.com",
  "role":         "CUSTOMER"
}
```

**Exemplo — troca de senha:**
```bash
curl -X PATCH http://localhost:8080/api/v1/auth/me/password \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"currentPassword": "senha123", "newPassword": "novaSenha456"}'
# Retorna 204 No Content
```

---

### Produtos — `/api/v1/products`

| Método | Endpoint | Acesso | Descrição |
|--------|----------|--------|-----------|
| `GET` | `/api/v1/products` | **Público** | Listar (paginado). Params: `?activeOnly=true`, `?categoryId={uuid}`, `?page=0&size=20&sort=name` |
| `GET` | `/api/v1/products/{id}` | **Público** | Buscar por ID |
| `POST` | `/api/v1/products` | `SELLER+` | Criar produto com estoque inicial |
| `PUT` | `/api/v1/products/{id}` | `SELLER+` | Atualizar nome, descrição e preço |
| `PATCH` | `/api/v1/products/{id}/category/{catId}` | `SELLER+` | Atribuir categoria |
| `PATCH` | `/api/v1/products/{id}/stock/replenish?quantity=N` | `SELLER+` | Repor estoque (registra `StockMovement`) |
| `DELETE` | `/api/v1/products/{id}` | `SELLER+` | Desativar (soft delete) |

**Exemplo — criar produto:**
```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Notebook Pro X1",
    "description": "Notebook para desenvolvimento",
    "sku": "NB-PRO-X1",
    "price": 5999.99,
    "currency": "BRL",
    "initialStock": 15,
    "categoryId": null
  }'
```

---

### Categorias — `/api/v1/categories`

| Método | Endpoint | Acesso | Descrição |
|--------|----------|--------|-----------|
| `GET` | `/api/v1/categories` | **Público** | Listar todas as categorias |
| `GET` | `/api/v1/categories/{id}` | **Público** | Buscar por ID |
| `POST` | `/api/v1/categories` | `MANAGER+` | Criar categoria |
| `PUT` | `/api/v1/categories/{id}` | `MANAGER+` | Atualizar |
| `DELETE` | `/api/v1/categories/{id}` | `MANAGER+` | Desativar (soft delete) |

---

### Pedidos — `/api/v1/orders`

| Método | Endpoint | Acesso | Descrição |
|--------|----------|--------|-----------|
| `POST` | `/api/v1/orders` | Autenticado | Criar pedido (`status: PENDING`) |
| `GET` | `/api/v1/orders/my` | Autenticado | Meus pedidos (paginado) |
| `GET` | `/api/v1/orders/{id}` | Autenticado * | Buscar por ID |
| `GET` | `/api/v1/orders` | `SELLER+` | Listar todos os pedidos (paginado). Filtro: `?status=PENDING` |
| `POST` | `/api/v1/orders/{id}/confirm` | `SELLER+` | Confirmar → decrementa estoque + evento |
| `POST` | `/api/v1/orders/{id}/ship` | `SELLER+` | Marcar como enviado |
| `POST` | `/api/v1/orders/{id}/deliver` | `SELLER+` | Marcar como entregue |
| `POST` | `/api/v1/orders/{id}/cancel?reason=...` | Autenticado ** | Cancelar |

> \* Cliente visualiza apenas seus próprios pedidos; funcionários visualizam qualquer pedido.  
> \*\* Cliente cancela apenas seus próprios pedidos; funcionários cancelam qualquer pedido.

**Máquina de estados:**
```
  ┌──────────────────────────────────────────────┐
  │                                              ▼
PENDING ──► CONFIRMED ──► SHIPPED ──► DELIVERED  CANCELLED
  │              │                               ▲
  └──────────────┴───────────────────────────────┘
```

**Exemplo — criar pedido:**
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"productId": "b0000000-0000-0000-0000-000000000001", "quantity": 2}
    ],
    "notes": "Entregar pela manhã"
  }'
```

### Estoque — `/api/v1/stock`

| Método | Endpoint | Acesso | Descrição |
|--------|----------|--------|-----------|
| `GET` | `/api/v1/stock/{productId}/movements` | `SELLER+` | Histórico de movimentações de um produto (paginado, mais recentes primeiro) |
| `GET` | `/api/v1/stock/movements` | `MANAGER+` | Histórico global de todas as movimentações |

> Escrita de estoque (reabastecimento) permanece em `PATCH /api/v1/products/{id}/stock/replenish`.

---

| Método | Endpoint | Acesso | Descrição |
|--------|----------|--------|-----------|
| `POST` | `/api/v1/users` | `MANAGER+` | Criar usuário com papel explícito (SELLER, MANAGER, ADMIN) |
| `GET` | `/api/v1/users` | `MANAGER+` | Listar todos (`?role=CUSTOMER`) |
| `GET` | `/api/v1/users/{id}` | `MANAGER+` | Buscar por ID |
| `PUT` | `/api/v1/users/{id}` | `MANAGER+` | Atualizar nome e email |
| `PATCH` | `/api/v1/users/{id}/role?role=SELLER` | `MANAGER+` | Alterar papel |
| `DELETE` | `/api/v1/users/{id}` | `MANAGER+` | Desativar (soft delete) |
| `PATCH` | `/api/v1/users/{id}/activate` | `MANAGER+` | Reativar usuário desativado |

---

## 🔐 Autenticação e Autorização

### Hierarquia de Papéis

```
CUSTOMER  <  SELLER  <  MANAGER  <  ADMIN
```

Cada papel herda todas as permissões dos papéis abaixo. `UserRole.hasPermissionOf(role)` encapsula essa lógica no domínio.

### Como autenticar no Swagger UI

1. Acesse `http://localhost:8080/swagger-ui.html`
2. Expanda `POST /api/v1/auth/login` → clique em **Try it out**
3. Use `admin@nexora.com` / `admin@123`
4. Copie o `accessToken` da resposta
5. Clique no botão **Authorize 🔒** no topo da página
6. Cole o token no campo `bearerAuth` → **Authorize**

---

## 📨 Eventos de Domínio (Kafka)

A Fase 3 introduz eventos de domínio imutáveis publicados no Kafka após operações transacionais bem-sucedidas.

### Tópicos e Eventos

| Tópico | Evento | Publicado quando |
|--------|--------|-----------------|
| `nexora.orders` | `OrderConfirmedEvent` | Pedido confirmado (snapshot de itens + total) |
| `nexora.orders` | `OrderCancelledEvent` | Pedido cancelado (motivo + executor) |
| `nexora.stock` | `StockReplenishedEvent` | Estoque reabastecido manualmente |
| `nexora.users` | `UserRegisteredEvent` | Novo usuário criado |

### Estrutura de evento

```json
{
  "eventId":       "550e8400-e29b-41d4-a716-446655440000",
  "eventType":     "order.confirmed",
  "aggregateType": "Order",
  "aggregateId":   "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "occurredAt":    "2025-03-13T14:32:10.123Z",
  "orderId":       "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "customerId":    "a0000000-0000-0000-0000-000000000001",
  "total":         5999.98,
  "currency":      "BRL",
  "items": [
    { "productId": "...", "sku": "NB-PRO-001", "quantity": 2, "unitPrice": 2999.99 }
  ]
}
```

### Roteamento e ordenação

A chave da mensagem Kafka é o `aggregateId`. Isso garante que todos os eventos de um mesmo aggregate (pedido, produto, usuário) sempre vão para a mesma partição, preservando a ordem causal por entidade.

### Transactional Outbox Pattern

A tabela `domain_events_outbox` (V9) persiste eventos na **mesma transação** do aggregate, eliminando o problema de *dual write*. Um relay job (ou CDC com Debezium) lê os registros com `published_at IS NULL`, publica no Kafka e atualiza `published_at` após confirmação do broker.

### Fallback sem Kafka

O bean `KafkaEventPublisher` só é criado quando `KafkaTemplate` está disponível no contexto (`@ConditionalOnBean`). O `EventPublisherConfig` registra automaticamente um no-op fallback (`@ConditionalOnMissingBean`) que apenas loga os eventos descartados — nenhum erro é lançado, nenhuma configuração de teste é necessária.

---

## ⚡ Cache (Redis)

O cache é aplicado na camada de Application Service usando Spring Cache.

### Configuração por cache

| Cache | TTL | Chave | Invalidado em |
|-------|-----|-------|--------------|
| `products` | 5 min | ID do produto | `createProduct`, `updateProduct`, `replenishStock`, `deleteProduct`, `assignCategory` |
| `categories` | 30 min | ID da categoria / `'all'` | `create`, `update`, `delete` |
| `stock` | 1 min | ID do produto | Operações de estoque |

### Comportamento em diferentes contextos

| Contexto | Comportamento |
|----------|--------------|
| Produção/Dev (Redis ativo) | Cache funciona normalmente com TTLs configurados |
| Testes unitários (`spring.cache.type=none`) | `@Cacheable`/`@CacheEvict` são no-op — sem erro |
| Testes de integração (Testcontainers) | Kafka/Redis desabilitados; cache é no-op |

### Monitorar o cache

```bash
# Via Redis CLI
docker exec -it nexora-redis redis-cli
> KEYS *
> TTL "products::some-uuid"
> DBSIZE

# Via Spring Actuator
curl http://localhost:8080/actuator/caches
```

---

## 🗃 Migrações de Banco de Dados

O schema é gerenciado exclusivamente pelo **Flyway**. O Hibernate nunca altera o schema (`ddl-auto: validate`).

| Versão | Arquivo | Descrição |
|--------|---------|-----------|
| V1 | `V1__create_users_table.sql` | Tabela `users` — UUID PK, `role` VARCHAR, soft delete, índices explícitos |
| V2 | `V2__create_products_table.sql` | Tabela `products` — `price`, `currency`, `stock_quantity`, SKU único |
| V3 | `V7__phase3_kafka_outbox.sql` | Seeds: admin, gerente + 3 produtos iniciais |
| V4 | `V3__create_categories_table.sql` | Tabela `categories` — nome único, soft delete |
| V5 | `V4__add_category_to_products.sql` | FK `category_id` em `products` (nullable) |
| V6 | `V5__create_stock_movements_table.sql` | Auditoria imutável de movimentações de estoque |
| V7 | `V6__create_orders_tables.sql` | Tabelas `orders` + `order_items` (ElementCollection) |
| V8 | `V8__register_and_performace_indeexes.sql` | 4 categorias, `vendedor@nexora.com`, `cliente@nexora.com` |
| V9 | `V9__seed_initial_data.sql` | `domain_events_outbox` (Outbox Pattern) + `revoked_tokens` |

---

## 🧪 Estratégia de Testes

```
┌────────────────────────────────────────────────────────────────────┐
│ NÍVEL 1 — Testes de Domínio (Puro JUnit 5)                        │
│ Sem Spring · Sem Mockito · Execução < 100ms                        │
│                                                                    │
│  MoneyTest       → invariantes do Value Object                     │
│  ProductTest     → comportamentos da entidade                      │
│  OrderStatusTest → todas as transições (parameterized test)        │
│  OrderTest       → aggregate root: addItem, confirm, cancel, total │
├────────────────────────────────────────────────────────────────────┤
│ NÍVEL 2 — Testes de Application Service (Mockito)                  │
│ Sem Spring Context · dependências mockadas                         │
│                                                                    │
│  OrderApplicationServiceTest  → orquestração, estoque, eventos     │
│  ProductApplicationServiceTest → CRUD, cache, eventos              │
├────────────────────────────────────────────────────────────────────┤
│ NÍVEL 3 — Testes de Controller (@WebMvcTest)                       │
│ Apenas web layer · JPA/banco NÃO carregado                         │
│                                                                    │
│  ProductControllerTest → rotas, HTTP status, serialização,         │
│                          validação, @WithMockUser, segurança       │
│  JwtTokenProviderTest  → geração, validação, tamper, expiração     │
├────────────────────────────────────────────────────────────────────┤
│ NÍVEL 4 — Testes de Integração (Testcontainers)                    │
│ PostgreSQL real em container · stack completa · Flyway ativo       │
│                                                                    │
│  ProductIntegrationTest    → CRUD E2E com PostgreSQL real          │
│  AuthOrderIntegrationTest  → auth → produto → pedido → confirmação │
│                              + verificação de estoque decrementado │
└────────────────────────────────────────────────────────────────────┘
```

---

## 🔑 Dados de Seed

Usuários disponíveis após as migrações — todos com senha `admin@123`:

| Email | Papel | Migration |
|-------|-------|-----------|
| `admin@nexora.com` | `ADMIN` | V3 |
| `gerente@nexora.com` | `MANAGER` | V3 |
| `vendedor@nexora.com` | `SELLER` | V8 |
| `cliente@nexora.com` | `CUSTOMER` | V8 |

**Categorias pré-cadastradas (V8):** `Eletrônicos`, `Periféricos`, `Acessórios`, `Redes`

**Produtos pré-cadastrados (V3):** `Notebook Profissional` (R$ 4.999,99 · 15 un), `Mouse Ergonômico` (R$ 199,90 · 50 un), `Teclado Mecânico` (inativo)

---

## ⚙ Variáveis de Configuração

Configuradas em `application.yml`, sobrescrevíveis via variáveis de ambiente ou `--spring.config.additional-location`.

### Banco de Dados

| Propriedade | Padrão |
|------------|--------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/nexora` |
| `spring.datasource.username` | `nexora` |
| `spring.datasource.password` | `nexora` |

### JWT

| Propriedade | Padrão | Obs |
|------------|--------|-----|
| `nexora.jwt.secret` | `nexora-local-dev-secret...` | **Mínimo 256 bits em produção** |
| `nexora.jwt.access-token-expiration-ms` | `900000` | 15 minutos |
| `nexora.jwt.refresh-token-expiration-ms` | `604800000` | 7 dias |

> ⚠ **Produção**: gere o secret com `openssl rand -hex 32` e injete via variável de ambiente (`NEXORA_JWT_SECRET`). Nunca comite secrets reais no repositório.

### Redis

| Propriedade | Padrão |
|------------|--------|
| `spring.data.redis.host` | `localhost` |
| `spring.data.redis.port` | `6379` |
| `spring.cache.redis.time-to-live` | `300000` (5 min, TTL padrão) |

### Kafka

| Propriedade | Padrão |
|------------|--------|
| `spring.kafka.bootstrap-servers` | `localhost:9092` |
| `nexora.kafka.topics.orders` | `nexora.orders` |
| `nexora.kafka.topics.stock` | `nexora.stock` |
| `nexora.kafka.topics.users` | `nexora.users` |

---

## 🐳 Docker Compose

A stack completa está definida em `docker-compose.yml` com suporte a profiles.

### Serviços disponíveis

| Serviço | Porta | Profile | Descrição |
|---------|-------|---------|-----------|
| `postgres` | `5432` | Padrão | PostgreSQL 16-alpine com health check |
| `redis` | `6379` | Padrão | Redis 7-alpine, `maxmemory-policy allkeys-lru` |
| `zookeeper` | `2181` | Padrão | Requerido pelo Kafka |
| `kafka` | `9092` | Padrão | Confluent Kafka 7.6, retenção 7 dias |
| `kafka-ui` | `8090` | `tools` | Interface web para tópicos e mensagens |
| `prometheus` | `9090` | `monitoring` | Coleta métricas do `/actuator/prometheus` |
| `grafana` | `3001` | `monitoring` | Dashboards — credenciais: `admin` / `nexora` |

### Comandos de referência

```bash
# Subir apenas banco (dev mínimo)
docker-compose up -d postgres

# Stack fase 3 (postgres + redis + kafka)
docker-compose up -d

# Com Kafka UI
docker-compose --profile tools up -d

# Com monitoramento
docker-compose --profile monitoring up -d

# Verificar saúde dos containers
docker-compose ps

# Acompanhar logs
docker-compose logs -f kafka
docker-compose logs -f nexora-redis

# Parar preservando dados
docker-compose stop

# Reset completo (apaga volumes)
docker-compose down -v
```

---

## 💡 Decisões de Design

### Por que Arquitetura Hexagonal?

A separação entre domínio, aplicação e infraestrutura garante que o core business nunca depende de detalhes de persistência, frameworks ou protocolos de comunicação. Na prática, isso significa que o domínio é testado em puro JUnit sem carregar nenhum contexto Spring, e que a troca de PostgreSQL por outro banco afeta apenas os adapters — zero impacto no domínio ou nos casos de uso.

### Por que Value Objects em vez de primitivos?

`Money` e `StockQuantity` encapsulam invariantes que, sem VOs, precisariam ser verificadas em cada ponto de uso. `Money` normaliza escala para 2 casas decimais e garante valor não-negativo. `StockQuantity` garante que o estoque nunca fica negativo. Ambos garantem essas invariantes no construtor — qualquer violação é detectada imediatamente, sem precisar de testes de borda em cada service.

### Por que a máquina de estados está no enum `OrderStatus`?

Colocar `allowedTransitions()` no próprio enum garante que nenhuma transição inválida pode acontecer sem ser detectada pelo domínio. O service não precisa implementar nenhuma lógica de transição — `order.confirm()` chama internamente `transitionTo(CONFIRMED)`, que consulta `status.canTransitionTo(next)` e lança `BusinessRuleException` se inválido. A lógica de negócio mora onde pertence: no domínio.

### Por que `EventPublisher` é uma interface no domínio?

Os services de aplicação publicam eventos sem saber que é Kafka. O `EventPublisherConfig` fornece um no-op bean quando `KafkaTemplate` não está no contexto. Isso significa que testes unitários e de slice (`@WebMvcTest`) funcionam sem nenhuma configuração adicional — não há código de teste dentro do código de produção.

### Por que o bug de `.ordinal()` era perigoso?

O código original comparava `status.ordinal() >= CONFIRMED.ordinal()` para decidir se o estoque deveria ser restaurado no cancelamento. Se um novo status fosse adicionado entre `PENDING` e `CONFIRMED` no enum, o comportamento mudaria silenciosamente — cancelamentos de pedidos pendentes poderiam incorretamente tentar restaurar estoque. A correção usa verificação explícita dos estados onde o estoque foi efetivamente comprometido: `status == CONFIRMED || status == SHIPPED`.

### Por que Transactional Outbox (V9)?

Publicar no Kafka dentro de uma transação JPA é impossível sem 2PC (Two-Phase Commit). A alternativa ingênua — publicar depois do commit — cria um gap: se a aplicação cair entre o commit e o `kafkaTemplate.send()`, o evento é perdido. O Outbox Pattern resolve isso: o evento é escrito na tabela `domain_events_outbox` na **mesma transação** do aggregate. Um relay job ou CDC lê os eventos pendentes e garante a entrega ao Kafka com semântica *at-least-once*.

---

## 🗺 Roadmap

| Fase | Status | Entregáveis |
|------|--------|-------------|
| **Fase 1** | ✅ Concluída | Fundação hexagonal, Produtos, Usuários, Flyway, Swagger |
| **Fase 2** | ✅ Concluída | JWT Auth, Orders, Categories, StockMovement, Paginação, RBAC |
| **Fase 3** | ✅ Concluída | Redis Cache, Kafka Events, Outbox Pattern, Docker stack completo |
| **Fase 4** | 🔜 Planejada | Outbox Relay Job (Debezium/scheduler), Rate Limiting, Circuit Breaker (Resilience4j), CI/CD pipeline, deploy containerizado |

---

<div align="center">

Desenvolvido por **Renan Pires** — Java 21 · Spring Boot 3.3.4 · Arquitetura Hexagonal

</div>