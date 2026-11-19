package com.smartpruefung.backend.Admin.Console.Security;

import com.smartpruefung.backend.Admin.Console.Config.*;
import com.smartpruefung.backend.Admin.Console.Controller.*;
import com.smartpruefung.backend.Admin.Console.DTO.*;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.Admin.Console.Repository.*;
import com.smartpruefung.backend.Admin.Console.Service.*;
import com.smartpruefung.backend.User.Repository.UsersRepository;
import com.smartpruefung.backend.Utils.JwtUtils;
import io.jsonwebtoken.*;
import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces admin account-state decisions without editing the legacy JWT filter or user services.
 */
@org.springframework.core.annotation.Order(-101)
public class AdminUserGuard extends OncePerRequestFilter {
  private final UsersRepository users;
  private final AdminUserStateRepository states;

  public AdminUserGuard(UsersRepository users, AdminUserStateRepository states) {
    this.users = users;
    this.states = states;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    String header = req.getHeader("Authorization");
    if (req.getRequestURI().startsWith(req.getContextPath() + "/api/")
        && header != null
        && header.startsWith("Bearer ")
        && !header.startsWith("Bearer spadm_"))
      try {
        Claims c =
            Jwts.parser()
                .setSigningKey(
                    JwtUtils.SECRET_KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .parseClaimsJws(header.substring(7))
                .getBody();
        java.util.List<?> authorities = c.get("authorities", java.util.List.class);
        if (authorities != null
            && authorities.stream()
                .anyMatch(
                    r ->
                        com.smartpruefung.backend.Shared.RoleTags.UserRole.equals(String.valueOf(r))
                            || "ROLE_USER".equals(String.valueOf(r)))) {
          var user = users.findByEmail(c.getSubject());
          if (user == null) {
            res.sendError(401, "Account no longer exists");
            return;
          }
          var state = states.findById(user.getId()).orElse(null);
          if (state != null
              && (!state.isEnabled()
                  || (state.getValidAfter() != null
                      && (c.getIssuedAt() == null
                          || c.getIssuedAt().toInstant().isBefore(state.getValidAfter()))))) {
            res.sendError(403, "Account disabled or credentials revoked by an administrator");
            return;
          }
        }
      } catch (JwtException | IllegalArgumentException e) {
        /* Original security chain retains responsibility for invalid legacy tokens. */
      }
    chain.doFilter(req, res);
  }

  @Configuration
  public static class Registration {
    @Bean
    public AdminUserGuard adminUserGuard(UsersRepository users, AdminUserStateRepository states) {
      return new AdminUserGuard(users, states);
    }

    @Bean
    public FilterRegistrationBean<AdminUserGuard> adminUserStateGuard(AdminUserGuard guard) {
      var bean = new FilterRegistrationBean<>(guard);
      bean.setOrder(-101);
      return bean;
    }
  }
}
