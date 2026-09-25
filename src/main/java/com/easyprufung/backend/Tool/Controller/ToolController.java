package com.easyprufung.backend.Tool.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.easyprufung.backend.EndPoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping
public class ToolController {


    @PostMapping(path = EndPoints.TOOL_IMAGE_REMOVE_BG)
    public ResponseEntity<?> removeProjectLogoBg(@RequestBody Map<String, String> body){
        try
        {
            String b64Json = body.get("image_b64");
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode result = objectMapper.createObjectNode();
            RestTemplate removeBgTemplate = new RestTemplate();
            Map<String, String> removeBgBody = Map.of("image_b64", b64Json);
            // Set HTTP headers (content type)
            HttpHeaders removeBgHeaders = new HttpHeaders();
            removeBgHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entityRemoveBg = new HttpEntity<>(removeBgBody, removeBgHeaders);

            ResponseEntity<Map> removeBgResult = removeBgTemplate.postForEntity( "http://51.89.23.133:8000/remove_bg", entityRemoveBg, Map.class);
            Map<String, Object> responseBody = removeBgResult.getBody();
            String resultBase64 = (String) responseBody.get("result_b64");
            result.put("b64_json", "data:image/png;base64,"+resultBase64);

            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(result.toString());

        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }
}
