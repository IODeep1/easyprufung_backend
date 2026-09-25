package com.easyprufung.backend.Contact.Controller;

import com.easyprufung.backend.Contact.Contact;
import com.easyprufung.backend.Contact.DTO.ContactDTO;
import com.easyprufung.backend.Contact.Service.ContactService;
import com.easyprufung.backend.EndPoints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;


@Slf4j
@RepositoryRestController
@RequestMapping
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping(path = EndPoints.CONTACT_CREATE)
    public ResponseEntity<?> createContact(@RequestBody ContactDTO contactDTO) {
        try
        {
            Contact newContact = contactService.createContact(contactDTO);
            if(newContact != null){
                return ResponseEntity.ok("OK");
            }
            else{
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "FAILED_TO_CREATE_CONTACT");
            }

        }
        catch (RuntimeException exc) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exc.getMessage());
        }
    }
}
