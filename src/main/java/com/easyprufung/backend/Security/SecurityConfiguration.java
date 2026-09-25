package com.easyprufung.backend.Security;

import com.easyprufung.backend.EndPoints;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    protected void configure(HttpSecurity http) throws Exception {

        http.csrf().disable()
                .addFilterAfter(new JWTAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class)
                .authorizeRequests()
                //.antMatchers(getPublicEndPoints()).permitAll()
                .antMatchers(getProtectedEndPoints()).authenticated()
                .anyRequest().permitAll();

    }


    String[] getProtectedEndPoints() {
        return new String[]{
                EndPoints.Protected.PREFIX + "/**",
        };
    }
}