package com.easyprufung.backend.Project.Helper;

import com.easyprufung.backend.Project.Models.Domain;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class DomainHelper {
    private static final String[] topLevelNames = {"com","ai","net","org","us","info","cloud","store","xyz","pro","gg","online"};
    public static List<String>  generateDomains(String name){
        List<String> domains = new ArrayList<>();
        for (var topLevelName :topLevelNames) {
            domains.add(name+"."+topLevelName);
        }
        return domains;
    }


    public static boolean isWebsiteUp(String url) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            // Make an HTTP request to the URL and check the response status code
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCodeValue() == 200; // Check for HTTP 200 OK
        } catch (Exception e) {
            // Handle any exceptions (e.g., timeouts, unreachable website)
            return false;
        }
    }

    public static String GenerateDNSToken(){

        String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder token = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            int index = random.nextInt(CHARACTERS.length());
            token.append(CHARACTERS.charAt(index));
        }
        return token.toString();
    }
}
