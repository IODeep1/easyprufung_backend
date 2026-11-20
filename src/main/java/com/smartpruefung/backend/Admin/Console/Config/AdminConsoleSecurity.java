package com.smartpruefung.backend.Admin.Console.Config;

import com.smartpruefung.backend.Admin.Console.Controller.*;
import com.smartpruefung.backend.Admin.Console.DTO.*;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.Admin.Console.Repository.*;
import com.smartpruefung.backend.Admin.Console.Security.*;
import com.smartpruefung.backend.Admin.Console.Service.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

@Configuration
@Order(1)
public class AdminConsoleSecurity extends WebSecurityConfigurerAdapter {
  private final AdminAuthService auth;

  @Value("${ADMIN_ALLOWED_ORIGINS:http://localhost:3007,http://127.0.0.1:3007}")
  private String origins;

  public AdminConsoleSecurity(AdminAuthService auth) {
    this.auth = auth;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // A separate filter chain for admin APIs leaves the customer authentication chain untouched.
    http.requestMatchers()
        .antMatchers(
            "/api/admin/**",
            "/public/admin/session",
            "/api/user/list",
            "/api/user/list/count",
            "/api/user/delete/**",
            "/api/subscription/list",
            "/api/subscription/create",
            "/api/contact/list",
            "/api/contact/get",
            "/api/contact/delete/**")
        .and()
        .csrf()
        .disable()
        .cors()
        .configurationSource(adminCors())
        .and()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .addFilterBefore(new AdminTokenFilter(auth), UsernamePasswordAuthenticationFilter.class)
        .authorizeRequests()
        .antMatchers(HttpMethod.OPTIONS, "/**")
        .permitAll()
        .antMatchers("/public/admin/session")
        .permitAll()
        .antMatchers(
            "/api/admin/create/**",
            "/api/admin/update/**",
            "/api/admin/delete/**",
            "/api/admin/list/**",
            "/api/admin/role_list/**",
            "/api/user/delete/**")
        .hasRole("SUPERADMIN")
        .antMatchers("/api/admin/v1/**")
        .hasAnyRole("ADMIN", "VIEWER")
        .anyRequest()
        .hasRole("ADMIN")
        .and()
        .exceptionHandling()
        .authenticationEntryPoint((req, res, e) -> res.sendError(401))
        .accessDeniedHandler((req, res, e) -> res.sendError(403));
  }

  private CorsConfigurationSource adminCors() {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(Arrays.asList(origins.split(",")));
    c.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Reason", "If-Match"));
    c.setAllowCredentials(false);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }
}
