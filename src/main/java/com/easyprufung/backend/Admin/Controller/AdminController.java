package com.easyprufung.backend.Admin.Controller;

import com.easyprufung.backend.Shared.RoleTags;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.easyprufung.backend.Admin.Admin;
import com.easyprufung.backend.Admin.DTO.AdminDTO;
import com.easyprufung.backend.Admin.Role;
import com.easyprufung.backend.Admin.Service.AdminService;
import com.easyprufung.backend.EndPoints;
import com.easyprufung.backend.Utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RepositoryRestController
@RequestMapping
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;


    @PostMapping(path = EndPoints.ADMIN_LOGIN)
    public ResponseEntity<?> loginAdmin(@RequestBody AdminDTO adminDTO) {
        try
        {
            Admin admin = adminService.checkAdminCridentials(adminDTO.getEmail(), adminDTO.getPassword());
            if(admin != null) {
                String token = JwtUtils.getJWTToken(adminDTO.getEmail(), RoleTags.AdminRole);
                adminDTO = adminService.getAdminDTO(admin);
                adminDTO.setToken(token);
                return ResponseEntity.ok(adminDTO);
            }
            else{
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "INVALID_CRIDENTIALS");
            }
        }catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", exc);
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping(path = EndPoints.ADMIN_CREATE)
    public ResponseEntity<?> createAdmin(@RequestBody AdminDTO adminDTO) {
        try
        {
            Admin newAdmin = adminService.createAdmin(adminDTO);
            if(newAdmin != null){
                adminDTO = adminService.getAdminDTO(newAdmin);
                ObjectWriter objectWriter = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).writer().withDefaultPrettyPrinter();
                return ResponseEntity.ok(adminDTO);
            }
            else{
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_CREATE_ADMIN");
            }

        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping(path = EndPoints.ADMIN_UPDATE)
    public ResponseEntity<?> updateAdmin(@RequestBody AdminDTO adminDTO) {
        try
        {
            var updatedAdmin = adminService.updateAdmin(adminDTO);
            if(updatedAdmin != null){
                adminDTO = adminService.getAdminDTO(updatedAdmin);
                return ResponseEntity.ok(adminDTO);
            }
            else{
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_UPDATE_ADMIN");
            }
        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @DeleteMapping(path = EndPoints.ADMIN_DELETE)
    public ResponseEntity<?> deleteAdmin(@PathVariable long id)
    {
        try
        {
            adminService.deleteAdmin(id);
            return ResponseEntity.ok("OK");
        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @GetMapping(path = EndPoints.ADMIN_LIST)
    public ResponseEntity<?> getAdminList(Pageable pageable) {
        try
        {
            Page<Admin> events = adminService.getAdmins(pageable);
            return ResponseEntity.ok(events.getContent());
        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @GetMapping(path = EndPoints.ADMIN_ROLE_LIST)
    public ResponseEntity<?> getRoleList(Pageable pageable) {
        try
        {
            Page<Role> events = adminService.getRoles(pageable);
            return ResponseEntity.ok(events.getContent());
        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }
}
