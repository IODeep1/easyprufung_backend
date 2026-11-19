package com.smartpruefung.backend.Admin.Console.Security;

import com.smartpruefung.backend.Admin.Console.Config.*;
import com.smartpruefung.backend.Admin.Console.Controller.*;
import com.smartpruefung.backend.Admin.Console.DTO.*;
import com.smartpruefung.backend.Admin.Console.Model.*;
import com.smartpruefung.backend.Admin.Console.Repository.*;
import com.smartpruefung.backend.Admin.Console.Service.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.*;
import javax.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class AdminTokenFilter extends OncePerRequestFilter {
  private final AdminAuthService auth;

  public AdminTokenFilter(AdminAuthService auth) {
    this.auth = auth;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    String h = req.getHeader("Authorization");
    boolean adminPath =
        req.getRequestURI().substring(req.getContextPath().length()).startsWith("/api/admin/v1/");
    if (h != null && h.startsWith("Bearer spadm_")) {
      Map<String, Object> identity = auth.authenticate(h.substring(7));
      if (identity == null) {
        res.sendError(401, "Session expired");
        return;
      }
      List<?> names = (List<?>) identity.get("roles");
      List<String> roles = new ArrayList<>();
      if (names.contains("Admin") || names.contains("SuperAdmin")) roles.add("ROLE_ADMIN");
      if (names.contains("SuperAdmin")) roles.add("ROLE_SUPERADMIN");
      if (names.contains("Viewer")) roles.add("ROLE_VIEWER");
      if (roles.isEmpty()) {
        res.sendError(403);
        return;
      }
      var authentication =
          new UsernamePasswordAuthenticationToken(
              identity.get("email"),
              null,
              roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
      authentication.setDetails(identity);
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } else if (adminPath) {
      res.sendError(401, "Admin session required");
      return;
    }
    chain.doFilter(req, res);
  }
}
