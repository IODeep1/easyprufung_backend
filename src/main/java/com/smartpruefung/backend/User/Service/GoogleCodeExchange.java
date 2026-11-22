package com.smartpruefung.backend.User.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.Arrays;

@Service
public class GoogleCodeExchange {
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final String clientId, clientSecret, redirectUris;
    public GoogleCodeExchange(RestTemplate restTemplate, ObjectMapper mapper,
            @Value("${smartpruefung.google.client-id:1043535428900-78a6e5fcg3gvcj6rvlu52pfrebhmff41.apps.googleusercontent.com}") String clientId,
            @Value("${smartpruefung.google.client-secret:}") String clientSecret,
            @Value("${smartpruefung.google.redirect-uris:https://app.smartpruefung.com/google/callback,http://localhost:3006/google/callback,http://127.0.0.1:3006/google/callback}") String redirectUris) {
        this.restTemplate=restTemplate;this.mapper=mapper;this.clientId=clientId;this.clientSecret=clientSecret;this.redirectUris=redirectUris;
    }
    public String exchange(String code, String verifier, String redirectUri) {
        if (code==null || code.isBlank() || code.length()>4096 || verifier==null || !verifier.matches("[A-Za-z0-9._~-]{43,128}") ||
                redirectUri==null || Arrays.stream(redirectUris.split(",")).map(String::trim).noneMatch(redirectUri::equals))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"INVALID_GOOGLE_REQUEST");
        if (clientSecret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"GOOGLE_REDIRECT_NOT_CONFIGURED");
        MultiValueMap<String,String> form=new LinkedMultiValueMap<>();
        form.add("client_id",clientId);form.add("client_secret",clientSecret);form.add("code",code);
        form.add("code_verifier",verifier);form.add("redirect_uri",redirectUri);form.add("grant_type","authorization_code");
        HttpHeaders headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            String body=restTemplate.postForObject("https://oauth2.googleapis.com/token",new HttpEntity<>(form,headers),String.class);
            JsonNode response=mapper.readTree(body);
            String token=response.path("access_token").asText();
            if (token.isBlank()) throw new IllegalArgumentException();
            return token;
        } catch (Exception error) {
            // Never log Google's response, codes, verifier or client secret.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"GOOGLE_CODE_EXCHANGE_FAILED");
        }
    }
}
