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
 * Rota pública principal: POST /api/v1/auth/register
 *   — permite auto-cadastro de clientes sem autenticação prévia.
 *   — cria usuários com papel CUSTOMER (controlado pelo AuthApplicationService).
 *
 * Demais decisões de acesso:
 *   - Leitura de produtos e categorias é pública (catálogo aberto)
 *   - Pedidos exigem usuário autenticado (qualquer papel)
 *   - Escrita de produtos: SELLER+
 *   - Gestão de categorias e usuários: MANAGER+
 *   - Histórico de estoque: SELLER+
 *   - Actuator além de /health e /info: ADMIN
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, NexoraProperties.class})
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
                        .requestMatchers("/api/v1/auth/register").permitAll()    // auto-cadastro público
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // ── Catálogo público (leitura) ─────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()

                        // ── Rotas que exigem autenticação + papel específico ───────
                        .requestMatchers("/api/v1/products/**").hasAnyRole("SELLER", "MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/categories/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/users/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/stock/**").hasAnyRole("SELLER", "MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/orders/**").authenticated()

                        // ── Actuator (acesso restrito) ─────────────────────────────
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
                "http://localhost:5173"   // Vite default (frontend típico)
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}