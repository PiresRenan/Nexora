package com.nexora.infrastructure.config;

import com.nexora.infrastructure.security.JwtAuthenticationFilter;
import com.nexora.infrastructure.security.JwtProperties;
import com.nexora.infrastructure.security.NexoraUserDetailsService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

/**
 * Configuração de segurança do Nexora.
 *
 * Política de acesso para attachments:
 *  - GET  /attachments/products/{id}         → público (imagens de catálogo)
 *  - POST /attachments/products/{id}/images  → SELLER+ (upload de imagem)
 *  - POST /attachments/products/{id}/documents → SELLER+ (nota fiscal, manual)
 *  - POST /attachments/users/{id}/photo      → autenticado (próprio usuário ou MANAGER+)
 *  - POST /attachments/users/{id}/documents  → MANAGER+ (documentos pessoais)
 *  - GET  /attachments/users/{id}            → MANAGER+ ou próprio usuário
 *  - GET  /attachments/{id}/url              → autenticado
 *  - DELETE /attachments/{id}                → SELLER+
 *
 * Controles mais granulares (ex: "apenas o próprio usuário pode ver seus documentos")
 * são aplicados via @PreAuthorize no AttachmentController.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, NexoraProperties.class, StorageProperties.class})
public class SecurityConfig {

    private final NexoraUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter  jwtAuthFilter;

    public SecurityConfig(NexoraUserDetailsService userDetailsService,
                          JwtAuthenticationFilter jwtAuthFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthFilter      = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth

                        // ── Rotas totalmente públicas ──────────────────────────────
                        .requestMatchers("/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // ── Catálogo público (leitura) ─────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()

                        // ── Imagens de produto: leitura pública (catálogo) ─────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/attachments/products/**").permitAll()

                        // ── Attachments: escrita e listagem de usuários (autenticado) ─
                        .requestMatchers("/api/v1/attachments/**").authenticated()

                        // ── Demais rotas por papel ─────────────────────────────────
                        .requestMatchers("/api/v1/products/**").hasAnyRole("SELLER", "MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/categories/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/users/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/stock/**").hasAnyRole("SELLER", "MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/orders/**").authenticated()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:5173"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}