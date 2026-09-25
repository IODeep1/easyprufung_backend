package com.easyprufung.backend.Storage.Controller;


import com.easyprufung.backend.EndPoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping
public class ImageUploadController {
    private final RestTemplate restTemplate = new RestTemplate();


    @PostMapping(path = EndPoints.RESOURCES_IMAGE_UPLOAD)
    public ResponseEntity<String> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "oldImageUrl", required = false) String oldImageUrl) {
        try {
            // Hardcoded secret
            final String IMAGE_SECRET = "pyvtLB7A1ih9jDCSZU2muBXnC962FBU1";

            if (oldImageUrl != null && !oldImageUrl.isEmpty() && oldImageUrl.contains("resources.easyprufung.com")) {
                try {

                    HttpHeaders deleteHeaders = new HttpHeaders();
                    deleteHeaders.set("X-Image-Secret", IMAGE_SECRET);

                    HttpEntity<?> deleteEntity = new HttpEntity<>(deleteHeaders);
                    restTemplate.exchange(oldImageUrl, HttpMethod.DELETE, deleteEntity, String.class);
                } catch (Exception e) {
                    // Handle or log error
                }
            }

            // Upload to Python server
            String uploadUrl = "https://resources.easyprufung.com/resources/images/";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-Image-Secret", IMAGE_SECRET);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl, requestEntity, Map.class);

            var imageUrl = String.format("%s/%s", "https://resources.easyprufung.com/resources/images", response.getBody().get("image_uuid").toString());
            return ResponseEntity.ok(imageUrl);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Helper class to send MultipartFile via RestTemplate
    class MultipartInputStreamFileResource extends InputStreamResource {
        private final String filename;
        public MultipartInputStreamFileResource(InputStream inputStream, String filename) {
            super(inputStream);
            this.filename = filename;
        }
        @Override
        public String getFilename() {
            return this.filename;
        }
        @Override
        public long contentLength() throws IOException {
            return -1; // we don't know
        }
    }

    // Helper to extract image UUID from URL
    private String extractImageUuid(String url) {
        // e.g., "/resources/images/{image_uuid}"
        if (url == null) return null;
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash == -1) return null;
        return url.substring(lastSlash + 1);
    }

    /*@GetMapping(path = EndPoints.RESOURCES_IMAGE_GET)
    public ResponseEntity<byte[]> proxyImage(@PathVariable String imageUuid) {
        String imageUrl = "https://resources.easyprufung.com/resources/images/" + imageUuid;
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                imageUrl,
                HttpMethod.GET,
                entity,
                byte[].class
        );
        MediaType contentType = response.getHeaders().getContentType();
        return ResponseEntity
                .status(response.getStatusCode())
                .contentType(contentType)
                .body(response.getBody());
    }

    @DeleteMapping(path = EndPoints.RESOURCES_IMAGE_DELETE)
    public ResponseEntity<?> deleteImage(@PathVariable String path) {
        String objectName = "images/" + Paths.get(path).getFileName().toString(); ;
        try {
            storageService.deleteImage(objectName);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete image: " + e.getMessage());
        }
    }*/
}
