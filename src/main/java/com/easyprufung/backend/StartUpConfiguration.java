package com.easyprufung.backend;


import com.easyprufung.backend.Admin.Admin;
import com.easyprufung.backend.Admin.Repository.AdminsRepository;
import com.easyprufung.backend.Admin.Repository.RolesRepository;
import com.easyprufung.backend.Admin.Role;
import com.easyprufung.backend.PromoCode.PromoCode;
import com.easyprufung.backend.PromoCode.Service.PromoCodeService;
import com.easyprufung.backend.Security.AESEncryption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class StartUpConfiguration {
    @Autowired
    private RolesRepository rolesRepository;
    @Autowired
    private AdminsRepository adminsRepository;
    @Autowired
    private PromoCodeService promoCodeService;

    public void Init() {
        SetAdminRoles();
        SetDefaultAdmin();
    }

    void SetDefaultAdmin() {
        if (adminsRepository.findByEmail("super.admin@easyprufung.com") != null)
            return;

        Admin defaultAdmin = new Admin();
        defaultAdmin.setFirstname("super");
        defaultAdmin.setLastname("admin");
        defaultAdmin.setEmail("super.admin@easyprufung.com");
        defaultAdmin.setPassword("!EasyPrufung4ever");
        Date date = new Date();
        defaultAdmin.setCreatedDate(new Timestamp(date.getTime()));
        defaultAdmin.setUpdatedDate(new Timestamp(date.getTime()));

        var superAdminRole = rolesRepository.findByName("SuperAdmin");
        var adminRole = rolesRepository.findByName("Admin");
        defaultAdmin.addRole(superAdminRole);
        defaultAdmin.addRole(adminRole);
        defaultAdmin.setPassword(AESEncryption.encrypt(defaultAdmin.getPassword()));
        adminsRepository.save(defaultAdmin);
    }

    void SetAdminRoles() {
        if (rolesRepository.findAll().size() > 0)
            return;
        Role superAdmin = new Role("SuperAdmin");
        Role admin = new Role("Admin");
        rolesRepository.save(superAdmin);
        rolesRepository.save(admin);
    }



    void SetAppSumoPromoCodes(){
        List<PromoCode> promoCodes = promoCodeService.findAll();
        if(promoCodes.size() == 0 || !(promoCodes.stream().anyMatch(promoCode -> promoCode.getCode().toLowerCase().contains("appsumo_pro_")))){
            for (String code: readAppSumoCodes() ) {
                promoCodeService.createAppSumoPromoCode(code);
            }
        }
    }

    String[] readAppSumoCodes() {
        try {
            ClassPathResource resource = new ClassPathResource("/PromoCode/appsumo_promocodes.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
            List<String> lines = reader.lines().collect(Collectors.toList());
            return lines.toArray(new String[0]);

        } catch (Exception e) {
           
            return new String[0];
        }
    }

    void GenerateAppSumoCodes(){
        List<String> uuidList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            uuidList.add("appsumo_pro_"+UUID.randomUUID().toString());
        }
        String filePath = Paths.get("src", "main", "resources", "PromoCode", "appsumo_promocodes.txt").toAbsolutePath().toString();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String uuid : uuidList) {
                writer.write(uuid);
                writer.newLine();
            }
        } catch (IOException e) {
           
        }
    }
}
