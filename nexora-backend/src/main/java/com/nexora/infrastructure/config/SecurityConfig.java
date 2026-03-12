package com.nexora.infrastructure.config;

import com.nexora.infrastructure.security.JwtAuthenticationFilter;
import com.nexora.infrastructure.security.JwtProperties;
import com.nexora.infrastructure.security.NexoraUserDetailsService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuração central do Spring Security.
 *
 * Estratégia de autorização por rota (coarse-grained):
 * - /api/v1/auth/**       → público (login, refresh)
 * - GET /api/v1/products  → público (catálogo consultável sem login)
 * - POST/PUT/DELETE       → autenticado
 * - /api/v1/users/**      → MANAGER ou ADMIN
 * - /api/v1/orders/**     → autenticado (cliente vê os próprios)
 * - /actuator/**          → ADMIN
 *
 * Autorização fina (por recurso) é feita no UseCase via @PreAuthorize.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity           // habilita @PreAuthorize, @PostAuthorize
@EnableConfigurationProperties(JwtProperties.class)
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
                        // ─── Rotas públicas ──────────────────────────────────────
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Catálogo público (leitura)
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                        // ─── Rotas autenticadas ──────────────────────────────────
                        .requestMatchers("/api/v1/products/**").hasAnyRole("SELLER", "MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/categories/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/users/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/orders/**").authenticated()
                        .requestMatchers("/api/v1/stock/**").hasAnyRole("SELLER", "MANAGER", "ADMIN")
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // ─── Qualquer outra rota requer autenticação ─────────────
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
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:3001"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}