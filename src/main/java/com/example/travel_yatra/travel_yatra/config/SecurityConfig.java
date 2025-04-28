package com.example.travel_yatra.travel_yatra.config;

import com.example.travel_yatra.travel_yatra.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtUtil jwtUtil = new JwtUtil(System.getenv().getOrDefault("JWT_SECRET", "defaultsecretdefaultsecretdefaultse"));
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/hello").permitAll() // Make /hello public
                .requestMatchers(HttpMethod.GET, "/api/bus-stops", "/api/bus-stops/").permitAll() // GET all
                .requestMatchers(HttpMethod.GET, "/api/bus-stops/{id}").permitAll() // GET by id
                .requestMatchers("/api/bus-stops/**").hasRole("ADMIN") // Protect POST and DELETE
                .requestMatchers(HttpMethod.GET, "/api/buses", "/api/buses/").permitAll() // GET all buses public
                .requestMatchers(HttpMethod.GET, "/api/buses/{id}").permitAll() // GET bus by id public
                .requestMatchers("/api/buses/**").hasRole("ADMIN") // Protect POST and DELETE for buses
                .requestMatchers(HttpMethod.GET, "/api/bus-categories", "/api/bus-categories/").permitAll() // GET all categories public
                .requestMatchers(HttpMethod.GET, "/api/bus-categories/{id}").permitAll() // GET category by id public
                .requestMatchers("/api/bus-categories/**").hasRole("ADMIN") // Protect POST and DELETE for categories
                .requestMatchers(HttpMethod.GET, "/api/trips/my-trips/current").hasRole("BUS_DRIVER")
                .requestMatchers(HttpMethod.GET, "/api/trips/my-trips").hasRole("BUS_DRIVER")
                .requestMatchers(HttpMethod.GET, "/api/trips", "/api/trips/", "/api/trips/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/drivers").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/bookings/reserved-seats").permitAll() // Make reserved-seats public
                .requestMatchers(HttpMethod.GET, "/api/bookings/by-email").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/bookings/my").hasRole("TRAVELLER")
                // Restrict Khalti payment initiation to travellers only
                .requestMatchers(HttpMethod.POST, "/api/payments/khalti/initiate").hasRole("TRAVELLER")
                // Restrict Khalti payment initiation to travellers only
                .requestMatchers(HttpMethod.POST, "/api/payments/khalti/lookup").hasRole("TRAVELLER")
                .requestMatchers("/uploads/**").permitAll() // Allow public access to uploaded images
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*"); // Allow all origins
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
