package com.bankhoahoc.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Nhớ import cái này
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Cấu hình CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. Tắt CSRF
                .csrf(csrf -> csrf.disable())

                // 3. Stateless Session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Phân quyền (CHO PHÉP TẤT CẢ ĐỂ DEBUG)
                .authorizeHttpRequests(auth -> auth
                        // Cho phép method OPTIONS (Preflight request) đi qua mọi nơi
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Cho phép tất cả các đường dẫn khác (Kể cả Admin, User, Public...)
                        // Sau khi test xong kết nối thì sửa lại chỗ này sau
                        .anyRequest().permitAll()
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // [CỰC KỲ QUAN TRỌNG] Dùng setAllowedOriginPatterns("*") để chấp nhận TẤT CẢ các domain
        // (Bao gồm localhost:5173, localhost:3000, IP LAN, v.v...)
        configuration.setAllowedOriginPatterns(List.of("*"));

        // Cho phép tất cả các method (GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH...)
        configuration.setAllowedMethods(List.of("*"));

        // Cho phép tất cả các Header
        configuration.setAllowedHeaders(List.of("*"));

        // Cho phép gửi Cookie/Credential
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}