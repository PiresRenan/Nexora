package com.nexora.application.service;

import com.nexora.application.dto.auth.*;
import com.nexora.domain.exception.BusinessRuleException;
import com.nexora.domain.exception.DuplicateResourceException;
import com.nexora.domain.model.*;
import com.nexora.domain.port.EventPublisher;
import com.nexora.domain.repository.UserRepository;
import com.nexora.infrastructure.security.JwtProperties;
import com.nexora.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthApplicationService")
class AuthApplicationServiceTest {

    @Mock AuthenticationManager authManager;
    @Mock JwtTokenProvider      tokenProvider;
    @Mock JwtProperties         jwtProperties;
    @Mock UserRepository        userRepository;
    @Mock PasswordEncoder       passwordEncoder;
    @Mock EventPublisher        eventPublisher;

    @InjectMocks AuthApplicationService service;

    // ─── register ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register — auto-cadastro público")
    class Register {

        @Test
        @DisplayName("Deve criar usuário CUSTOMER e retornar JWT")
        void shouldRegisterAndReturnTokens() {
            var req = new RegisterRequest("João Silva", "joao@test.com", "senha123");

            given(userRepository.existsByEmail("joao@test.com")).willReturn(false);
            given(passwordEncoder.encode("senha123")).willReturn("$2a$12$hash");
            given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(tokenProvider.generateAccessToken(any(), any(), any())).willReturn("access.token");
            given(tokenProvider.generateRefreshToken(any())).willReturn("refresh.token");
            given(jwtProperties.accessTokenExpirationMs()).willReturn(900_000L);
            willDoNothing().given(eventPublisher).publish(any());

            var result = service.register(req);

            assertThat(result.accessToken()).isEqualTo("access.token");
            assertThat(result.role()).isEqualTo(UserRole.CUSTOMER);
            // Verifica que o papel é CUSTOMER independente do que possa vir da request
            then(userRepository).should().save(argThat(u -> u.getRole() == UserRole.CUSTOMER));
            then(eventPublisher).should().publish(any());
        }

        @Test
        @DisplayName("Deve rejeitar email já cadastrado com 409")
        void shouldRejectDuplicateEmail() {
            given(userRepository.existsByEmail("existente@test.com")).willReturn(true);

            assertThatThrownBy(() ->
                    service.register(new RegisterRequest("Nome", "existente@test.com", "senha123"))
            ).isInstanceOf(DuplicateResourceException.class);

            then(userRepository).should(never()).save(any());
        }
    }

    // ─── changePassword ───────────────────────────────────────────────────

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        private User user;

        @BeforeEach
        void setUp() {
            user = User.create("João", "joao@test.com", "$2a$12$currentHash", UserRole.CUSTOMER);
        }

        @Test
        @DisplayName("Deve alterar a senha quando a senha atual está correta")
        void shouldChangePassword() {
            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
            given(passwordEncoder.matches("senhaAtual", "$2a$12$currentHash")).willReturn(true);
            given(passwordEncoder.matches("novaSenha8", "$2a$12$currentHash")).willReturn(false);
            given(passwordEncoder.encode("novaSenha8")).willReturn("$2a$12$newHash");
            given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            assertThatCode(() ->
                    service.changePassword(user.getId(),
                            new ChangePasswordRequest("senhaAtual", "novaSenha8"))
            ).doesNotThrowAnyException();

            then(userRepository).should().save(argThat(u ->
                    u.getPasswordHash().equals("$2a$12$newHash")
            ));
        }

        @Test
        @DisplayName("Deve rejeitar quando a senha atual está errada")
        void shouldRejectWrongCurrentPassword() {
            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
            given(passwordEncoder.matches("senhaErrada", "$2a$12$currentHash")).willReturn(false);

            assertThatThrownBy(() ->
                    service.changePassword(user.getId(),
                            new ChangePasswordRequest("senhaErrada", "novaSenha8"))
            ).isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Current password is incorrect");
        }

        @Test
        @DisplayName("Deve rejeitar quando nova senha é igual à atual")
        void shouldRejectSamePassword() {
            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
            given(passwordEncoder.matches("mesmaSenha", "$2a$12$currentHash")).willReturn(true);

            assertThatThrownBy(() ->
                    service.changePassword(user.getId(),
                            new ChangePasswordRequest("mesmaSenha", "mesmaSenha"))
            ).isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("different");
        }
    }

    // ─── login ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("Deve rejeitar usuário desativado com BusinessRuleException")
        void shouldRejectDeactivatedUser() {
            var user = User.create("João", "joao@test.com", "$2a$12$hash", UserRole.CUSTOMER);
            user.deactivate();

            willDoNothing().given(authManager).authenticate(any());
            given(userRepository.findByEmail("joao@test.com")).willReturn(Optional.of(user));

            assertThatThrownBy(() ->
                    service.login(new LoginRequest("joao@test.com", "senha123"))
            ).isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("deactivated");
        }

        @Test
        @DisplayName("Deve propagar BadCredentialsException do AuthenticationManager")
        void shouldPropagateAuthException() {
            given(authManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("bad credentials"));

            assertThatThrownBy(() ->
                    service.login(new LoginRequest("joao@test.com", "errada"))
            ).isInstanceOf(BadCredentialsException.class);
        }
    }
}