package com.buseiny.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // ensure API paths return 401 instead of 302 redirects
        var apiMatcher = new AntPathRequestMatcher("/api/**");
        RequestCache requestCache = new HttpSessionRequestCache();

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // static login page and assets are public
                        .requestMatchers("/login.html", "/assets/**", "/favicon.ico",
                                "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // main HTML is public to avoid redirect loops; APIs are protected separately
                        .requestMatchers(HttpMethod.GET, "/", "/index.html").permitAll()

                        // admin endpoints require ADMIN role
                        .requestMatchers("/admin.html").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // any API request requires authentication
                        .requestMatchers("/api/**").authenticated()

                        // everything else requires login
                        .anyRequest().authenticated()
                )
                // return 401 for API URLs so frontend can redirect to /login.html
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), apiMatcher
                        )
                )
                .requestCache(cache -> cache.requestCache(requestCache))
                .formLogin(form -> form
                        .loginPage("/login.html")          // GET: serve static login page
                        .loginProcessingUrl("/login")       // POST: handled by Spring Security
                        .successHandler((req, res, auth) -> {
                            // 1) explicit redirect parameter
                            String redirect = req.getParameter("redirect");
                            if (redirect != null && !redirect.isBlank()) {
                                res.sendRedirect(redirect);
                                return;
                            }
                            // 2) saved request destination
                            var saved = requestCache.getRequest(req, res);
                            if (saved != null) {
                                res.sendRedirect(saved.getRedirectUrl());
                                return;
                            }
                            // 3) otherwise redirect based on role
                            boolean isAdmin = auth.getAuthorities().stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .anyMatch(a -> a.equals("ROLE_ADMIN"));
                            res.sendRedirect(isAdmin ? "/admin.html" : "/");
                        })
                        .failureUrl("/login.html?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login.html?logout=true")
                );

        return http.build();
    }

    @Bean
    public UserDetailsService users(PasswordEncoder encoder) {
        var admin = User.withUsername("admin")
                .password(encoder.encode("admin"))
                .roles("ADMIN")
                .build();
        var anna = User.withUsername("Anna")
                .password(encoder.encode("rabota"))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(admin, anna);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
