package com.easyprufung.backend.Admin.Service;


import com.easyprufung.backend.Admin.Admin;
import com.easyprufung.backend.Admin.DTO.AdminDTO;
import com.easyprufung.backend.Admin.Repository.AdminsRepository;
import com.easyprufung.backend.Admin.Repository.RolesRepository;
import com.easyprufung.backend.Admin.Role;
import com.easyprufung.backend.Security.AESEncryption;
import com.easyprufung.backend.User.DTO.UserDTO;
import com.easyprufung.backend.User.User;
import com.easyprufung.backend.Utils.Parser;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.util.Date;

@Service
public class AdminService {

    //Properties
    @Autowired
    RolesRepository rolesRepository;
    @Autowired
    AdminsRepository adminsRepository;

    //Methods
    public Admin createAdmin(AdminDTO adminDTO) {
        try {
            if(!Parser.isValidEmailAddress(adminDTO.getEmail())){
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "EMAIL_NOT_VALID");
            }
            if(checkIfAdminExist(adminDTO.getEmail())){
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "ADMIN_ALREADY_EXIST");
            }

            ModelMapper modelMapper = new ModelMapper();
            Date date = new Date();
            adminDTO.setCreatedDate(new Timestamp(date.getTime()));
            adminDTO.setUpdatedDate(new Timestamp(date.getTime()));
            adminDTO.setPassword(AESEncryption.encrypt(adminDTO.getPassword()));
            Admin admin = modelMapper.map(adminDTO, Admin.class);
            return adminsRepository.save(admin);
        }
        catch (Exception e){
            return null;
        }
    }
    public void deleteAdmin(long id) {
        var admin = adminsRepository.findById(id);
        adminsRepository.delete(admin);
    }
    public Admin updateAdmin(AdminDTO adminDTO) {
        var savedAdmin = getAdminById(adminDTO.getId());
        if(savedAdmin != null){
            Date date = new Date();
            savedAdmin.setUpdatedDate(new Timestamp(date.getTime()));
            savedAdmin.setFirstname(adminDTO.getFirstname());
            savedAdmin.setLastname(adminDTO.getLastname());
            savedAdmin.setEmail(adminDTO.getEmail());
            if(StringUtils.hasText(adminDTO.getPassword())){
                savedAdmin.setPassword(AESEncryption.encrypt(adminDTO.getPassword()));
            }
            savedAdmin.getRoles().clear();
            for(var role : adminDTO.getRoles()){
                savedAdmin.addRole(new Role(role.getId(),role.getName()));
            }
            return adminsRepository.save(savedAdmin);
        }
        else {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "ADMIN_NOT_FOUND");
        }
    }
    public void createSuperAdmin(Admin admin) {

        try {
            var superAdminRole= rolesRepository.findByName("SuperAdmin");
            var adminRole= rolesRepository.findByName("Admin");
            admin.addRole(superAdminRole);
            admin.addRole(adminRole);
            admin.setPassword(AESEncryption.encrypt(admin.getPassword()));
            adminsRepository.save(admin);
        } catch (Exception e){
        }
    }
    public Page<Admin> getAdmins(Pageable pageable) {
        return adminsRepository.findAll(pageable);
    }
    public Admin getAdminByEmail(String email) {
        return adminsRepository.findByEmail(email);
    }
    public Admin getAdminById(long id) {
        return  adminsRepository.findById(id);
    }
    public  boolean checkIfAdminExist (String email){
        var admin = adminsRepository.findByEmail(email);
        if(admin != null)
            return true;
        else
            return  false;
    }
    public Admin checkAdminCridentials(String email, String password) {
        var admin = adminsRepository.findByEmail(email);
        if(admin != null)
        {
            var userPassword = admin.getPassword();
            try {
                var encryptedPassword = AESEncryption.decrypt(userPassword);
                if(encryptedPassword.equals(password))
                    return admin;
                else
                    return null;
            } catch (Exception exception) {
            }
        }
        return null;
    }

    public AdminDTO getAdminDTO(Admin admin)
    {
        AdminDTO adminDTO = new AdminDTO();
        try {
            ModelMapper modelMapper = new ModelMapper();
            adminDTO = modelMapper.map(admin, AdminDTO.class);
            adminDTO.setPassword("");
        }
        catch (Exception exc) {
        }
        return adminDTO;
    }

    //Roles
    public Page<Role> getRoles(Pageable pageable) {
        return rolesRepository.findAll(pageable);
    }
}
