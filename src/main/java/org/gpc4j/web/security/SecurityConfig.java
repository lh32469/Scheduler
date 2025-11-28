package org.gpc4j.web.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  @Value("${management.server.port}")
  int managementPort;

  private final RavenUserDetailsService userDetailsService;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf ->
                  csrf.ignoringRequestMatchers("/actuator/**"))
        .csrf(csrf ->
                  csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                request ->
                    request.getLocalPort() == managementPort).permitAll()
            .requestMatchers(
                "/",
                "/login", "/error",
                "/styles.css", "/static/**", "/webjars/**", "/favicon.ico",
                "/templates/fragments/**",
                "/signup", "/verify", "/verify/**",
                "/verify-reset", "/verify-reset/**")
            .permitAll()
            .requestMatchers("/bookings/**")
            .authenticated()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().permitAll()
        )
        .formLogin(login -> login
            .loginPage("/login").permitAll()
            .defaultSuccessUrl("/", true)
            .failureUrl("/login?error")
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            // After logging out, send the user back to the main index page
            .logoutSuccessUrl("/")
            .permitAll()
        );

    return http.build();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws
      Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

}
