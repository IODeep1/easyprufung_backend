package com.easyprufung.backend.Utils;
import com.easyprufung.backend.Shared.HttpClientConsumer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

public class JwtUtils {
    public static String GOOGLE_SECRET_KEY ="Ts||Qssliw938JR'wkjd#@4A#HYzIe?";
    public static String SECRET_KEY ="BZI'Xliw938JR'wkjd#@4A#HYzIe?";
    public static String HEADER = "Authorization";
    public static String BEARER_PREFIX = "Bearer ";

    public static HttpClientConsumer getHttpClientConsumer(String token) {
        HttpClientConsumer httpClientConsumer = new HttpClientConsumer();
        try {
            String finalToken = token.replace(BEARER_PREFIX, "");
            Claims body = Jwts.parser()
                    .setSigningKey(SECRET_KEY.getBytes(Charset.forName("UTF-8")))
                    .parseClaimsJws(finalToken)
                    .getBody();
            var authorities = (ArrayList<String>)(body.get("authorities"));
            httpClientConsumer.role = authorities.get(0);
            httpClientConsumer.email = body.get("sub").toString();

        } catch (JwtException | ClassCastException e) {
        }
        return httpClientConsumer;
    }

    public static String getJWTToken(String httpClientEmail, String role) {
        List<GrantedAuthority> grantedAuthorities = AuthorityUtils
                .commaSeparatedStringToAuthorityList(role);

        Claims claims = Jwts.claims().setSubject(httpClientEmail);
        claims.put("authorities",
                grantedAuthorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()));


        String token = Jwts
                .builder()
                .setId("softtekJWT")
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 2592000000L))
                .signWith(SignatureAlgorithm.HS512,
                        SECRET_KEY.getBytes()).compact();

        return token;
    }

    public static boolean validateToken(String token) {
        try {
            var jwtToken = token.replace(BEARER_PREFIX, "");
            Jwts.parser().setSigningKey(SECRET_KEY.getBytes()).parseClaimsJws(jwtToken);
            return true;
        } catch (Exception exception){
        }
        return false;
    }
}

