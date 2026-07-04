package com.madhurgram.productservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 🌐 1. Next.js के लिए CORS एनेबल करना
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(List.of("*")); // प्रोडक्शन में इसे अपने Next.js डोमेन से रिप्लेस करना
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                return config;
            }))
            
            // 🛡️ 2. CSRF को डिसेबल करना (क्योंकि हम Stateless JWT यूज़ कर रहे हैं)
            .csrf(csrf -> csrf.disable())
            
            // 🚦 3. राउट्स के नियम (असली ताला)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()   // लॉगिन करने का रास्ता सबके लिए खुला है
                .requestMatchers("/api/admin/**").authenticated() // 🔒 एडमिन का सारा डेटा सिर्फ टोकन वालों को मिलेगा
                .anyRequest().permitAll() // बाकी स्टोरफ्रंट API (जैसे कस्टमर के लिए प्रोडक्ट देखना) खुले हैं
            )
            
            // 🧠 4. स्प्रिंग को बताओ कि हम सेशन (Cookies) नहीं, बल्कि टोकन (Stateless) यूज़ करेंगे
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 🛂 5. डिफ़ॉल्ट लॉगिन से पहले अपना JWT गार्ड खड़ा कर दो
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}