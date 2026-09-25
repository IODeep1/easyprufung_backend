package com.easyprufung.backend.User.Controller;

import com.easyprufung.backend.User.DTO.*;
import com.easyprufung.backend.User.Service.SubscriptionService;
import com.easyprufung.backend.User.Subscription;
import com.easyprufung.backend.User.User;
import com.easyprufung.backend.Shared.RoleTags;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.easyprufung.backend.User.DTO.UserDTO;
import com.easyprufung.backend.User.Service.UserService;
import com.easyprufung.backend.EndPoints;
import com.easyprufung.backend.Utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;

@Slf4j
@RepositoryRestController
@RequestMapping
@RequiredArgsConstructor
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final SubscriptionService subscriptionService;
    private final UserService userService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping(path = EndPoints.USER_GOOGLE_LOGIN)
    public ResponseEntity<?> loginGoogleUser(HttpServletRequest request, @RequestBody GoogleTokenDTO googleTokenDTO) {
        try
        {
            String userInfoUri = "https://www.googleapis.com/oauth2/v3/userinfo";
            String userInfo = restTemplate.getForObject(userInfoUri + "?access_token=" + googleTokenDTO.getAccess_token(), String.class);
            JsonNode userInfoNode = objectMapper.readTree(userInfo); // Parse JSON to JsonNode

            String email = userInfoNode.get("email").asText();
            String firstName = userInfoNode.get("given_name").asText();
            String lastName = userInfoNode.get("family_name").asText();
            User user ;
            if(!userService.checkIfUserExist(email)) {
                UserDTO userDTO = new UserDTO();
                userDTO.setEmail(email);
                userDTO.setFirstname(firstName);
                userDTO.setLastname(lastName);
                userDTO.setSource("google");
                userDTO.setPassword(JwtUtils.GOOGLE_SECRET_KEY);
                String ipAddress = "";
                try {
                    ipAddress = request.getHeader("X-Forwarded-For");
                    if (ipAddress == null || ipAddress.isEmpty()) {
                        ipAddress = request.getRemoteAddr();
                    }
                }
                catch (Exception e) {}
                user = userService.createUser(userDTO, ipAddress);
                Subscription newSubscription = subscriptionService.createFreeSubscription(email);
                user.setSubscriptions(new HashSet<>());
                user.addSubscription(newSubscription);
                user = userService.updateUser(user);
            }
            else  {
                user = userService.getUserByEmail(email);
            }
            if(user != null) {
                String token = JwtUtils.getJWTToken(user.getEmail(), RoleTags.UserRole);
                UserDTO userDTO = userService.getUserDTO(user);
                userDTO.setToken(token);
                return ResponseEntity.ok(userDTO);

            }
            else{
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "INVALID_CRIDENTIALS");
            }
        }catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", exc);
        } catch (JsonMappingException e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }
    }

    @PostMapping(path = EndPoints.USER_LOGIN)
    public ResponseEntity<?> loginUser(@RequestBody UserDTO userDTO) {
        try
        {
            User user = userService.checkUserCridentials(userDTO.getEmail(), userDTO.getPassword());
            if(user != null) {
                String token = JwtUtils.getJWTToken(userDTO.getEmail(), RoleTags.UserRole);
                userDTO = userService.getUserDTO(user);
                userDTO.setToken(token);
                return ResponseEntity.ok(userDTO);
            }
            else{
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "INVALID_CRIDENTIALS");
            }
        }catch (RuntimeException  exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", exc);
        }
    }

    @PostMapping(path = EndPoints.USER_CREATE)
    public ResponseEntity<?> createUser(HttpServletRequest request, @RequestBody UserDTO userDTO) {
        try
        {
            String ipAddress = "";
            try {
                ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isEmpty()) {
                    ipAddress = request.getRemoteAddr();
                }
            }
            catch (Exception e) {}
            User newUser = userService.createUser(userDTO, ipAddress);
            Subscription newSubscription = subscriptionService.createFreeSubscription(userDTO.getEmail());
            newUser.setSubscriptions(new HashSet<>());
            newUser.addSubscription(newSubscription);
            newUser = userService.updateUser(newUser);
            if(newUser != null){
                String token = JwtUtils.getJWTToken(newUser.getEmail(), RoleTags.UserRole);
                userDTO = userService.getUserDTO(newUser);
                userDTO.setToken(token);
                return ResponseEntity.ok(userDTO);
            }
            else{
                logger.error("FAILED_TO_CREATE_USER");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_CREATE_USER");
            }

        }
        catch (RuntimeException  exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PostMapping(path = EndPoints.USER_FORGOT_PASSWORD)
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        try
        {
            String result = userService.forgotPassword(email);
            return ResponseEntity.ok(result);
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PostMapping(path = EndPoints.USER_RESET_PASSWORD)
    public ResponseEntity<String> resetPassword(@RequestBody PasswordReset passwordReset) {
        try
        {
            String result = userService.resetPassword(passwordReset.getToken(),passwordReset.getPassword());
            return ResponseEntity.ok(result);
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PostMapping(path = EndPoints.USER_INTERCOM_HASH)
    public ResponseEntity<?> intercomHash(@RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            String secret = "NWVqcYb5a-QNBuaHp8k78169nIskoR2FReEt-Hm7"; // IMPORTANT: your web Identity Verification secret key - keep it safe!
            String userIdentifier = httpClientConsumer.email; // IMPORTANT: the email for your user
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKey);
            byte[] hmacBytes = hmacSha256.doFinal(userIdentifier.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hmacBytes) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }
            String hmac = hexString.toString();
            return ResponseEntity.ok(hmac);
        }
        catch (RuntimeException | NoSuchAlgorithmException | InvalidKeyException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PostMapping(path = EndPoints.USER_UPDATE)
    public ResponseEntity<?> updateUser(@RequestBody UserDTO userDTO, @RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            if(!httpClientConsumer.email.equals(userDTO.getEmail()))
            {
                logger.error("UNAUTHORIZED_TO_UPDATE_USER");

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "UNAUTHORIZED_TO_UPDATE_USER");
            }
            var updatedUser = userService.updateUser(userDTO);
            if(updatedUser != null){
                return ResponseEntity.ok(updatedUser);
            }
            else{
                logger.error("FAILED_TO_UPDATE_USER");

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_UPDATE_USER");
            }
        }
        catch (RuntimeException exc) {
            logger.error(exc.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @DeleteMapping(path = EndPoints.USER_DELETE)
    public ResponseEntity<?> deleteUser(@PathVariable long id)
    {
        try
        {
            userService.deleteUser(id);
            return ResponseEntity.ok("OK");
        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @GetMapping(path = EndPoints.USER_GET)
    public ResponseEntity<?> getUser(@RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            User user = userService.getUserByEmail(httpClientConsumer.email);
            UserDTO userDTO = userService.getUserDTO(user);
            return ResponseEntity.ok(userDTO);
        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @GetMapping(path = EndPoints.USER_PROJECTS)
    public ResponseEntity<?> getProjects(@RequestHeader("Authorization") String authorizationHeader) {
        try
        {
            var httpClientConsumer = JwtUtils.getHttpClientConsumer(authorizationHeader);
            UserDTO userDTO = userService.getUserProjects(httpClientConsumer.email);
            return ResponseEntity.ok(userDTO);
        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @GetMapping(path = EndPoints.USER_LIST)
    public ResponseEntity<?> getUserList(Pageable pageable) {
        try
        {
            Page<User> events = userService.getUsers(pageable);
            return ResponseEntity.ok(events.getContent());
        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @GetMapping(path = EndPoints.USER_LIST_COUNT)
    public ResponseEntity<?> getUsersCount() {
        try
        {
            long usersCount = userService.getUsersCount();
            return ResponseEntity.ok(usersCount);
        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

}
