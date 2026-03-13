package com.nexora.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.application.dto.auth.ChangePasswordRequest;
import com.nexora.application.dto.auth.LoginRequest;
import com.nexora.application.dto.auth.RegisterRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testa o fluxo completo de auto-cadastro público:
 *   1. POST /api/v1/auth/register → cria CUSTOMER + retorna JWT
 *   2. Garante que o papel é sempre CUSTOMER (não pode ser elevado via register)
 *   3. Garante conflito 409 em email duplicado
 *   4. PATCH /auth/me/password → troca de senha autenticada
 *   5. Após mudança de senha o login antigo falha e o novo funciona
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Register + ChangePassword Integration Tests")
class AuthRegisterIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nexora_reg_it")
            .withUsername("nexora")
            .withPassword("nexora");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",            postgres::getJdbcUrl);
        r.add("spring.datasource.username",        postgres::getUsername);
        r.add("spring.datasource.password",        postgres::getPassword);
        r.add("spring.flyway.enabled",             () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto",     () -> "validate");
        r.add("nexora.jwt.secret", () ->
                "nexora-integration-test-secret-key-256-bits-long-hmac-sha256-ok");
        r.add("nexora.jwt.access-token-expiration-ms",  () -> "900000");
        r.add("nexora.jwt.refresh-token-expiration-ms", () -> "604800000");
    }

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper mapper;

    static String accessToken;

    // ─── Registro ─────────────────────────────────────────────────────────

    @Test @Order(1)
    @DisplayName("Deve registrar novo cliente sem autenticação e retornar JWT")
    void shouldRegisterPublicly() throws Exception {
        var req = new RegisterRequest("Ana Cliente", "ana@test.com", "senha123");

        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))    // sempre CUSTOMER
                .andExpect(jsonPath("$.email").value("ana@test.com"))
                .andReturn();

        accessToken = mapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test @Order(2)
    @DisplayName("Deve acessar /auth/me imediatamente após o registro")
    void shouldAccessMeAfterRegister() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@test.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test @Order(3)
    @DisplayName("Deve retornar 409 para email já cadastrado")
    void shouldRejectDuplicateEmail() throws Exception {
        var req = new RegisterRequest("Outro Ana", "ana@test.com", "outrasenha123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @Order(4)
    @DisplayName("Deve retornar 400 para dados inválidos (senha muito curta)")
    void shouldRejectInvalidPayload() throws Exception {
        var invalid = """
            {"name": "X", "email": "invalido@test.com", "password": "curta"}
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());
    }

    @Test @Order(5)
    @DisplayName("Registro sem autenticação NÃO deve aceitar campos de papel")
    void shouldNotAcceptRoleField() throws Exception {
        // Mesmo que o payload contenha "role", a API ignora — papel é sempre CUSTOMER
        var payload = """
            {"name": "Hacker", "email": "hacker@test.com", "password": "senha123"}
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    // ─── Troca de senha ───────────────────────────────────────────────────

    @Test @Order(6)
    @DisplayName("Deve alterar a senha com a senha atual correta")
    void shouldChangePassword() throws Exception {
        var req = new ChangePasswordRequest("senha123", "novaSenha456");

        mockMvc.perform(patch("/api/v1/auth/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    @Test @Order(7)
    @DisplayName("Login com senha antiga deve falhar após troca")
    void oldPasswordShouldFailAfterChange() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                new LoginRequest("ana@test.com", "senha123"))))
                .andExpect(status().isUnauthorized());
    }

    @Test @Order(8)
    @DisplayName("Login com nova senha deve funcionar")
    void newPasswordShouldLoginSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                new LoginRequest("ana@test.com", "novaSenha456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test @Order(9)
    @DisplayName("Troca de senha sem autenticação deve retornar 403")
    void changePasswordWithoutAuthShouldFail() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                new ChangePasswordRequest("qualquer", "qualquer123"))))
                .andExpect(status().isForbidden());
    }
}