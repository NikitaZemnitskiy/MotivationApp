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
        // для корректного "401 вместо 302" на /api/**
        var apiMatcher = new AntPathRequestMatcher("/api/**");
        RequestCache requestCache = new HttpSessionRequestCache();

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // статическая страница логина и ассеты — открыты
                        .requestMatchers("/login.html", "/assets/**", "/favicon.ico",
                                "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // главная HTML — открыта (чтобы не было лупов). API защищены отдельно.
                        .requestMatchers(HttpMethod.GET, "/", "/index.html").permitAll()

                        // админка — только ADMIN
                        .requestMatchers("/admin.html").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // любые API — только авторизованным
                        .requestMatchers("/api/**").authenticated()

                        // всё остальное — требует логина
                        .anyRequest().authenticated()
                )
                // ВАЖНО: для API-урлов отдаём 401, а не 302 → фронт поймёт и уйдёт на /login.html
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), apiMatcher
                        )
                )
                .requestCache(cache -> cache.requestCache(requestCache))
                .formLogin(form -> form
                        .loginPage("/login.html")          // GET: отдаём статику
                        .loginProcessingUrl("/login")       // POST: обрабатывает Spring Security
                        .successHandler((req, res, auth) -> {
                            // 1) если есть явный redirect (прокинули hidden-полем) — идём туда
                            String redirect = req.getParameter("redirect");
                            if (redirect != null && !redirect.isBlank()) {
                                res.sendRedirect(redirect);
                                return;
                            }
                            // 2) если была сохранённая целевая страница (SavedRequest) — туда
                            var saved = requestCache.getRequest(req, res);
                            if (saved != null) {
                                res.sendRedirect(saved.getRedirectUrl());
                                return;
                            }
                            // 3) иначе — по ролям
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
