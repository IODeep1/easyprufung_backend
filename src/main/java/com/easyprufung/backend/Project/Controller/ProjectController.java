package com.easyprufung.backend.Project.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.easyprufung.backend.EndPoints;
import com.easyprufung.backend.Project.DTO.*;
import com.easyprufung.backend.Project.Helper.IconHelper;
import com.easyprufung.backend.Project.Models.LandingPage;
import com.easyprufung.backend.Project.Project;
import com.easyprufung.backend.Project.Service.ProjectService;
import com.easyprufung.backend.Project.Utility.ImageUtils;
import com.easyprufung.backend.User.Service.SubscriptionService;
import com.easyprufung.backend.User.Service.UserService;
import com.easyprufung.backend.User.Subscription;
import com.easyprufung.backend.User.User;
import com.easyprufung.backend.Utils.JwtUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping
public class ProjectController {
    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private Environment env;

    @PostMapping(path = EndPoints.PROJECT_CREATE)
    public ResponseEntity<?> createProject(@RequestBody ProjectDTO projectDTO, @RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            Subscription subscription = user.getSubscriptions().iterator().next();
            if(subscription.getPlan().equals("free"))
            {
                logger.error("INVALID FREE TRY for "+ user.getEmail());
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "INVALID FREE TRY");
            }

            if(subscription.getQuota() == 0)
            {
                logger.error("INSUFFICIENT_QUOTA for "+ user.getEmail() +"Invalid try");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "INSUFFICIENT_QUOTA");
            }
            if(subscription.getQuota() != -1){
                subscription.setQuota(subscription.getQuota()-1);
                subscriptionService.updateSubscription(subscription);
            }
            projectDTO = projectService.createProject(projectDTO, user);
            if(projectDTO != null){
                return ResponseEntity.ok(projectDTO);
            }
            else{
                logger.error("FAILED_TO_CREATE_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_CREATE_PROJECT");
            }
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PostMapping(path = EndPoints.PROJECT_PREBUILT_CREATE)
    public ResponseEntity<?> createPrebuiltProject(@RequestBody ProjectDTO projectDTO, @RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            projectDTO = projectService.createExternalProject(projectDTO, user);
            if(projectDTO != null){
                return ResponseEntity.ok(projectDTO);
            }
            else{
                logger.error("FAILED_TO_CREATE_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_CREATE_PROJECT");
            }
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PostMapping(path = EndPoints.PROJECT_UPDATE)
    public ResponseEntity<?> updateProject(@RequestBody ProjectDTO projectDTO, @RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(projectDTO.getUuid()))
                    .findFirst();
            if(project.isPresent()){
                var result = projectService.updateProject(projectDTO, project.get());
                return ResponseEntity.ok(result);
            }
            else {
                logger.error("FAILED_TO_GET_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @DeleteMapping(path = EndPoints.PROJECT_DELETE)
    public ResponseEntity<?> deleteProject(@RequestParam String uuid, @RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(uuid))
                    .findFirst();
            if(project.isPresent()){
                var result = projectService.deleteProject(project.get(), user);
                if(result){
                    return ResponseEntity.ok("OK");
                }
                else{
                    logger.error("FAILED_TO_DELETE_PROJECT");
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "FAILED_TO_DELETE_PROJECT");
                }
            }
            else
            {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }

        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PostMapping(path = EndPoints.PROJECT_UPVOTE)
    public ResponseEntity<?> upvoteProject(@RequestParam String uuid, @RequestHeader("Authorization") String authorizationHeader){
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            if(user != null){
                var result = projectService.upvoteProject(user, uuid);
                return ResponseEntity.ok(result);
            }
            else {
                logger.error("FAILED_TO_UPVOTE_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_UPVOTE_PROJECT");
            }
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @GetMapping(path = EndPoints.PROJECT_GET)
    public ResponseEntity<?> getProject(@RequestParam String uuid, @RequestHeader("Authorization") String authorizationHeader){
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(uuid))
                    .findFirst();
            if(project.isPresent()){
                var projectDTO = projectService.getProjectByUUID(project.get());
                return ResponseEntity.ok(projectDTO);
            }
            else {
                logger.error("FAILED_TO_GET_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }
        }
        catch (RuntimeException | JsonProcessingException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @GetMapping(path = EndPoints.PROJECT_VALIDATE)
    public ResponseEntity<?> getProjectValidation(@RequestParam String uuid, @RequestHeader("Authorization") String authorizationHeader){
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(uuid))
                    .findFirst();
            if(project.isPresent()){
                var projectDTO = projectService.getProjectValidation(project.get());
                return ResponseEntity.ok(projectDTO);
            }
            else {
                logger.error("FAILED_TO_GET_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }
        }
        catch (RuntimeException | JsonProcessingException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PostMapping(path = EndPoints.PROJECT_SAVE_DESCRIPTION)
    public ResponseEntity<?> saveProjectDescription(@RequestBody ProjectDTO projectDTO, @RequestHeader("Authorization") String authorizationHeader){
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(projectDTO.getUuid()))
                    .findFirst();
            if(project.isPresent()){
                projectService.saveProjectDescription(projectDTO);
                return ResponseEntity.ok("OK");
            }
            else {
                logger.error("FAILED_TO_GET_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PostMapping(path = EndPoints.PROJECT_SAVE_NAME)
    public ResponseEntity<?> saveProjectName(@RequestBody ProjectDTO projectDTO, @RequestHeader("Authorization") String authorizationHeader){
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(projectDTO.getUuid()))
                    .findFirst();
            if(project.isPresent()){
                var result = projectService.saveProjectName(projectDTO);
                return ResponseEntity.ok(result);
            }
            else {
                logger.error("FAILED_TO_GET_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @GetMapping(path = EndPoints.PROJECT_GENERATE_NAME)
    public ResponseEntity<?> getProjectName(@RequestParam String uuid, @RequestHeader("Authorization") String authorizationHeader){
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(uuid))
                    .findFirst();
            if(project.isPresent()){
                var projectDTO = projectService.getProjectName(project.get());
                return ResponseEntity.ok(projectDTO);
            }
            else {
                logger.error("FAILED_TO_GET_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }
        }
        catch (RuntimeException | JsonProcessingException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }


    @GetMapping(path = EndPoints.PROJECT_DOMAIN_CHECK)
    public ResponseEntity<?> getProjectDomainCheck(@RequestParam String name, @RequestHeader("Authorization") String authorizationHeader){
        try
        {
            var domainList = projectService.checkDomainsAvailability(name);
            return ResponseEntity.ok(domainList);
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @GetMapping(path = EndPoints.PROJECT_SOCIAL_CHECK)
    public ResponseEntity<?> getProjectSocialCheck(@RequestParam String name, @RequestHeader("Authorization") String authorizationHeader){
        try
        {
            var socialList = projectService.checkSocialsAvailability(name);
            return ResponseEntity.ok(socialList);
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PostMapping(path = EndPoints.PROJECT_LOGO_SAVE)
    public ResponseEntity<?> saveProjectLogo(@RequestBody ProjectDTO projectDTO, @RequestHeader("Authorization") String authorizationHeader){
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(projectDTO.getUuid()))
                    .findFirst();
            if(project.isPresent()){
                var result = projectService.saveProjectLogo(projectDTO);
                return ResponseEntity.ok(result);
            }
            else {
                logger.error("FAILED_TO_GET_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @DeleteMapping(path = EndPoints.PROJECT_LOGO_DELETE)
    public ResponseEntity<?> deleteProjectLogo(@RequestParam String uuid, @RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(uuid))
                    .findFirst();
            if(project.isPresent()){
                var result = projectService.deleteProjectLogo(project.get());
                return ResponseEntity.ok(result);
            }
            else
            {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }

        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @GetMapping(path = EndPoints.PROJECT_LOGO_SVG_CONTENT)
    public ResponseEntity<?> getProjectLogoSvgContent(@RequestParam String uuid, @RequestHeader("Authorization") String authorizationHeader){
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(uuid))
                    .findFirst();
            if(project.isPresent()){
                var projectDTO = projectService.getProjectLogoSvgContent(project.get());
                return ResponseEntity.ok(projectDTO);
            }
            else {
                logger.error("FAILED_TO_GET_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }
        }
        catch (RuntimeException | JsonProcessingException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PostMapping(path = EndPoints.PROJECT_LOGO_GENERATE)
    public ResponseEntity<?> generateProjectLogo(@RequestBody Map<String, String> body, @RequestHeader("Authorization") String authorizationHeader){
        try {
            String prompt = body.get("prompt");
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            Subscription subscription = user.getSubscriptions().iterator().next();
            if (subscription.getIteration() == 0) {
                logger.error("INSUFFICIENT_ITERATION for " + user.getEmail() + " Invalid try");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INSUFFICIENT_ITERATION");
            }
            if (subscription.getIteration() != -1) {
                subscription.setIteration(subscription.getIteration() - 1);
                subscriptionService.updateSubscription(subscription);
            }

            String[] models = {
                    "black-forest-labs/FLUX.2-flex",
                    "black-forest-labs/FLUX.1-schnell"
            };

            ObjectMapper objectMapper = new ObjectMapper();
            String b64Json = null;
            ResponseEntity<String> response = null;
            Exception lastException = null;

            for (String model : models) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("model", model);
                payload.put("prompt", prompt);
                payload.put("width", 1024);
                payload.put("height", 1024);
                payload.put("steps", 4);
                payload.put("n", 1);
                payload.put("response_format", "b64_json");

                // Prepare headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(env.getProperty("easyprufung.togetherai.key"));
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

                // Call external API
                RestTemplate restTemplate = new RestTemplate();
                String url = "https://api.together.xyz/v1/images/generations";
                try {
                    response = restTemplate.postForEntity(url, entity, String.class);
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode data = root.path("data");
                    if (data.isArray() && data.size() > 0) {
                        b64Json = data.get(0).path("b64_json").asText(null);
                        if (b64Json != null && !b64Json.isEmpty()) {
                            break; // Success, exit loop
                        }
                    }
                } catch (Exception ex) {
                    lastException = ex;
                    // Try next model
                }
            }

            if (b64Json != null && !b64Json.isEmpty()) {
                ObjectNode result = objectMapper.createObjectNode();
                //logic to remove image BG
               /* RestTemplate removeBgTemplate = new RestTemplate();
                Map<String, String> removeBgBody = Map.of("image_b64", b64Json);
                // Set HTTP headers (content type)
                HttpHeaders removeBgHeaders = new HttpHeaders();
                removeBgHeaders.setContentType(MediaType.APPLICATION_JSON);
                // Create the HttpEntity to send the body with the headers
                HttpEntity<Map<String, String>> entityRemoveBg = new HttpEntity<>(removeBgBody, removeBgHeaders);

                ResponseEntity<Map> removeBgResult = removeBgTemplate.postForEntity(
                        "http://51.89.23.133:8000/remove_bg", entityRemoveBg, Map.class);
                Map<String, Object> responseBody = removeBgResult.getBody();
                String resultBase64 = (String) responseBody.get("result_b64");*/
                result.put("b64_json", "data:image/png;base64," + b64Json);

                return ResponseEntity.status(response.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(result.toString());
            } else {
                String errorMsg = (lastException != null) ?
                        "Error: " + lastException.getMessage() :
                        "b64_json not found";
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(errorMsg);
            }
        } catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PostMapping(path = EndPoints.PROJECT_LOGO_REMOVE_BG)
    public ResponseEntity<?> removeProjectLogoBg(@RequestBody Map<String, String> body, @RequestHeader("Authorization") String authorizationHeader){
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
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PostMapping(path = EndPoints.PROJECT_LANDINGPAGE_CREATE)
    public ResponseEntity<?> createProjectLandingPage(@RequestBody LandingPage landingPage, @RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            Subscription subscription = user.getSubscriptions().iterator().next();
            if(subscription.getPlan().equals("free"))
            {
                logger.error("INVALID FREE TRY for "+ user.getEmail());
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "INVALID FREE TRY");
            }
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(landingPage.getProjectUuid()))
                    .findFirst();
            if(project.isPresent()){
                var projectDTO = projectService.createProjectLandingPage(project.get(), landingPage.getTemplateName(), landingPage.getFeatures());
                if(projectDTO != null){
                    return ResponseEntity.ok(projectDTO);
                }
                else{
                    logger.error("FAILED_TO_CREATE_PROJECT");
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "FAILED_TO_CREATE_PROJECT");
                }
            }
            else {
                logger.error("FAILED_TO_GET_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }

        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        } catch (IOException exception) {
            logger.error(exception.getMessage());
            exception.printStackTrace();
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    @PostMapping(path = EndPoints.PROJECT_LANDINGPAGE_SAVE)
    public ResponseEntity<?> saveProjectLandingPage(@RequestBody ProjectDTO projectDTO, @RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(projectDTO.getUuid()))
                    .findFirst();
            if(project.isPresent()){
                projectService.saveProjectLandingPage(projectDTO);
                return ResponseEntity.ok("OK");
            }
            else {
                logger.error("FAILED_TO_GET_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }

        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @GetMapping(path = EndPoints.PROJECT_LANDINGPAGE_GET)
    public ResponseEntity<?> getProjectLandingPage(@RequestParam String uuid, @RequestHeader("Authorization") String authorizationHeader){
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(uuid))
                    .findFirst();
            if(project.isPresent()){
                var projectDTO = projectService.getProjectLandingPage(project.get());
                return ResponseEntity.ok(projectDTO);
            }
            else {
                logger.error("FAILED_TO_GET_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @GetMapping(path = EndPoints.PROJECT_LANDINGPAGE_DOWNLOAD)
    public ResponseEntity<?> downloadProjectLandingPageCode(@RequestParam String uuid, @RequestParam String type, @RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(uuid))
                    .findFirst();
            if(project.isPresent()){
                var path = projectService.downloadProjectLandingPageCode(project.get(),type);
                if(path != null){
                    Resource resource = new UrlResource(path.toUri());
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName().toString() + "\"")
                            .body(resource);            }
                else{
                    logger.error("FAILED_TO_DOWNLOAD_LANDINGPAGE");
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "FAILED_TO_DOWNLOAD_LANDINGPAGE");
                }
            }
            else {
                logger.error("FAILED_TO_GET_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }

        }
        catch (RuntimeException | IOException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @DeleteMapping(path = EndPoints.PROJECT_LANDINGPAGE_DELETE)
    public ResponseEntity<?> deleteProjectLandingPage(@RequestParam String uuid, @RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(uuid))
                    .findFirst();
            if(project.isPresent()){
                var projectDTO = projectService.deleteProjectLandingPage(project.get());
                if(projectDTO != null){
                    return ResponseEntity.ok(projectDTO);
                }
                else{
                    logger.error("FAILED_TO_DELETE");
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "FAILED_TO_DELETE");
                }
            }
            else {
                logger.error("FAILED_TO_GET_PROJECT");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }

        }
        catch (RuntimeException | IOException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PostMapping(path = EndPoints.PROJECT_DOMAIN_ADD)
    public ResponseEntity<?> addDomain(@RequestBody DomainDTO domainDTO, @RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(domainDTO.getProjectUuid()))
                    .findFirst();
            if(project.isPresent()){
                var result = projectService.addDomain(project.get(), domainDTO, httpClientConsumer.email);
                if(result){
                    return ResponseEntity.ok("OK");
                }
                else{
                    logger.error("FAILED_TO_ADD_DOMAIN");
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "FAILED_TO_ADD_DOMAIN");
                }
            }
            else
            {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }

        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @DeleteMapping(path = EndPoints.PROJECT_DOMAIN_DELETE)
    public ResponseEntity<?> deleteDomain(@RequestParam String uuid, @RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            var project = user.getProjects().stream()
                    .filter(p -> p.getUuid().equals(uuid))
                    .findFirst();
            if(project.isPresent()){
                var result = projectService.deleteDomain(project.get());
                if(result){
                    return ResponseEntity.ok("OK");
                }
                else{
                    logger.error("FAILED_TO_DELETE_DOMAIN");
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "FAILED_TO_DELETE_DOMAIN");
                }
            }
            else
            {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_GET_PROJECT");
            }

        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PostMapping(path = EndPoints.PROJECT_WAITLIST_ADD)
    public ResponseEntity<?> addUserToWaitList(@RequestBody WaitlistDTO waitlistDTO) {
        try
        {
            var result = projectService.addUserToWaitList(waitlistDTO);
            if(result){
                return ResponseEntity.ok("OK");
            }
            else{
                logger.error("FAILED_TO_ADD_USER");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_ADD_USER");
            }
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PostMapping(path = EndPoints.PROJECT_CONTACTFORM_ADD)
    public ResponseEntity<?> addContactForm(@RequestBody ContactFormDTO contactFormDTO) {
        try
        {
            var result = projectService.addContactForm(contactFormDTO);
            if(result){
                return ResponseEntity.ok("OK");
            }
            else{
                logger.error("FAILED_TO_ADD_CONTACTFORM");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_ADD_CONTACTFORM");
            }
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @GetMapping(path = EndPoints.PROJECT_PUBLIC_LIST)
    public ResponseEntity<?> getPublicProjectList() {
        try
        {
            List<PublicProjectDTO> projects = projectService.getPublicProjects();
            return ResponseEntity.ok(projects);
        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @GetMapping(path = EndPoints.PROJECT_LIST)
    public ResponseEntity<?> getProjectList(Pageable pageable) {
        try
        {
            Page<Project> events = projectService.getProjects(pageable);
            return ResponseEntity.ok(events.getContent());
        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @GetMapping(path = EndPoints.PROJECT_LIST_COUNT)
    public ResponseEntity<?> getUsersCount() {
        try
        {
            long projectsCount = projectService.getProjectsCount();
            return ResponseEntity.ok(String.valueOf(projectsCount));
        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    /*@PostMapping(path = EndPoints.PROJECT_UPLOAD_GITHUB)
    public ResponseEntity<?> uploadProjectToGitHub(@RequestBody ProjectDTO projectDTO, @RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var response = new Response();
            projectDTO.setName(projectDTO.getName().replaceAll("\\s+", ""));
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            if(user != null && user.getProjects().stream().filter(pro -> pro.getId() == (projectDTO.getId())) != null){
                String repoName = user.getGithubUsername().toLowerCase()+"_"+projectDTO.getName().toLowerCase();
                // Check if the repository already exists
                if (!gitHubService.checkIfRepoExists(repoName)) {
                    var  localDirectoryPath = Paths.get("./projects/projectsDir/"+projectDTO.getUuid()+"/"+projectDTO.getName()).toAbsolutePath().toString();
                    gitHubService.createRepository(repoName , projectDTO.getDescription());
                    gitHubService.uploadProjectToGitHub(repoName, localDirectoryPath);
                    gitHubService.addCollaborator(repoName, user.getGithubUsername());
                    //gitHubService.uploadAndroidBuildToGitHub(repoName);
                    //var  localDirectoryPath = Paths.get("./projects/projectsDir/"+projectDTO.getUuid()+"/"+projectDTO.getName()).toAbsolutePath().toString();
                    //gitHubService.uploadDirectoryToRepo(repoName, localDirectoryPath);
                    //
                    var project = projectService.getProjectByUUID(projectDTO.getUuid());
                    if(project != null){
                        project.setGithubUrl("https://github.com/"+GitHubService.OWNER+"/"+repoName);
                        projectService.updateProject(project);
                        ModelMapper modelMapper = new ModelMapper();
                        var updatedProject = modelMapper.map(project, ProjectDTO.class);
                        ObjectWriter objectWriter = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).writer().withDefaultPrettyPrinter();
                        response.data = objectWriter.writeValueAsString(updatedProject);
                        response.success = true;
                        return ResponseEntity.ok(response);
                    }
                }
            }
        }
        catch (Exception exc) {
            logger.error(exc.getMessage());
        }
        logger.error("FAILED_TO_UPLOAD_TO_GITHUB");
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "FAILED_TO_UPLOAD_TO_GITHUB");
    }*/
}
