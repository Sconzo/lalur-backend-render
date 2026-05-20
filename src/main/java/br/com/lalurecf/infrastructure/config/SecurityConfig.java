package br.com.lalurecf.infrastructure.config;

import br.com.lalurecf.infrastructure.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuração de segurança Spring Security com JWT.
 *
 * <p>Configura autenticação stateless usando JWT, BCrypt para hashing de senhas e autorização
 * baseada em roles.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Value("${cors.allowed-origins:*}")
  private String allowedOrigins;

  public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
  }

  /**
   * Password encoder usando BCrypt com strength 12.
   *
   * @return BCryptPasswordEncoder configurado
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  /**
   * Configura CORS (Cross-Origin Resource Sharing).
   *
   * @return CorsConfigurationSource configurado
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    // Origens permitidas (configurável via env var)
    List<String> origins = Arrays.asList(allowedOrigins.split(","));

    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(origins);

    // Métodos HTTP permitidos
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

    // Headers permitidos
    configuration.setAllowedHeaders(Arrays.asList("*"));

    // Permitir credenciais (cookies, authorization headers)
    configuration.setAllowCredentials(true);

    // Tempo de cache do preflight (em segundos)
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }

  /**
   * Configura security filter chain.
   *
   * @param http HttpSecurity builder
   * @return SecurityFilterChain configurado
   * @throws Exception se houver erro na configuração
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(
                        (request, response, authException) -> {
                          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                          response.setContentType("application/json");
                          String errorJson =
                              "{"
                                  + "\"error\": \"Unauthorized\", "
                                  + "\"message\": \"Authentication required\""
                                  + "}";
                          response.getWriter().write(errorJson);
                        })
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) -> {
                          response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                          response.setContentType("application/json");
                          String errorJson =
                              "{"
                                  + "\"error\": \"Forbidden\", "
                                  + "\"message\": \"Acesso negado: você não tem permissão "
                                  + "para acessar este recurso\""
                                  + "}";
                          response.getWriter().write(errorJson);
                        }))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/auth/login",
                        "/auth/change-password",
                        "/public/**",
                        "/actuator/health",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
