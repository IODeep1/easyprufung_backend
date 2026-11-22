package com.smartpruefung.backend.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpruefung.backend.User.Service.GoogleCodeExchange;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GoogleCodeExchangeTest {
    private final RestTemplate http=new RestTemplate();
    private final MockRestServiceServer server=MockRestServiceServer.createServer(http);
    private final String uri="https://app.smartpruefung.com/google/callback";
    private final GoogleCodeExchange exchange=new GoogleCodeExchange(http,new ObjectMapper(),"client-id","test-secret",uri);
    @Test void exchangesCodeWithBackendSecretAndPkce() {
        server.expect(requestTo("https://oauth2.googleapis.com/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("client_secret=test-secret")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("code_verifier="+"a".repeat(43))))
            .andRespond(withSuccess("{\"access_token\":\"google-token\"}",MediaType.APPLICATION_JSON));
        assertEquals("google-token",exchange.exchange("code","a".repeat(43),uri));server.verify();
    }
    @Test void rejectsForeignRedirectAndMissingPkceWithoutExternalCall() {
        assertThrows(ResponseStatusException.class,()->exchange.exchange("code","a".repeat(43),"https://evil.example/google/callback"));
        assertThrows(ResponseStatusException.class,()->exchange.exchange("code",null,uri));server.verify();
    }
    @Test void rejectsMissingConfiguration() {
        var unconfigured=new GoogleCodeExchange(http,new ObjectMapper(),"client-id","",uri);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,assertThrows(ResponseStatusException.class,()->unconfigured.exchange("code","a".repeat(43),uri)).getStatus());
        server.verify();
    }
    @Test void failsWithoutLeakingGoogleErrorDetails() {
        server.expect(requestTo("https://oauth2.googleapis.com/token")).andRespond(withBadRequest().body("private-provider-response"));
        var error=assertThrows(ResponseStatusException.class,()->exchange.exchange("code","a".repeat(43),uri));
        assertFalse(error.toString().contains("private-provider-response"));assertNull(error.getCause());server.verify();
    }
}
